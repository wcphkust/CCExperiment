package org.apache.solr.core;
import org.apache.solr.util.AbstractSolrTestCase;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.FooQParserPlugin;
import org.apache.solr.search.ValueSourceParser;
public class SOLR749Test extends AbstractSolrTestCase{
  public String getSchemaFile() {
    return "schema.xml";
  }
  public String getSolrConfigFile() {
    return "solrconfig-SOLR-749.xml";
  }
  public void testConstruction() throws Exception {
    SolrCore core = h.getCore();
    assertTrue("core is null and it shouldn't be", core != null);
    QParserPlugin parserPlugin = core.getQueryPlugin(QParserPlugin.DEFAULT_QTYPE);
    assertTrue("parserPlugin is null and it shouldn't be", parserPlugin != null);
    assertTrue("parserPlugin is not an instanceof " + FooQParserPlugin.class, parserPlugin instanceof FooQParserPlugin);
    ValueSourceParser vsp = core.getValueSourceParser("boost");
    assertTrue("vsp is null and it shouldn't be", vsp != null);
    assertTrue("vsp is not an instanceof " + DummyValueSourceParser.class, vsp instanceof DummyValueSourceParser);
  }
}
