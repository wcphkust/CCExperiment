package org.apache.solr.core;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.core.SolrCore;
import java.util.*;
@Deprecated
public class SolrInfoRegistry {
  @Deprecated
  public static Map<String, SolrInfoMBean> getRegistry() {
    return SolrCore.getSolrCore().getInfoRegistry();
  }
}
