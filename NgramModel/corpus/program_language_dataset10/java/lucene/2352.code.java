package org.apache.solr.handler.component;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.common.SolrException;
import org.apache.solr.request.SimpleFacets;
import org.apache.lucene.util.OpenBitSet;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.schema.FieldType;
import org.apache.lucene.queryParser.ParseException;
public class  FacetComponent extends SearchComponent
{
  public static final String COMPONENT_NAME = "facet";
  @Override
  public void prepare(ResponseBuilder rb) throws IOException
  {
    if (rb.req.getParams().getBool(FacetParams.FACET,false)) {
      rb.setNeedDocSet( true );
      rb.doFacets = true;
    }
  }
  @Override
  public void process(ResponseBuilder rb) throws IOException
  {
    if (rb.doFacets) {
      SolrParams params = rb.req.getParams();
      SimpleFacets f = new SimpleFacets(rb.req,
              rb.getResults().docSet,
              params,
              rb );
      rb.rsp.add( "facet_counts", f.getFacetCounts() );
    }
  }
  private static final String commandPrefix = "{!" + CommonParams.TERMS + "=$";
  @Override
  public int distributedProcess(ResponseBuilder rb) throws IOException {
    if (!rb.doFacets) {
      return ResponseBuilder.STAGE_DONE;
    }
    if (rb.stage == ResponseBuilder.STAGE_GET_FIELDS) {
      for (int shardNum=0; shardNum<rb.shards.length; shardNum++) {
        List<String> refinements = null;
        for (DistribFieldFacet dff : rb._facetInfo.facets.values()) {
          if (!dff.needRefinements) continue;
          List<String> refList = dff._toRefine[shardNum];
          if (refList == null || refList.size()==0) continue;
          String key = dff.getKey();  
          String termsKey = key + "__terms";
          String termsVal = StrUtils.join(refList, ',');
          String facetCommand;
          if (dff.localParams != null) {
            facetCommand = commandPrefix+termsKey + " " + dff.facetStr.substring(2);
          } else {
            facetCommand = commandPrefix+termsKey+'}'+dff.field;
          }
          if (refinements == null) {
            refinements = new ArrayList<String>();
          }
          refinements.add(facetCommand);
          refinements.add(termsKey);
          refinements.add(termsVal);
        }
        if (refinements == null) continue;
        String shard = rb.shards[shardNum];
        ShardRequest refine = null;
        boolean newRequest = false;
        for (ShardRequest sreq : rb.outgoing) {
          if ((sreq.purpose & ShardRequest.PURPOSE_GET_FIELDS)!=0
                  && sreq.shards != null
                  && sreq.shards.length==1
                  && sreq.shards[0].equals(shard))
          {
            refine = sreq;
            break;
          }
        }
        if (refine == null) {
          newRequest = true;
          refine = new ShardRequest();
          refine.shards = new String[]{rb.shards[shardNum]};
          refine.params = new ModifiableSolrParams(rb.req.getParams());
          refine.params.remove(CommonParams.START);
          refine.params.set(CommonParams.ROWS,"0");
        }
        refine.purpose |= ShardRequest.PURPOSE_REFINE_FACETS;
        refine.params.set(FacetParams.FACET, "true");
        refine.params.remove(FacetParams.FACET_FIELD);
        refine.params.remove(FacetParams.FACET_QUERY);
        for (int i=0; i<refinements.size();) {
          String facetCommand=refinements.get(i++);
          String termsKey=refinements.get(i++);
          String termsVal=refinements.get(i++);
          refine.params.add(FacetParams.FACET_FIELD, facetCommand);
          refine.params.set(termsKey, termsVal);
        }
        if (newRequest) {
          rb.addRequest(this, refine);
        }
      }
    }
    return ResponseBuilder.STAGE_DONE;
  }
  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    if (!rb.doFacets) return;
    if ((sreq.purpose & ShardRequest.PURPOSE_GET_TOP_IDS) != 0) {
        sreq.purpose |= ShardRequest.PURPOSE_GET_FACETS;
        FacetInfo fi = rb._facetInfo;
        if (fi == null) {
          rb._facetInfo = fi = new FacetInfo();
          fi.parse(rb.req.getParams(), rb);
        }
        sreq.params.remove(FacetParams.FACET_MINCOUNT);
        sreq.params.remove(FacetParams.FACET_OFFSET);
        sreq.params.remove(FacetParams.FACET_LIMIT);
        for (DistribFieldFacet dff : fi.facets.values()) {
          String paramStart = "f." + dff.field + '.';
          sreq.params.remove(paramStart + FacetParams.FACET_MINCOUNT);
          sreq.params.remove(paramStart + FacetParams.FACET_OFFSET);
          if(dff.sort.equals(FacetParams.FACET_SORT_COUNT) && dff.limit > 0) {
            dff.initialLimit = dff.offset + dff.limit;
            dff.initialLimit = (int)(dff.initialLimit * 1.5) + 10;
          } else {
            dff.initialLimit = dff.limit;
          }
          sreq.params.set(paramStart + FacetParams.FACET_LIMIT,  dff.initialLimit);
      }
    } else {
      sreq.params.set(FacetParams.FACET, "false");
    }
  }
  @Override
  public void handleResponses(ResponseBuilder rb, ShardRequest sreq) {
    if (!rb.doFacets) return;
    if ((sreq.purpose & ShardRequest.PURPOSE_GET_FACETS)!=0) {
      countFacets(rb, sreq);
    } else if ((sreq.purpose & ShardRequest.PURPOSE_REFINE_FACETS)!=0) {
      refineFacets(rb, sreq);
    }
  }
  private void countFacets(ResponseBuilder rb, ShardRequest sreq) {
    FacetInfo fi = rb._facetInfo;
    for (ShardResponse srsp: sreq.responses) {
      int shardNum = rb.getShardNum(srsp.getShard());
      NamedList facet_counts = (NamedList)srsp.getSolrResponse().getResponse().get("facet_counts");
      NamedList facet_queries = (NamedList)facet_counts.get("facet_queries");
      if (facet_queries != null) {
        for (int i=0; i<facet_queries.size(); i++) {
          String returnedKey = (String)facet_queries.getName(i);
          long count = ((Number)facet_queries.getVal(i)).longValue();
          QueryFacet qf = fi.queryFacets.get(returnedKey);
          qf.count += count;
        }
      }
      NamedList facet_fields = (NamedList)facet_counts.get("facet_fields");
      for (DistribFieldFacet dff : fi.facets.values()) {
        dff.add(shardNum, (NamedList)facet_fields.get(dff.getKey()), dff.initialLimit);
      }
    }
    for (DistribFieldFacet dff : fi.facets.values()) {
      if (dff.limit <= 0) continue; 
      if (dff.minCount <= 1 && dff.sort.equals(FacetParams.FACET_SORT_INDEX)) continue;
      dff._toRefine = new List[rb.shards.length];
      ShardFacetCount[] counts = dff.getCountSorted();
      int ntop = Math.min(counts.length, dff.offset + dff.limit);
      long smallestCount = counts.length == 0 ? 0 : counts[ntop-1].count;
      for (int i=0; i<counts.length; i++) {
        ShardFacetCount sfc = counts[i];
        boolean needRefinement = false;
        if (i<ntop) {
          needRefinement = true;
        } else {
          long maxCount = sfc.count;
          for (int shardNum=0; shardNum<rb.shards.length; shardNum++) {
            OpenBitSet obs = dff.counted[shardNum];
            if (!obs.get(sfc.termNum)) {
              maxCount += dff.maxPossible(sfc,shardNum);
            }
          }
          if (maxCount >= smallestCount) {
            needRefinement = true;
          }
        }
        if (needRefinement) {
          for (int shardNum=0; shardNum<rb.shards.length; shardNum++) {
            OpenBitSet obs = dff.counted[shardNum];
            if (!obs.get(sfc.termNum) && dff.maxPossible(sfc,shardNum)>0) {
              dff.needRefinements = true;
              List<String> lst = dff._toRefine[shardNum];
              if (lst == null) {
                lst = dff._toRefine[shardNum] = new ArrayList<String>();
              }
              lst.add(sfc.name);
            }
          }
        }
      }
    }
  }
  private void refineFacets(ResponseBuilder rb, ShardRequest sreq) {
    FacetInfo fi = rb._facetInfo;
    for (ShardResponse srsp: sreq.responses) {
      NamedList facet_counts = (NamedList)srsp.getSolrResponse().getResponse().get("facet_counts");
      NamedList facet_fields = (NamedList)facet_counts.get("facet_fields");
      for (int i=0; i<facet_fields.size(); i++) {
        String key = facet_fields.getName(i);
        DistribFieldFacet dff = (DistribFieldFacet)fi.facets.get(key);
        if (dff == null) continue;
        NamedList shardCounts = (NamedList)facet_fields.getVal(i);
        for (int j=0; j<shardCounts.size(); j++) {
          String name = shardCounts.getName(j);
          long count = ((Number)shardCounts.getVal(j)).longValue();
          ShardFacetCount sfc = dff.counts.get(name);
          sfc.count += count;
        }
      }
    }
  }
  @Override
  public void finishStage(ResponseBuilder rb) {
    if (!rb.doFacets || rb.stage != ResponseBuilder.STAGE_GET_FIELDS) return;
    FacetInfo fi = rb._facetInfo;
    NamedList facet_counts = new SimpleOrderedMap();
    NamedList facet_queries = new SimpleOrderedMap();
    facet_counts.add("facet_queries",facet_queries);
    for (QueryFacet qf : fi.queryFacets.values()) {
      facet_queries.add(qf.getKey(), num(qf.count));
    }
    NamedList facet_fields = new SimpleOrderedMap();
    facet_counts.add("facet_fields", facet_fields);
    for (DistribFieldFacet dff : fi.facets.values()) {
      NamedList fieldCounts = new NamedList(); 
      facet_fields.add(dff.getKey(), fieldCounts);
      ShardFacetCount[] counts;
      boolean countSorted = dff.sort.equals(FacetParams.FACET_SORT_COUNT);
      if (countSorted) {
        counts = dff.countSorted;
        if (counts == null || dff.needRefinements) {
          counts = dff.getCountSorted();
        }
      } else if (dff.sort.equals(FacetParams.FACET_SORT_INDEX)) {
          counts = dff.getLexSorted();
      } else { 
          counts = dff.getLexSorted();
      }
      int end = dff.limit < 0 ? counts.length : Math.min(dff.offset + dff.limit, counts.length);
      for (int i=dff.offset; i<end; i++) {
        if (counts[i].count < dff.minCount) {
          if (countSorted) break;  
          else continue;
        }
        fieldCounts.add(counts[i].name, num(counts[i].count));
      }
      if (dff.missing) {
        fieldCounts.add(null, num(dff.missingCount));
      }
    }
    facet_counts.add("facet_dates", new SimpleOrderedMap());
    rb.rsp.add("facet_counts", facet_counts);
    rb._facetInfo = null;  
  }
  private Number num(long val) {
   if (val < Integer.MAX_VALUE) return (int)val;
   else return val;
  }
  private Number num(Long val) {
    if (val.longValue() < Integer.MAX_VALUE) return val.intValue();
    else return val;
  }
  @Override
  public String getDescription() {
    return "Handle Faceting";
  }
  @Override
  public String getVersion() {
    return "$Revision: 781801 $";
  }
  @Override
  public String getSourceId() {
    return "$Id: FacetComponent.java 781801 2009-06-04 17:28:56Z yonik $";
  }
  @Override
  public String getSource() {
    return "$URL: http://svn.apache.org/repos/asf/lucene/solr/branches/newtrunk/solr/src/java/org/apache/solr/handler/component/FacetComponent.java $";
  }
  @Override
  public URL[] getDocs() {
    return null;
  }
  public static class FacetInfo {
    public LinkedHashMap<String,QueryFacet> queryFacets;
    public LinkedHashMap<String,DistribFieldFacet> facets;
    void parse(SolrParams params, ResponseBuilder rb) {
      queryFacets = new LinkedHashMap<String,QueryFacet>();
      facets = new LinkedHashMap<String,DistribFieldFacet>();
      String[] facetQs = params.getParams(FacetParams.FACET_QUERY);
      if (facetQs != null) {
        for (String query : facetQs) {
          QueryFacet queryFacet = new QueryFacet(rb, query);
          queryFacets.put(queryFacet.getKey(), queryFacet);
        }
      }
      String[] facetFs = params.getParams(FacetParams.FACET_FIELD);
      if (facetFs != null) {
        for (String field : facetFs) {
          DistribFieldFacet ff = new DistribFieldFacet(rb, field);
          facets.put(ff.getKey(), ff);
        }
      }
    }
  }
  public static class FacetBase {
    String facetType;  
    String facetStr;   
    String facetOn;    
    private String key; 
    SolrParams localParams;  
    public FacetBase(ResponseBuilder rb, String facetType, String facetStr) {
      this.facetType = facetType;
      this.facetStr = facetStr;
      try {
        this.localParams = QueryParsing.getLocalParams(facetStr, rb.req.getParams());
      } catch (ParseException e) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
      }
      this.facetOn = facetStr;
      this.key = facetStr;
      if (localParams != null) {
        if (!facetType.equals(FacetParams.FACET_QUERY)) {
          facetOn = localParams.get(CommonParams.VALUE);
          key = facetOn;
        }
        key = localParams.get(CommonParams.OUTPUT_KEY, key);
      }
    }
    public String getKey() { return key; }
    public String getType() { return facetType; }
  }
  public static class QueryFacet extends FacetBase {
    public long count;
    public QueryFacet(ResponseBuilder rb, String facetStr) {
      super(rb, FacetParams.FACET_QUERY, facetStr);
    }
  }
  public static class FieldFacet extends FacetBase {
    public String field;     
    public FieldType ftype;
    public int offset;
    public int limit;
    public int minCount;
    public String sort;
    public boolean missing;
    public String prefix;
    public long missingCount;
    public FieldFacet(ResponseBuilder rb, String facetStr) {
      super(rb, FacetParams.FACET_FIELD, facetStr);
      fillParams(rb, rb.req.getParams(), facetOn);
    }
    private void fillParams(ResponseBuilder rb, SolrParams params, String field) {
      this.field = field;
      this.ftype = rb.req.getSchema().getFieldTypeNoEx(this.field);
      this.offset = params.getFieldInt(field, FacetParams.FACET_OFFSET, 0);
      this.limit = params.getFieldInt(field, FacetParams.FACET_LIMIT, 100);
      Integer mincount = params.getFieldInt(field, FacetParams.FACET_MINCOUNT);
      if (mincount==null) {
        Boolean zeros = params.getFieldBool(field, FacetParams.FACET_ZEROS);
        mincount = (zeros!=null && !zeros) ? 1 : 0;
      }
      this.minCount = mincount;
      this.missing = params.getFieldBool(field, FacetParams.FACET_MISSING, false);
      this.sort = params.getFieldParam(field, FacetParams.FACET_SORT, limit>0 ? FacetParams.FACET_SORT_COUNT : FacetParams.FACET_SORT_INDEX);
      if (this.sort.equals(FacetParams.FACET_SORT_COUNT_LEGACY)) {
        this.sort = FacetParams.FACET_SORT_COUNT;
      } else if (this.sort.equals(FacetParams.FACET_SORT_INDEX_LEGACY)) {
        this.sort = FacetParams.FACET_SORT_INDEX;
      }
      this.prefix = params.getFieldParam(field,FacetParams.FACET_PREFIX);
    }
  }
  public static class DistribFieldFacet extends FieldFacet {
    public List<String>[] _toRefine; 
    public long missingMaxPossible;
    public long[] missingMax;
    public OpenBitSet[] counted; 
    public HashMap<String,ShardFacetCount> counts = new HashMap<String,ShardFacetCount>(128);
    public int termNum;
    public int initialLimit;  
    public boolean needRefinements;
    public ShardFacetCount[] countSorted;
    DistribFieldFacet(ResponseBuilder rb, String facetStr) {
      super(rb, facetStr);
      missingMax = new long[rb.shards.length];
      counted = new OpenBitSet[rb.shards.length];
    }
    void add(int shardNum, NamedList shardCounts, int numRequested) {
      int sz = shardCounts.size();
      int numReceived = sz;
      OpenBitSet terms = new OpenBitSet(termNum+sz);
      long last = 0;
      for (int i=0; i<sz; i++) {
        String name = shardCounts.getName(i);
        long count = ((Number)shardCounts.getVal(i)).longValue();
        if (name == null) {
          missingCount += count;
          numReceived--;
        } else {
          ShardFacetCount sfc = counts.get(name);
          if (sfc == null) {
            sfc = new ShardFacetCount();
            sfc.name = name;
            sfc.indexed = ftype == null ? sfc.name : ftype.toInternal(sfc.name);
            sfc.termNum = termNum++;
            counts.put(name, sfc);
          }
          sfc.count += count;
          terms.fastSet(sfc.termNum);
          last = count;
        }
      }
      if (numRequested<0 || numRequested != 0 && numReceived < numRequested) {
        last = 0;
      }
      missingMaxPossible += last;
      missingMax[shardNum] = last;
      counted[shardNum] = terms;
    }
    public ShardFacetCount[] getLexSorted() {
      ShardFacetCount[] arr = counts.values().toArray(new ShardFacetCount[counts.size()]);
      Arrays.sort(arr, new Comparator<ShardFacetCount>() {
        public int compare(ShardFacetCount o1, ShardFacetCount o2) {
          return o1.indexed.compareTo(o2.indexed);
        }
      });
      countSorted = arr;
      return arr;
    }
    public ShardFacetCount[] getCountSorted() {
      ShardFacetCount[] arr = counts.values().toArray(new ShardFacetCount[counts.size()]);
      Arrays.sort(arr, new Comparator<ShardFacetCount>() {
        public int compare(ShardFacetCount o1, ShardFacetCount o2) {
          if (o2.count < o1.count) return -1;
          else if (o1.count < o2.count) return 1;
          return o1.indexed.compareTo(o2.indexed);
        }
      });
      countSorted = arr;
      return arr;
    }
    long maxPossible(ShardFacetCount sfc, int shardNum) {
      return missingMax[shardNum];
    }
  }
  public static class ShardFacetCount {
    public String name;
    public String indexed;  
    public long count;
    public int termNum;  
    public String toString() {
      return "{term="+name+",termNum="+termNum+",count="+count+"}";
    }
  }
}