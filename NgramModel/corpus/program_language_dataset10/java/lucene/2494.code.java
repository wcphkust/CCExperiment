package org.apache.solr.search;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
public class OldLuceneQParserPlugin extends QParserPlugin {
  public static String NAME = "lucenePlusSort";
  public void init(NamedList args) {
  }
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new OldLuceneQParser(qstr, localParams, params, req);
  }
}
