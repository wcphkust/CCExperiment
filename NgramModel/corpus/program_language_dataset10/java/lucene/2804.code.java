package org.apache.solr.handler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MoreLikeThisParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.AbstractSolrTestCase;
public class MoreLikeThisHandlerTest extends AbstractSolrTestCase {
  @Override public String getSchemaFile() { return "schema.xml"; }
  @Override public String getSolrConfigFile() { return "solrconfig.xml"; }
  @Override public void setUp() throws Exception {
    super.setUp();
    lrf = h.getRequestFactory("standard", 0, 20 );
  }
  public void testInterface() throws Exception
  {
    SolrCore core = h.getCore();
    MoreLikeThisHandler mlt = new MoreLikeThisHandler();
    Map<String,String[]> params = new HashMap<String,String[]>();
    MultiMapSolrParams mmparams = new MultiMapSolrParams( params );
    SolrQueryRequestBase req = new SolrQueryRequestBase( core, (SolrParams)mmparams ) {};
    try {
      mlt.handleRequestBody( req, new SolrQueryResponse() );
    }
    catch( Exception ex ) {} 
    try {
      ArrayList<ContentStream> streams = new ArrayList<ContentStream>( 2 );
      streams.add( new ContentStreamBase.StringStream( "hello" ) );
      streams.add( new ContentStreamBase.StringStream( "there" ) );
      req.setContentStreams( streams );
      mlt.handleRequestBody( req, new SolrQueryResponse() );
    }
    catch( Exception ex ) {} 
    assertU(adoc("id","42","name","Tom Cruise","subword","Top Gun","subword","Risky Business","subword","The Color of Money","subword","Minority Report","subword", "Days of Thunder","subword", "Eyes Wide Shut","subword", "Far and Away", "foo_ti","10"));
    assertU(adoc("id","43","name","Tom Hanks","subword","The Green Mile","subword","Forest Gump","subword","Philadelphia Story","subword","Big","subword","Cast Away", "foo_ti","10"));
    assertU(adoc("id","44","name","Harrison Ford","subword","Star Wars","subword","Indiana Jones","subword","Patriot Games","subword","Regarding Henry"));
    assertU(adoc("id","45","name","George Harrison","subword","Yellow Submarine","subword","Help","subword","Magical Mystery Tour","subword","Sgt. Peppers Lonley Hearts Club Band"));
    assertU(adoc("id","46","name","Nicole Kidman","subword","Batman","subword","Days of Thunder","subword","Eyes Wide Shut","subword","Far and Away"));
    assertU(commit());
    params.put(CommonParams.Q, new String[]{"id:42"});
    params.put(MoreLikeThisParams.MLT, new String[]{"true"});
    params.put(MoreLikeThisParams.SIMILARITY_FIELDS, new String[]{"name,subword,foo_ti"});
    params.put(MoreLikeThisParams.INTERESTING_TERMS,new String[]{"details"});
    params.put(MoreLikeThisParams.MIN_TERM_FREQ,new String[]{"1"});
    params.put(MoreLikeThisParams.MIN_DOC_FREQ,new String[]{"1"});
    params.put("indent",new String[]{"true"});
    SolrQueryRequest mltreq = new LocalSolrQueryRequest( core, (SolrParams)mmparams);
    assertQ("morelikethis - tom cruise",mltreq
        ,"//result/doc[1]/int[@name='id'][.='46']"
        ,"//result/doc[2]/int[@name='id'][.='43']");
    params.put(CommonParams.Q, new String[]{"id:44"});
    assertQ("morelike this - harrison ford",mltreq
        ,"//result/doc[1]/int[@name='id'][.='45']");
    params.put(CommonParams.Q, new String[]{"id:42"}); 
    params.put(MoreLikeThisParams.QF,new String[]{"name^5.0 subword^0.1"});
    assertQ("morelikethis with weights",mltreq
        ,"//result/doc[1]/int[@name='id'][.='43']"
        ,"//result/doc[2]/int[@name='id'][.='46']");
  }
}
