package org.apache.solr.search;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.request.UnInvertedField;
import org.apache.lucene.util.OpenBitSet;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class SolrIndexSearcher extends IndexSearcher implements SolrInfoMBean {
  private static Logger log = LoggerFactory.getLogger(SolrIndexSearcher.class);
  private final SolrCore core;
  private final IndexSchema schema;
  private String indexDir;
  private final String name;
  private long openTime = System.currentTimeMillis();
  private long registerTime = 0;
  private long warmupTime = 0;
  private final SolrIndexReader reader;
  private final boolean closeReader;
  private final int queryResultWindowSize;
  private final int queryResultMaxDocsCached;
  private final boolean useFilterForSortedQuery;
  public final boolean enableLazyFieldLoading;
  private final boolean cachingEnabled;
  private final SolrCache<Query,DocSet> filterCache;
  private final SolrCache<QueryResultKey,DocList> queryResultCache;
  private final SolrCache<Integer,Document> documentCache;
  private final SolrCache<String,Object> fieldValueCache;
  private final LuceneQueryOptimizer optimizer;
  private final HashMap<String, SolrCache> cacheMap;
  private static final HashMap<String, SolrCache> noGenericCaches=new HashMap<String,SolrCache>(0);
  private final SolrCache[] cacheList;
  private static final SolrCache[] noCaches = new SolrCache[0];
  private final Collection<String> fieldNames;
  private Collection<String> storedHighlightFieldNames;
  public SolrIndexSearcher(SolrCore core, IndexSchema schema, String name, String path, boolean enableCache) throws IOException {
    this(core, schema,name, core.getIndexReaderFactory().newReader(core.getDirectoryFactory().open(path), false), true, enableCache);
  }
  public SolrIndexSearcher(SolrCore core, IndexSchema schema, String name,
      Directory directory, boolean enableCache) throws IOException {
    this(core, schema,name, core.getIndexReaderFactory().newReader(directory, false), true, enableCache);
  }
  public SolrIndexSearcher(SolrCore core, IndexSchema schema, String name, Directory directory, boolean readOnly, boolean enableCache) throws IOException {
    this(core, schema,name, core.getIndexReaderFactory().newReader(directory, readOnly), true, enableCache);
  }
  public SolrIndexSearcher(SolrCore core, IndexSchema schema, String name, IndexReader r, boolean enableCache) {
    this(core, schema,name,r, false, enableCache);
  }
  private static SolrIndexReader wrap(IndexReader r) {
    SolrIndexReader sir;
    if (!(r instanceof SolrIndexReader)) {
      sir = new SolrIndexReader(r, null, 0);
      sir.associateInfo(null);
    } else {
      sir = (SolrIndexReader)r;
    }
    return sir;
  }
  public SolrIndexSearcher(SolrCore core, IndexSchema schema, String name, IndexReader r, boolean closeReader, boolean enableCache) {
    super(wrap(r));
    this.reader = (SolrIndexReader)super.getIndexReader();
    this.core = core;
    this.schema = schema;
    this.name = "Searcher@" + Integer.toHexString(hashCode()) + (name!=null ? " "+name : "");
    log.info("Opening " + this.name);
    SolrIndexReader.setSearcher(reader, this);
    if (r.directory() instanceof FSDirectory) {
      FSDirectory fsDirectory = (FSDirectory) r.directory();
      indexDir = fsDirectory.getFile().getAbsolutePath();
    }
    this.closeReader = closeReader;
    setSimilarity(schema.getSimilarity());
    SolrConfig solrConfig = core.getSolrConfig();
    queryResultWindowSize = solrConfig.queryResultWindowSize;
    queryResultMaxDocsCached = solrConfig.queryResultMaxDocsCached;
    useFilterForSortedQuery = solrConfig.useFilterForSortedQuery;
    enableLazyFieldLoading = solrConfig.enableLazyFieldLoading;
    cachingEnabled=enableCache;
    if (cachingEnabled) {
      ArrayList<SolrCache> clist = new ArrayList<SolrCache>();
      fieldValueCache = solrConfig.fieldValueCacheConfig==null ? null : solrConfig.fieldValueCacheConfig.newInstance();
      if (fieldValueCache!=null) clist.add(fieldValueCache);
      filterCache= solrConfig.filterCacheConfig==null ? null : solrConfig.filterCacheConfig.newInstance();
      if (filterCache!=null) clist.add(filterCache);
      queryResultCache = solrConfig.queryResultCacheConfig==null ? null : solrConfig.queryResultCacheConfig.newInstance();
      if (queryResultCache!=null) clist.add(queryResultCache);
      documentCache = solrConfig.documentCacheConfig==null ? null : solrConfig.documentCacheConfig.newInstance();
      if (documentCache!=null) clist.add(documentCache);
      if (solrConfig.userCacheConfigs == null) {
        cacheMap = noGenericCaches;
      } else {
        cacheMap = new HashMap<String,SolrCache>(solrConfig.userCacheConfigs.length);
        for (CacheConfig userCacheConfig : solrConfig.userCacheConfigs) {
          SolrCache cache = null;
          if (userCacheConfig != null) cache = userCacheConfig.newInstance();
          if (cache != null) {
            cacheMap.put(cache.name(), cache);
            clist.add(cache);
          }
        }
      }
      cacheList = clist.toArray(new SolrCache[clist.size()]);
    } else {
      filterCache=null;
      queryResultCache=null;
      documentCache=null;
      fieldValueCache=null;
      cacheMap = noGenericCaches;
      cacheList= noCaches;
    }
    optimizer = solrConfig.filtOptEnabled ? new LuceneQueryOptimizer(solrConfig.filtOptCacheSize,solrConfig.filtOptThreshold) : null;
    fieldNames = r.getFieldNames(IndexReader.FieldOption.ALL);
  }
  public String toString() {
    return name;
  }
  public void register() {
    core.getInfoRegistry().put("searcher", this);
    core.getInfoRegistry().put(name, this);
    for (SolrCache cache : cacheList) {
      cache.setState(SolrCache.State.LIVE);
      core.getInfoRegistry().put(cache.name(), cache);
    }
    registerTime=System.currentTimeMillis();
  }
  public void close() throws IOException {
    if (cachingEnabled) {
      StringBuilder sb = new StringBuilder();
      sb.append("Closing ").append(name);
      for (SolrCache cache : cacheList) {
        sb.append("\n\t");
        sb.append(cache);
      }
      log.info(sb.toString());
    } else {
      log.debug("Closing " + name);
    }
    core.getInfoRegistry().remove(name);
    if (closeReader) reader.decRef();
    for (SolrCache cache : cacheList) {
      cache.close();
    }
  }
  public SolrIndexReader getReader() { return reader; }
  public IndexSchema getSchema() { return schema; }
  public Collection<String> getFieldNames() {
    return fieldNames;
  }
  public Collection<String> getStoredHighlightFieldNames() {
    if (storedHighlightFieldNames == null) {
      storedHighlightFieldNames = new LinkedList<String>();
      for (String fieldName : fieldNames) {
        try {
          SchemaField field = schema.getField(fieldName);
          if (field.stored() &&
                  ((field.getType() instanceof org.apache.solr.schema.TextField) ||
                  (field.getType() instanceof org.apache.solr.schema.StrField))) {
            storedHighlightFieldNames.add(fieldName);
          }
        } catch (RuntimeException e) { 
            log.warn("Field \"" + fieldName + "\" found in index, but not defined in schema.");
        }
      }
    }
    return storedHighlightFieldNames;
  }
  public static void initRegenerators(SolrConfig solrConfig) {
    if (solrConfig.fieldValueCacheConfig != null && solrConfig.fieldValueCacheConfig.getRegenerator() == null) {
      solrConfig.fieldValueCacheConfig.setRegenerator(
              new CacheRegenerator() {
                public boolean regenerateItem(SolrIndexSearcher newSearcher, SolrCache newCache, SolrCache oldCache, Object oldKey, Object oldVal) throws IOException {
                  if (oldVal instanceof UnInvertedField) {
                    UnInvertedField.getUnInvertedField((String)oldKey, newSearcher);
                  }
                  return true;
                }
              }
      );
    }
    if (solrConfig.filterCacheConfig != null && solrConfig.filterCacheConfig.getRegenerator() == null) {
      solrConfig.filterCacheConfig.setRegenerator(
              new CacheRegenerator() {
                public boolean regenerateItem(SolrIndexSearcher newSearcher, SolrCache newCache, SolrCache oldCache, Object oldKey, Object oldVal) throws IOException {
                  newSearcher.cacheDocSet((Query)oldKey, null, false);
                  return true;
                }
              }
      );
    }
    if (solrConfig.queryResultCacheConfig != null && solrConfig.queryResultCacheConfig.getRegenerator() == null) {
      final int queryResultWindowSize = solrConfig.queryResultWindowSize;
      solrConfig.queryResultCacheConfig.setRegenerator(
              new CacheRegenerator() {
                public boolean regenerateItem(SolrIndexSearcher newSearcher, SolrCache newCache, SolrCache oldCache, Object oldKey, Object oldVal) throws IOException {
                  QueryResultKey key = (QueryResultKey)oldKey;
                  int nDocs=1;
                  if (queryResultWindowSize<=1) {
                    DocList oldList = (DocList)oldVal;
                    int oldnDocs = oldList.offset() + oldList.size();
                    nDocs = Math.min(oldnDocs,40);
                  }
                  int flags=NO_CHECK_QCACHE | key.nc_flags;
                  QueryCommand qc = new QueryCommand();
                  qc.setQuery(key.query)
                    .setFilterList(key.filters)
                    .setSort(key.sort)
                    .setLen(nDocs)
                    .setSupersetMaxDoc(nDocs)
                    .setFlags(flags);
                  QueryResult qr = new QueryResult();
                  newSearcher.getDocListC(qr,qc);
                  return true;
                }
              }
      );
    }
  }
  public QueryResult search(QueryResult qr, QueryCommand cmd) throws IOException {
    getDocListC(qr,cmd);
    return qr;
  }
  public String getIndexDir() {
    return indexDir;
  }
  static class SetNonLazyFieldSelector implements FieldSelector {
    private Set<String> fieldsToLoad;
    SetNonLazyFieldSelector(Set<String> toLoad) {
      fieldsToLoad = toLoad;
    }
    public FieldSelectorResult accept(String fieldName) { 
      if(fieldsToLoad.contains(fieldName))
        return FieldSelectorResult.LOAD; 
      else
        return FieldSelectorResult.LAZY_LOAD;
    }
  }
  public Document doc(int i) throws IOException {
    return doc(i, (Set<String>)null);
  }
  public Document doc(int n, FieldSelector fieldSelector) throws IOException {
    return getIndexReader().document(n, fieldSelector);
  }
  public Document doc(int i, Set<String> fields) throws IOException {
    Document d;
    if (documentCache != null) {
      d = (Document)documentCache.get(i);
      if (d!=null) return d;
    }
    if(!enableLazyFieldLoading || fields == null) {
      d = getIndexReader().document(i);
    } else {
      d = getIndexReader().document(i, 
             new SetNonLazyFieldSelector(fields));
    }
    if (documentCache != null) {
      documentCache.put(i, d);
    }
    return d;
  }
  public void readDocs(Document[] docs, DocList ids) throws IOException {
    readDocs(docs, ids, null);
  }
  public void readDocs(Document[] docs, DocList ids, Set<String> fields) throws IOException {
    DocIterator iter = ids.iterator();
    for (int i=0; i<docs.length; i++) {
      docs[i] = doc(iter.nextDoc(), fields);
    }
  }
  public SolrCache getFieldValueCache() {
    return fieldValueCache;
  }
  public int getFirstMatch(Term t) throws IOException {
    TermDocs tdocs = null;
    try {
      tdocs = reader.termDocs(t);
      if (!tdocs.next()) return -1;
      return tdocs.doc();
    } finally {
      if (tdocs!=null) tdocs.close();
    }
  }
  public void cacheDocSet(Query query, DocSet optionalAnswer, boolean mustCache) throws IOException {
    if (optionalAnswer != null) {
      if (filterCache!=null) {
        filterCache.put(query,optionalAnswer);
      }
      return;
    }
    getDocSet(query);
  }
  public DocSet getDocSet(Query query) throws IOException {
    Query absQ = QueryUtils.getAbs(query);
    boolean positive = query==absQ;
    if (filterCache != null) {
      DocSet absAnswer = (DocSet)filterCache.get(absQ);
      if (absAnswer!=null) {
        if (positive) return absAnswer;
        else return getPositiveDocSet(matchAllDocsQuery).andNot(absAnswer);
      }
    }
    DocSet absAnswer = getDocSetNC(absQ, null);
    DocSet answer = positive ? absAnswer : getPositiveDocSet(matchAllDocsQuery).andNot(absAnswer);
    if (filterCache != null) {
      filterCache.put(absQ, absAnswer);
    }
    return answer;
  }
  DocSet getPositiveDocSet(Query q) throws IOException {
    DocSet answer;
    if (filterCache != null) {
      answer = (DocSet)filterCache.get(q);
      if (answer!=null) return answer;
    }
    answer = getDocSetNC(q,null);
    if (filterCache != null) filterCache.put(q,answer);
    return answer;
  }
  private static Query matchAllDocsQuery = new MatchAllDocsQuery();
  public DocSet getDocSet(List<Query> queries) throws IOException {
    if (queries==null) return null;
    if (queries.size()==1) return getDocSet(queries.get(0));
    DocSet answer=null;
    boolean[] neg = new boolean[queries.size()];
    DocSet[] sets = new DocSet[queries.size()];
    int smallestIndex = -1;
    int smallestCount = Integer.MAX_VALUE;
    for (int i=0; i<sets.length; i++) {
      Query q = queries.get(i);
      Query posQuery = QueryUtils.getAbs(q);
      sets[i] = getPositiveDocSet(posQuery);
      if (q==posQuery) {
        neg[i] = false;
        int sz = sets[i].size();
        if (sz<smallestCount) {
          smallestCount=sz;
          smallestIndex=i;
          answer = sets[i];
        }
      } else {
        neg[i] = true;
      }
    }
    if (answer==null) answer = getPositiveDocSet(matchAllDocsQuery);
    for (int i=0; i<sets.length; i++) {
      if (neg[i]) answer = answer.andNot(sets[i]);
    }
    for (int i=0; i<sets.length; i++) {
      if (!neg[i] && i!=smallestIndex) answer = answer.intersection(sets[i]);
    }
    return answer;
  }
  protected DocSet getDocSetNC(Query query, DocSet filter) throws IOException {
    DocSetCollector collector = new DocSetCollector(maxDoc()>>6, maxDoc());
    if (filter==null) {
      if (query instanceof TermQuery) {
        Term t = ((TermQuery)query).getTerm();
        SolrIndexReader[] readers = reader.getLeafReaders();
        int[] offsets = reader.getLeafOffsets();
        int[] arr = new int[256];
        int[] freq = new int[256];
        for (int i=0; i<readers.length; i++) {
          SolrIndexReader sir = readers[i];
          int offset = offsets[i];
          collector.setNextReader(sir, offset);
          TermDocs tdocs = sir.termDocs(t);
          for(;;) {
            int num = tdocs.read(arr, freq);
            if (num==0) break;
            for (int j=0; j<num; j++) {
              collector.collect(arr[j]);
            }
          }
          tdocs.close();
        }
      } else {
        super.search(query,null,collector);
      }
      return collector.getDocSet();
    } else {
      Filter luceneFilter = filter.getTopFilter();
      super.search(query, luceneFilter, collector);
      return collector.getDocSet();
    }
  }
  public DocSet getDocSet(Query query, DocSet filter) throws IOException {
    if (filter==null) return getDocSet(query);
    Query absQ = QueryUtils.getAbs(query);
    boolean positive = absQ==query;
    DocSet first;
    if (filterCache != null) {
      first = (DocSet)filterCache.get(absQ);
      if (first==null) {
        first = getDocSetNC(absQ,null);
        filterCache.put(absQ,first);
      }
      return positive ? first.intersection(filter) : filter.andNot(first);
    }
    return positive ? getDocSetNC(absQ,filter) : filter.andNot(getPositiveDocSet(absQ));
  }
  public DocSet convertFilter(Filter lfilter) throws IOException {
    DocIdSet docSet = lfilter.getDocIdSet(this.reader);
    OpenBitSet obs = new OpenBitSet();
    DocIdSetIterator it = docSet.iterator();
    int doc;
    while((doc = it.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      obs.fastSet(doc);
    }
    return new BitDocSet(obs);
  }
  public DocList getDocList(Query query, Query filter, Sort lsort, int offset, int len) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilterList(filter)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocList();
  }
  public DocList getDocList(Query query, List<Query> filterList, Sort lsort, int offset, int len, int flags) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilterList(filterList)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setFlags(flags);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocList();
  }
  private static final int NO_CHECK_QCACHE       = 0x80000000;
  private static final int GET_DOCSET            = 0x40000000;
  private static final int NO_CHECK_FILTERCACHE  = 0x20000000;
  public static final int GET_SCORES             =       0x01;
  private void getDocListC(QueryResult qr, QueryCommand cmd) throws IOException {
    DocListAndSet out = new DocListAndSet();
    qr.setDocListAndSet(out);
    QueryResultKey key=null;
    int maxDocRequested = cmd.getOffset() + cmd.getLen();
    if (maxDocRequested < 0 || maxDocRequested > maxDoc()) maxDocRequested = maxDoc();
    int supersetMaxDoc= maxDocRequested;
    DocList superset;
    if (queryResultCache != null && cmd.getFilter()==null) {
        key = new QueryResultKey(cmd.getQuery(), cmd.getFilterList(), cmd.getSort(), cmd.getFlags());
        if ((cmd.getFlags() & NO_CHECK_QCACHE)==0) {
          superset = (DocList)queryResultCache.get(key);
          if (superset != null) {
            if ((cmd.getFlags() & GET_SCORES)==0 || superset.hasScores()) {
              out.docList = superset.subset(cmd.getOffset(),cmd.getLen());
            }
          }
          if (out.docList != null) {
            if (out.docSet==null && ((cmd.getFlags() & GET_DOCSET)!=0) ) {
              if (cmd.getFilterList()==null) {
                out.docSet = getDocSet(cmd.getQuery());
              } else {
                List<Query> newList = new ArrayList<Query>(cmd.getFilterList()
.size()+1);
                newList.add(cmd.getQuery());
                newList.addAll(cmd.getFilterList());
                out.docSet = getDocSet(newList);
              }
            }
            return;
          }
        }
        if (maxDocRequested < queryResultWindowSize) {
          supersetMaxDoc=queryResultWindowSize;
        } else {
          supersetMaxDoc = ((maxDocRequested -1)/queryResultWindowSize + 1)*queryResultWindowSize;
          if (supersetMaxDoc < 0) supersetMaxDoc=maxDocRequested;
        }
    }
    boolean useFilterCache=false;
    if ((cmd.getFlags() & (GET_SCORES|NO_CHECK_FILTERCACHE))==0 && useFilterForSortedQuery && cmd.getSort() != null && filterCache != null) {
      useFilterCache=true;
      SortField[] sfields = cmd.getSort().getSort();
      for (SortField sf : sfields) {
        if (sf.getType() == SortField.SCORE) {
          useFilterCache=false;
          break;
        }
      }
    }
    if (useFilterCache) {
      if (out.docSet == null) {
        out.docSet = getDocSet(cmd.getQuery(),cmd.getFilter());
        DocSet bigFilt = getDocSet(cmd.getFilterList());
        if (bigFilt != null) out.docSet = out.docSet.intersection(bigFilt);
      }
      superset = sortDocSet(out.docSet,cmd.getSort(),supersetMaxDoc);
      out.docList = superset.subset(cmd.getOffset(),cmd.getLen());
    } else {
      cmd.setSupersetMaxDoc(supersetMaxDoc);
      if ((cmd.getFlags() & GET_DOCSET)!=0) {
        DocSet qDocSet = getDocListAndSetNC(qr,cmd);
        if (qDocSet!=null && filterCache!=null && !qr.isPartialResults()) filterCache.put(cmd.getQuery(),qDocSet);
      } else {
        getDocListNC(qr,cmd);
      }
      superset = out.docList;
      out.docList = superset.subset(cmd.getOffset(),cmd.getLen());
    }
    if (key != null && superset.size() <= queryResultMaxDocsCached && !qr.isPartialResults()) {
      queryResultCache.put(key, superset);
    }
  }
  private void getDocListNC(QueryResult qr,QueryCommand cmd) throws IOException {
    DocSet filter = cmd.getFilter()!=null ? cmd.getFilter() : getDocSet(cmd.getFilterList());
    final long timeAllowed = cmd.getTimeAllowed();
    int len = cmd.getSupersetMaxDoc();
    int last = len;
    if (last < 0 || last > maxDoc()) last=maxDoc();
    final int lastDocRequested = last;
    int nDocsReturned;
    int totalHits;
    float maxScore;
    int[] ids;
    float[] scores;
    boolean needScores = (cmd.getFlags() & GET_SCORES) != 0;
    Query query = QueryUtils.makeQueryable(cmd.getQuery());
    final Filter luceneFilter = filter==null ? null : filter.getTopFilter();
    if (lastDocRequested<=0) {
      final float[] topscore = new float[] { Float.NEGATIVE_INFINITY };
      final int[] numHits = new int[1];
      Collector collector;
      if (!needScores) {
        collector = new Collector () {
          public void setScorer(Scorer scorer) throws IOException {
          }
          public void collect(int doc) throws IOException {
            numHits[0]++;
          }
          public void setNextReader(IndexReader reader, int docBase) throws IOException {
          }
          public boolean acceptsDocsOutOfOrder() {
            return true;
          }
        };
      } else {
        collector = new Collector() {
          Scorer scorer;
          public void setScorer(Scorer scorer) throws IOException {
            this.scorer = scorer;
          }
          public void collect(int doc) throws IOException {
            numHits[0]++;
            float score = scorer.score();
            if (score > topscore[0]) topscore[0]=score;            
          }
          public void setNextReader(IndexReader reader, int docBase) throws IOException {
          }
          public boolean acceptsDocsOutOfOrder() {
            return true;
          }
        };
      }
      if( timeAllowed > 0 ) {
        collector = new TimeLimitingCollector(collector, timeAllowed);
      }
      try {
        super.search(query, luceneFilter, collector);
      }
      catch( TimeLimitingCollector.TimeExceededException x ) {
        log.warn( "Query: " + query + "; " + x.getMessage() );
        qr.setPartialResults(true);
      }
      nDocsReturned=0;
      ids = new int[nDocsReturned];
      scores = new float[nDocsReturned];
      totalHits = numHits[0];
      maxScore = totalHits>0 ? topscore[0] : 0.0f;
    } else {
      TopDocsCollector topCollector;
      if (cmd.getSort() == null) {
        topCollector = TopScoreDocCollector.create(len, true);
      } else {
        topCollector = TopFieldCollector.create(cmd.getSort(), len, false, needScores, needScores, true);
      }
      Collector collector = topCollector;
      if( timeAllowed > 0 ) {
        collector = new TimeLimitingCollector(collector, timeAllowed);
      }
      try {
        super.search(query, luceneFilter, collector);
      }
      catch( TimeLimitingCollector.TimeExceededException x ) {
        log.warn( "Query: " + query + "; " + x.getMessage() );
        qr.setPartialResults(true);
      }
      totalHits = topCollector.getTotalHits();
      TopDocs topDocs = topCollector.topDocs(0, len);
      maxScore = totalHits>0 ? topDocs.getMaxScore() : 0.0f;
      nDocsReturned = topDocs.scoreDocs.length;
      ids = new int[nDocsReturned];
      scores = (cmd.getFlags()&GET_SCORES)!=0 ? new float[nDocsReturned] : null;
      for (int i=0; i<nDocsReturned; i++) {
        ScoreDoc scoreDoc = topDocs.scoreDocs[i];
        ids[i] = scoreDoc.doc;
        if (scores != null) scores[i] = scoreDoc.score;
      }
    }
    int sliceLen = Math.min(lastDocRequested,nDocsReturned);
    if (sliceLen < 0) sliceLen=0;
    qr.setDocList(new DocSlice(0,sliceLen,ids,scores,totalHits,maxScore));
  }
  private DocSet getDocListAndSetNC(QueryResult qr,QueryCommand cmd) throws IOException {
    int len = cmd.getSupersetMaxDoc();
    DocSet filter = cmd.getFilter()!=null ? cmd.getFilter() : getDocSet(cmd.getFilterList());
    int last = len;
    if (last < 0 || last > maxDoc()) last=maxDoc();
    final int lastDocRequested = last;
    int nDocsReturned;
    int totalHits;
    float maxScore;
    int[] ids;
    float[] scores;
    DocSet set;
    boolean needScores = (cmd.getFlags() & GET_SCORES) != 0;
    int maxDoc = maxDoc();
    int smallSetSize = maxDoc>>6;
    Query query = QueryUtils.makeQueryable(cmd.getQuery());
    final long timeAllowed = cmd.getTimeAllowed();
    final Filter luceneFilter = filter==null ? null : filter.getTopFilter();
    if (lastDocRequested<=0) {
      final float[] topscore = new float[] { Float.NEGATIVE_INFINITY };
      Collector collector;
      DocSetCollector setCollector;
       if (!needScores) {
         collector = setCollector = new DocSetCollector(smallSetSize, maxDoc);
       } else {
         collector = setCollector = new DocSetDelegateCollector(smallSetSize, maxDoc, new Collector() {
           Scorer scorer;
           public void setScorer(Scorer scorer) throws IOException {
             this.scorer = scorer;
           }
           public void collect(int doc) throws IOException {
             float score = scorer.score();
             if (score > topscore[0]) topscore[0]=score;
           }
           public void setNextReader(IndexReader reader, int docBase) throws IOException {
           }
           public boolean acceptsDocsOutOfOrder() {
             return false;
           }
         });
       }
       if( timeAllowed > 0 ) {
         collector = new TimeLimitingCollector(collector, timeAllowed);
       }
       try {
         super.search(query, luceneFilter, collector);
       }
       catch( TimeLimitingCollector.TimeExceededException x ) {
         log.warn( "Query: " + query + "; " + x.getMessage() );
         qr.setPartialResults(true);
       }
      set = setCollector.getDocSet();
      nDocsReturned = 0;
      ids = new int[nDocsReturned];
      scores = new float[nDocsReturned];
      totalHits = set.size();
      maxScore = totalHits>0 ? topscore[0] : 0.0f;
    } else {
      TopDocsCollector topCollector;
      if (cmd.getSort() == null) {
        topCollector = TopScoreDocCollector.create(len, true);
      } else {
        topCollector = TopFieldCollector.create(cmd.getSort(), len, false, needScores, needScores, true);
      }
      DocSetCollector setCollector = new DocSetDelegateCollector(maxDoc>>6, maxDoc, topCollector);
      Collector collector = setCollector;
      if( timeAllowed > 0 ) {
        collector = new TimeLimitingCollector(collector, timeAllowed );
      }
      try {
        super.search(query, luceneFilter, collector);
      }
      catch( TimeLimitingCollector.TimeExceededException x ) {
        log.warn( "Query: " + query + "; " + x.getMessage() );
        qr.setPartialResults(true);
      }
      set = setCollector.getDocSet();      
      totalHits = topCollector.getTotalHits();
      assert(totalHits == set.size());
      TopDocs topDocs = topCollector.topDocs(0, len);
      maxScore = totalHits>0 ? topDocs.getMaxScore() : 0.0f;
      nDocsReturned = topDocs.scoreDocs.length;
      ids = new int[nDocsReturned];
      scores = (cmd.getFlags()&GET_SCORES)!=0 ? new float[nDocsReturned] : null;
      for (int i=0; i<nDocsReturned; i++) {
        ScoreDoc scoreDoc = topDocs.scoreDocs[i];
        ids[i] = scoreDoc.doc;
        if (scores != null) scores[i] = scoreDoc.score;
      }
    }
    int sliceLen = Math.min(lastDocRequested,nDocsReturned);
    if (sliceLen < 0) sliceLen=0;
    qr.setDocList(new DocSlice(0,sliceLen,ids,scores,totalHits,maxScore));
    qr.setDocSet(set);
    return filter==null ? qr.getDocSet() : null;
  }
  public DocList getDocList(Query query, DocSet filter, Sort lsort, int offset, int len) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilter(filter)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocList();
  }
  public DocListAndSet getDocListAndSet(Query query, Query filter, Sort lsort, int offset, int len) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilterList(filter)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setNeedDocSet(true);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocListAndSet();
  }
  public DocListAndSet getDocListAndSet(Query query, Query filter, Sort lsort, int offset, int len, int flags) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilterList(filter)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setFlags(flags)
      .setNeedDocSet(true);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocListAndSet();
  }
  public DocListAndSet getDocListAndSet(Query query, List<Query> filterList, Sort lsort, int offset, int len) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilterList(filterList)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setNeedDocSet(true);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocListAndSet();
  }
  public DocListAndSet getDocListAndSet(Query query, List<Query> filterList, Sort lsort, int offset, int len, int flags) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilterList(filterList)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setFlags(flags)
      .setNeedDocSet(true);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocListAndSet();
  }
  public DocListAndSet getDocListAndSet(Query query, DocSet filter, Sort lsort, int offset, int len) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilter(filter)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setNeedDocSet(true);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocListAndSet();
  }
  public DocListAndSet getDocListAndSet(Query query, DocSet filter, Sort lsort, int offset, int len, int flags) throws IOException {
    QueryCommand qc = new QueryCommand();
    qc.setQuery(query)
      .setFilter(filter)
      .setSort(lsort)
      .setOffset(offset)
      .setLen(len)
      .setFlags(flags)
      .setNeedDocSet(true);
    QueryResult qr = new QueryResult();
    search(qr,qc);
    return qr.getDocListAndSet();
  }
  protected DocList sortDocSet(DocSet set, Sort sort, int nDocs) throws IOException {
    boolean inOrder = set instanceof BitDocSet || set instanceof SortedIntDocSet;
    TopDocsCollector topCollector = TopFieldCollector.create(sort, nDocs, false, false, false, inOrder);
    DocIterator iter = set.iterator();
    int base=0;
    int end=0;
    int readerIndex = -1;
    SolrIndexReader r=null;
    while(iter.hasNext()) {
      int doc = iter.nextDoc();
      while (doc>=end) {
        r = reader.getLeafReaders()[++readerIndex];
        base = reader.getLeafOffsets()[readerIndex];
        end = base + r.maxDoc();
        topCollector.setNextReader(r, base);
      }
      topCollector.collect(doc-base);
    }
    TopDocs topDocs = topCollector.topDocs(0, nDocs);
    int nDocsReturned = topDocs.scoreDocs.length;
    int[] ids = new int[nDocsReturned];
    for (int i=0; i<nDocsReturned; i++) {
      ScoreDoc scoreDoc = topDocs.scoreDocs[i];
      ids[i] = scoreDoc.doc;
    }
    return new DocSlice(0,nDocsReturned,ids,null,topDocs.totalHits,0.0f);
  }
  public int numDocs(Query a, DocSet b) throws IOException {
    Query absQ = QueryUtils.getAbs(a);
    DocSet positiveA = getPositiveDocSet(absQ);
    return a==absQ ? b.intersectionSize(positiveA) : b.andNotSize(positiveA);
  }
  public int numDocs(Query a, Query b) throws IOException {
    Query absA = QueryUtils.getAbs(a);
    Query absB = QueryUtils.getAbs(b);     
    DocSet positiveA = getPositiveDocSet(absA);
    DocSet positiveB = getPositiveDocSet(absB);
    if (a==absA) {
      if (b==absB) return positiveA.intersectionSize(positiveB);
      return positiveA.andNotSize(positiveB);
    }
    if (b==absB) return positiveB.andNotSize(positiveA);
    DocSet all = getPositiveDocSet(matchAllDocsQuery);
    return all.andNotSize(positiveA.union(positiveB));
  }
  public Document[] readDocs(DocList ids) throws IOException {
     Document[] docs = new Document[ids.size()];
     readDocs(docs,ids);
     return docs;
  }
  public void warm(SolrIndexSearcher old) throws IOException {
    boolean logme = log.isInfoEnabled();
    long warmingStartTime = System.currentTimeMillis();
    for (int i=0; i<cacheList.length; i++) {
      if (logme) log.info("autowarming " + this + " from " + old + "\n\t" + old.cacheList[i]);
      this.cacheList[i].warm(this, old.cacheList[i]);
      if (logme) log.info("autowarming result for " + this + "\n\t" + this.cacheList[i]);
    }
    warmupTime = System.currentTimeMillis() - warmingStartTime;
  }
  public SolrCache getCache(String cacheName) {
    return cacheMap.get(cacheName);
  }
  public Object cacheLookup(String cacheName, Object key) {
    SolrCache cache = cacheMap.get(cacheName);
    return cache==null ? null : cache.get(key);
  }
  public Object cacheInsert(String cacheName, Object key, Object val) {
    SolrCache cache = cacheMap.get(cacheName);
    return cache==null ? null : cache.put(key,val);
  }
  public long getOpenTime() {
    return openTime;
  }
  public String getName() {
    return SolrIndexSearcher.class.getName();
  }
  public String getVersion() {
    return SolrCore.version;
  }
  public String getDescription() {
    return "index searcher";
  }
  public Category getCategory() {
    return Category.CORE;
  }
  public String getSourceId() {
    return "$Id: SolrIndexSearcher.java 922957 2010-03-14 20:58:32Z markrmiller $";
  }
  public String getSource() {
    return "$URL: http://svn.apache.org/repos/asf/lucene/solr/branches/newtrunk/solr/src/java/org/apache/solr/search/SolrIndexSearcher.java $";
  }
  public URL[] getDocs() {
    return null;
  }
  public NamedList getStatistics() {
    NamedList lst = new SimpleOrderedMap();
    lst.add("searcherName", name);
    lst.add("caching", cachingEnabled);
    lst.add("numDocs", reader.numDocs());
    lst.add("maxDoc", reader.maxDoc());
    lst.add("reader", reader.toString());
    lst.add("readerDir", reader.directory());
    lst.add("indexVersion", reader.getVersion());
    lst.add("openedAt", new Date(openTime));
    if (registerTime!=0) lst.add("registeredAt", new Date(registerTime));
    lst.add("warmupTime", warmupTime);
    return lst;
  }
  public static class QueryCommand {
    private Query query;
    private List<Query> filterList;
    private DocSet filter;
    private Sort sort;
    private int offset;
    private int len;
    private int supersetMaxDoc;
    private int flags;
    private long timeAllowed = -1;
    public Query getQuery() { return query; }
    public QueryCommand setQuery(Query query) {
      this.query = query;
      return this;
    }
    public List<Query> getFilterList() { return filterList; }
    public QueryCommand setFilterList(List<Query> filterList) {
      if( filter != null ) {
        throw new IllegalArgumentException( "Either filter or filterList may be set in the QueryCommand, but not both." );
      }
      this.filterList = filterList;
      return this;
    }
    public QueryCommand setFilterList(Query f) {
      if( filter != null ) {
        throw new IllegalArgumentException( "Either filter or filterList may be set in the QueryCommand, but not both." );
      }
      filterList = null;
      if (f != null) {
        filterList = new ArrayList<Query>(2);
        filterList.add(f);
      }
      return this;
    }
    public DocSet getFilter() { return filter; }
    public QueryCommand setFilter(DocSet filter) {
      if( filterList != null ) {
        throw new IllegalArgumentException( "Either filter or filterList may be set in the QueryCommand, but not both." );
      }
      this.filter = filter;
      return this;
    }
    public Sort getSort() { return sort; }
    public QueryCommand setSort(Sort sort) {
      this.sort = sort;
      return this;
    }
    public int getOffset() { return offset; }
    public QueryCommand setOffset(int offset) {
      this.offset = offset;
      return this;
    }
    public int getLen() { return len; }
    public QueryCommand setLen(int len) {
      this.len = len;
      return this;
    }
    public int getSupersetMaxDoc() { return supersetMaxDoc; }
    public QueryCommand setSupersetMaxDoc(int supersetMaxDoc) {
      this.supersetMaxDoc = supersetMaxDoc;
      return this;
    }
    public int getFlags() {
      return flags;
    }
    public QueryCommand replaceFlags(int flags) {
      this.flags = flags;
      return this;
    }
    public QueryCommand setFlags(int flags) {
      this.flags |= flags;
      return this;
    }
    public QueryCommand clearFlags(int flags) {
      this.flags &= ~flags;
      return this;
    }
    public long getTimeAllowed() { return timeAllowed; }
    public QueryCommand setTimeAllowed(long timeAllowed) {
      this.timeAllowed = timeAllowed;
      return this;
    }
    public boolean isNeedDocSet() { return (flags & GET_DOCSET) != 0; }
    public QueryCommand setNeedDocSet(boolean needDocSet) {
      return needDocSet ? setFlags(GET_DOCSET) : clearFlags(GET_DOCSET);
    }
  }
  public static class QueryResult {
    private boolean partialResults;
    private DocListAndSet docListAndSet;
    public DocList getDocList() { return docListAndSet.docList; }
    public void setDocList(DocList list) {
      if( docListAndSet == null ) {
        docListAndSet = new DocListAndSet();
      }
      docListAndSet.docList = list;
    }
    public DocSet getDocSet() { return docListAndSet.docSet; }
    public void setDocSet(DocSet set) {
      if( docListAndSet == null ) {
        docListAndSet = new DocListAndSet();
      }
      docListAndSet.docSet = set;
    }
    public boolean isPartialResults() { return partialResults; }
    public void setPartialResults(boolean partialResults) { this.partialResults = partialResults; }
    public void setDocListAndSet( DocListAndSet listSet ) { docListAndSet = listSet; }
    public DocListAndSet getDocListAndSet() { return docListAndSet; }
  }
}
