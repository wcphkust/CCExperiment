package org.apache.solr.request;
import java.util.Map;
@Deprecated
public class MultiMapSolrParams extends org.apache.solr.common.params.MultiMapSolrParams {
  public MultiMapSolrParams(Map<String, String[]> map) {
    super(map);
  }
}
