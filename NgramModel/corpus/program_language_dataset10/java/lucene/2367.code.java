package org.apache.solr.handler.component;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.util.StringHelper;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.StrField;
import org.apache.solr.request.SimpleFacets.CountPair;
import org.apache.solr.util.BoundedTreeSet;
import org.apache.solr.client.solrj.response.TermsResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
public class TermsComponent extends SearchComponent {
  public static final int UNLIMITED_MAX_COUNT = -1;
  public static final String COMPONENT_NAME = "terms";
  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    if (params.getBool(TermsParams.TERMS, false)) {
      rb.doTerms = true;
    }
    String shards = params.get(ShardParams.SHARDS);
    if (shards != null) {
      if (params.get(ShardParams.SHARDS_QT) == null) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No shards.qt parameter specified");
      }
      List<String> lst = StrUtils.splitSmart(shards, ",", true);
      rb.shards = lst.toArray(new String[lst.size()]);
    }
  }
  public void process(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    if (params.getBool(TermsParams.TERMS, false)) {
      String lowerStr = params.get(TermsParams.TERMS_LOWER, null);
      String[] fields = params.getParams(TermsParams.TERMS_FIELD);
      if (fields != null && fields.length > 0) {
        NamedList terms = new NamedList();
        rb.rsp.add("terms", terms);
        int limit = params.getInt(TermsParams.TERMS_LIMIT, 10);
        if (limit < 0) {
          limit = Integer.MAX_VALUE;
        }
        String upperStr = params.get(TermsParams.TERMS_UPPER);
        boolean upperIncl = params.getBool(TermsParams.TERMS_UPPER_INCLUSIVE, false);
        boolean lowerIncl = params.getBool(TermsParams.TERMS_LOWER_INCLUSIVE, true);
        boolean sort = !TermsParams.TERMS_SORT_INDEX.equals(
                          params.get(TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_COUNT));
        int freqmin = params.getInt(TermsParams.TERMS_MINCOUNT, 1); 
        int freqmax = params.getInt(TermsParams.TERMS_MAXCOUNT, UNLIMITED_MAX_COUNT); 
        if (freqmax<0) {
          freqmax = Integer.MAX_VALUE;
        }
        String prefix = params.get(TermsParams.TERMS_PREFIX_STR);
        String regexp = params.get(TermsParams.TERMS_REGEXP_STR);
        Pattern pattern = regexp != null ? Pattern.compile(regexp, resolveRegexpFlags(params)) : null;
        boolean raw = params.getBool(TermsParams.TERMS_RAW, false);
        for (int j = 0; j < fields.length; j++) {
          String field = StringHelper.intern(fields[j]);
          FieldType ft = raw ? null : rb.req.getSchema().getFieldTypeNoEx(field);
          if (ft==null) ft = new StrField();
          String lower = lowerStr==null ? prefix : (raw ? lowerStr : ft.toInternal(lowerStr));
          if (lower == null) lower="";
          String upper = upperStr==null ? null : (raw ? upperStr : ft.toInternal(upperStr));
          Term lowerTerm = new Term(field, lower);
          Term upperTerm = upper==null ? null : new Term(field, upper);
          TermEnum termEnum = rb.req.getSearcher().getReader().terms(lowerTerm); 
          int i = 0;
          BoundedTreeSet<CountPair<String, Integer>> queue = (sort ? new BoundedTreeSet<CountPair<String, Integer>>(limit) : null); 
          NamedList fieldTerms = new NamedList();
          terms.add(field, fieldTerms);
          Term lowerTestTerm = termEnum.term();
          if (lowerTestTerm!=null && lowerIncl == false && lowerTestTerm.field() == field  
                  && lowerTestTerm.text().equals(lower)) {
            termEnum.next();
          }
          while (i<limit || sort) {
            Term theTerm = termEnum.term();
            if (theTerm==null || field != theTerm.field())  
              break;
            String indexedText = theTerm.text();
            if (prefix != null && !indexedText.startsWith(prefix)) break;
            if (pattern != null && !pattern.matcher(indexedText).matches()) {
                termEnum.next();
                continue;
            }
            if (upperTerm != null) {
              int upperCmp = theTerm.compareTo(upperTerm);
              if (upperCmp>0 || (upperCmp==0 && !upperIncl)) break;
            }
            int docFreq = termEnum.docFreq();
            if (docFreq >= freqmin && docFreq <= freqmax) {
              String label = raw ? indexedText : ft.indexedToReadable(indexedText);
              if (sort) {
                queue.add(new CountPair<String, Integer>(label, docFreq));
              } else {
                fieldTerms.add(label, docFreq);
                i++;
              }
            }
            termEnum.next();
          }
          termEnum.close();
          if (sort) {
            for (CountPair<String, Integer> item : queue) {
              if (i < limit) {
                fieldTerms.add(item.key, item.val);
                i++;
              } else {
                break;
              }
            }
          }
        }
      } else {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "No terms.fl parameter specified");
      }
    }
  }
  int resolveRegexpFlags(SolrParams params) {
      String[] flagParams = params.getParams(TermsParams.TERMS_REGEXP_FLAG);
      if (flagParams == null) {
          return 0;
      }
      int flags = 0;
      for (String flagParam : flagParams) {
          try {
            flags |= TermsParams.TermsRegexpFlag.valueOf(flagParam.toUpperCase()).getValue();
          } catch (IllegalArgumentException iae) {
              throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Unknown terms regex flag '" + flagParam + "'");
          }
      }
      return flags;
  }
  @Override
  public int distributedProcess(ResponseBuilder rb) throws IOException {
    if (!rb.doTerms) {
      return ResponseBuilder.STAGE_DONE;
    }
    if (rb.stage == ResponseBuilder.STAGE_EXECUTE_QUERY) {
      TermsHelper th = rb._termsHelper;
      if (th == null) {
        th = rb._termsHelper = new TermsHelper();
        th.init(rb.req.getParams());
      }
      ShardRequest sreq = createShardQuery(rb.req.getParams());
      rb.addRequest(this, sreq);
    }
    if (rb.stage < ResponseBuilder.STAGE_EXECUTE_QUERY) {
      return ResponseBuilder.STAGE_EXECUTE_QUERY;
    } else {
      return ResponseBuilder.STAGE_DONE;
    }
  }
  @Override
  public void handleResponses(ResponseBuilder rb, ShardRequest sreq) {
    if (!rb.doTerms || (sreq.purpose & ShardRequest.PURPOSE_GET_TERMS) == 0) {
      return;
    }
    TermsHelper th = rb._termsHelper;
    if (th != null) {
      for (ShardResponse srsp : sreq.responses) {
        th.parse((NamedList) srsp.getSolrResponse().getResponse().get("terms"));
      }
    }
  }
  @Override
  public void finishStage(ResponseBuilder rb) {
    if (!rb.doTerms || rb.stage != ResponseBuilder.STAGE_EXECUTE_QUERY) {
      return;
    }
    TermsHelper ti = rb._termsHelper;
    NamedList terms = ti.buildResponse();
    rb.rsp.add("terms", terms);
    rb._termsHelper = null;
  }
  private ShardRequest createShardQuery(SolrParams params) {
    ShardRequest sreq = new ShardRequest();
    sreq.purpose = ShardRequest.PURPOSE_GET_TERMS;
    sreq.params = new ModifiableSolrParams(params);
    sreq.params.remove(ShardParams.SHARDS);
    sreq.params.remove(TermsParams.TERMS_MAXCOUNT);
    sreq.params.remove(TermsParams.TERMS_MINCOUNT);
    sreq.params.set(TermsParams.TERMS_LIMIT, -1);
    sreq.params.set(TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_INDEX);
    String qt = params.get(CommonParams.QT);
    if (qt != null) {
      sreq.params.add(CommonParams.QT, qt);
    }
    return sreq;
  }
  public class TermsHelper {
    private HashMap<String, HashMap<String, TermsResponse.Term>> fieldmap;
    private SolrParams params;
    public TermsHelper() {
      fieldmap = new HashMap<String, HashMap<String, TermsResponse.Term>>(5);
    }
    public void init(SolrParams params) {
      this.params = params;
      String[] fields = params.getParams(TermsParams.TERMS_FIELD);
      if (fields != null) {
        for (String field : fields) {
          fieldmap.put(field, new HashMap<String, TermsResponse.Term>(128));
        }
      }
    }
    public void parse(NamedList terms) {
      if (terms == null) {
        return;
      }
      TermsResponse termsResponse = new TermsResponse(terms);
      for (String key : fieldmap.keySet()) {
        HashMap<String, TermsResponse.Term> termmap = fieldmap.get(key);
        List<TermsResponse.Term> termlist = termsResponse.getTerms(key); 
        if (termlist == null) {
          continue;
        }
        for (TermsResponse.Term tc : termlist) {
          String term = tc.getTerm();
          if (termmap.containsKey(term)) {
            TermsResponse.Term oldtc = termmap.get(term);
            oldtc.addFrequency(tc.getFrequency());
            termmap.put(term, oldtc);
          } else {
            termmap.put(term, tc);
          }
        }
      }
    }
    public NamedList buildResponse() {
      NamedList response = new SimpleOrderedMap();
      boolean sort = !TermsParams.TERMS_SORT_INDEX.equals(params.get(
          TermsParams.TERMS_SORT, TermsParams.TERMS_SORT_COUNT));
      long freqmin = 1;
      String s = params.get(TermsParams.TERMS_MINCOUNT);
      if (s != null)  freqmin = Long.parseLong(s);
      long freqmax = -1;
      s = params.get(TermsParams.TERMS_MAXCOUNT);
      if (s != null)  freqmax = Long.parseLong(s);
      if (freqmax < 0) {
        freqmax = Long.MAX_VALUE;
      }
      long limit = 10;
      s = params.get(TermsParams.TERMS_LIMIT);
      if (s != null)  limit = Long.parseLong(s);
      if (limit < 0) {
        limit = Long.MAX_VALUE;
      }
      for (String key : fieldmap.keySet()) {
        NamedList fieldterms = new SimpleOrderedMap();
        TermsResponse.Term[] data = null;
        if (sort) {
          data = getCountSorted(fieldmap.get(key));
        } else {
          data = getLexSorted(fieldmap.get(key));
        }
        int cnt = 0;
        for (TermsResponse.Term tc : data) {
          if (tc.getFrequency() >= freqmin && tc.getFrequency() <= freqmax) {
            fieldterms.add(tc.getTerm(), num(tc.getFrequency()));
            cnt++;
          }
          if (cnt >= limit) {
            break;
          }
        }
        response.add(key, fieldterms);
      }
      return response;
    }
    private Number num(long val) {
      if (val < Integer.MAX_VALUE) return (int) val;
      else return val;
    }
    public TermsResponse.Term[] getLexSorted(HashMap<String, TermsResponse.Term> data) {
      TermsResponse.Term[] arr = data.values().toArray(new TermsResponse.Term[data.size()]);
      Arrays.sort(arr, new Comparator<TermsResponse.Term>() {
        public int compare(TermsResponse.Term o1, TermsResponse.Term o2) {
          return o1.getTerm().compareTo(o2.getTerm());
        }
      });
      return arr;
    }
    public TermsResponse.Term[] getCountSorted(HashMap<String, TermsResponse.Term> data) {
      TermsResponse.Term[] arr = data.values().toArray(new TermsResponse.Term[data.size()]);
      Arrays.sort(arr, new Comparator<TermsResponse.Term>() {
        public int compare(TermsResponse.Term o1, TermsResponse.Term o2) {
          long freq1 = o1.getFrequency();
          long freq2 = o2.getFrequency();
          if (freq2 < freq1) {
            return -1;
          } else if (freq1 < freq2) {
            return 1;
          } else {
            return o1.getTerm().compareTo(o2.getTerm());
          }
        }
      });
      return arr;
    }
  }
  public String getVersion() {
    return "$Revision: 890199 $";
  }
  public String getSourceId() {
    return "$Id: TermsComponent.java 890199 2009-12-14 07:06:22Z shalin $";
  }
  public String getSource() {
    return "$URL: http://svn.apache.org/repos/asf/lucene/solr/branches/newtrunk/solr/src/java/org/apache/solr/handler/component/TermsComponent.java $";
  }
  public String getDescription() {
    return "A Component for working with Term Enumerators";
  }
}
