package org.apache.solr.request;
import java.io.IOException;
import java.io.Writer;
import java.io.StringWriter;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.response.JSONResponseWriter;
import org.apache.solr.response.PHPSerializedResponseWriter;
import org.apache.solr.response.PythonResponseWriter;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.RubyResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.AbstractSolrTestCase;
public class JSONWriterTest extends AbstractSolrTestCase {
  public String getSchemaFile() { return "schema.xml"; }
  public String getSolrConfigFile() { return "solrconfig.xml"; }
  public void testNaNInf() throws IOException {
    SolrQueryRequest req = req("dummy");
    SolrQueryResponse rsp = new SolrQueryResponse();
    QueryResponseWriter w = new PythonResponseWriter();
    StringWriter buf = new StringWriter();
    rsp.add("data1", Float.NaN);
    rsp.add("data2", Double.NEGATIVE_INFINITY);
    rsp.add("data3", Float.POSITIVE_INFINITY);
    w.write(buf, req, rsp);
    assertEquals(buf.toString(), "{'data1':float('NaN'),'data2':-float('Inf'),'data3':float('Inf')}");
    w = new RubyResponseWriter();
    buf = new StringWriter();
    w.write(buf, req, rsp);
    assertEquals(buf.toString(), "{'data1'=>(0.0/0.0),'data2'=>-(1.0/0.0),'data3'=>(1.0/0.0)}");
  }
  public void testPHPS() throws IOException {
    SolrQueryRequest req = req("dummy");
    SolrQueryResponse rsp = new SolrQueryResponse();
    QueryResponseWriter w = new PHPSerializedResponseWriter();
    StringWriter buf = new StringWriter();
    rsp.add("data1", "hello");
    rsp.add("data2", 42);
    rsp.add("data3", true);
    w.write(buf, req, rsp);
    assertEquals(buf.toString(), "a:3:{s:5:\"data1\";s:5:\"hello\";s:5:\"data2\";i:42;s:5:\"data3\";b:1;}");
  }
  public void testJSON() throws IOException {
    SolrQueryRequest req = req("wt","json","json.nl","arrarr");
    SolrQueryResponse rsp = new SolrQueryResponse();
    JSONResponseWriter w = new JSONResponseWriter();
    StringWriter buf = new StringWriter();
    NamedList nl = new NamedList();
    nl.add("data1", "hello");
    nl.add(null, 42);
    rsp.add("nl", nl);
    w.write(buf, req, rsp);
    assertEquals(buf.toString(), "{\"nl\":[[\"data1\",\"hello\"],[null,42]]}");
  }
}
