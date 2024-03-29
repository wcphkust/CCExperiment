package org.apache.solr.highlight;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;
public interface SolrFragmentsBuilder extends SolrInfoMBean, NamedListInitializedPlugin {
  public void init( NamedList args);
  public FragmentsBuilder getFragmentsBuilder( SolrParams params );
}
