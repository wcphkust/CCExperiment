package org.apache.solr.handler.clustering.carrot2;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.clustering.AbstractClusteringTest;
import org.apache.solr.handler.clustering.ClusteringComponent;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.carrot2.util.attribute.AttributeUtils;
import java.io.IOException;
import java.util.List;
@SuppressWarnings("unchecked")
public class CarrotClusteringEngineTest extends AbstractClusteringTest {
  public void testCarrotLingo() throws Exception {
    checkEngine(getClusteringEngine("default"), 10);
  }
  public void testProduceSummary() throws Exception {
    ModifiableSolrParams solrParams = new ModifiableSolrParams();
    solrParams.add(CarrotParams.SNIPPET_FIELD_NAME, "snippet");
    solrParams.add(CarrotParams.SUMMARY_FRAGSIZE, "200");
    checkEngine(getClusteringEngine("default"), numberOfDocs -2 , 15, new TermQuery(new Term("snippet", "mine")), solrParams);
  }
  public void testCarrotStc() throws Exception {
    checkEngine(getClusteringEngine("stc"), 1);
  }
  public void testWithoutSubclusters() throws Exception {
    checkClusters(checkEngine(getClusteringEngine("mock"), this.numberOfDocs),
            1, 1, 0);
  }
  public void testWithSubclusters() throws Exception {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(CarrotParams.OUTPUT_SUB_CLUSTERS, true);
    checkClusters(checkEngine(getClusteringEngine("mock"), this.numberOfDocs), 1, 1, 2);
  }
  public void testNumDescriptions() throws Exception {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(AttributeUtils.getKey(MockClusteringAlgorithm.class, "labels"), 5);
    params.set(CarrotParams.NUM_DESCRIPTIONS, 3);
    checkClusters(checkEngine(getClusteringEngine("mock"), this.numberOfDocs,
            params), 1, 3, 0);
  }
  public void testCarrotAttributePassing() throws Exception {
    ModifiableSolrParams params = new ModifiableSolrParams();
    params.set(AttributeUtils.getKey(MockClusteringAlgorithm.class, "depth"), 1);
    params.set(AttributeUtils.getKey(MockClusteringAlgorithm.class, "labels"), 3);
    checkClusters(checkEngine(getClusteringEngine("mock"), this.numberOfDocs,
            params), 1, 3, 0);
  }
  private CarrotClusteringEngine getClusteringEngine(String engineName) {
    ClusteringComponent comp = (ClusteringComponent) h.getCore()
            .getSearchComponent("clustering");
    assertNotNull("clustering component should not be null", comp);
    CarrotClusteringEngine engine = (CarrotClusteringEngine) comp
            .getSearchClusteringEngines().get(engineName);
    assertNotNull("clustering engine for name: " + engineName
            + " should not be null", engine);
    return engine;
  }
  private List checkEngine(CarrotClusteringEngine engine,
                            int expectedNumClusters) throws IOException {
    return checkEngine(engine, numberOfDocs, expectedNumClusters, new MatchAllDocsQuery(), new ModifiableSolrParams());
  }
  private List checkEngine(CarrotClusteringEngine engine,
                            int expectedNumClusters, SolrParams clusteringParams) throws IOException {
    return checkEngine(engine, numberOfDocs, expectedNumClusters, new MatchAllDocsQuery(), clusteringParams);
  }
  private List checkEngine(CarrotClusteringEngine engine, int expectedNumDocs,
                           int expectedNumClusters, Query query, SolrParams clusteringParams) throws IOException {
    RefCounted<SolrIndexSearcher> ref = h.getCore().getSearcher();
    DocList docList;
    try {
      SolrIndexSearcher searcher = ref.get();
      docList = searcher.getDocList(query, (Query) null, new Sort(), 0,
              numberOfDocs);
      assertEquals("docList size", expectedNumDocs, docList.matches());
    } finally {
      ref.decref();
    }
    ModifiableSolrParams solrParams = new ModifiableSolrParams();
    solrParams.add(CarrotParams.PRODUCE_SUMMARY, "true");
    solrParams.add(clusteringParams);
    LocalSolrQueryRequest req = new LocalSolrQueryRequest(h.getCore(), solrParams);
    List results = (List) engine.cluster(query, docList, req);
    req.close();
    assertEquals("number of clusters: " + results, expectedNumClusters, results.size());
    checkClusters(results, false);
    return results;
  }
  private void checkClusters(List results, int expectedDocCount,
                             int expectedLabelCount, int expectedSubclusterCount) {
    for (int i = 0; i < results.size(); i++) {
      NamedList cluster = (NamedList) results.get(i);
      checkCluster(cluster, expectedDocCount, expectedLabelCount,
              expectedSubclusterCount);
    }
  }
  private void checkClusters(List results, boolean hasSubclusters) {
    for (int i = 0; i < results.size(); i++) {
      checkCluster((NamedList) results.get(i), hasSubclusters);
    }
  }
  private void checkCluster(NamedList cluster, boolean hasSubclusters) {
    List docs = (List) cluster.get("docs");
    assertNotNull("docs is null and it shouldn't be", docs);
    for (int j = 0; j < docs.size(); j++) {
      String id = (String) docs.get(j);
      assertNotNull("id is null and it shouldn't be", id);
    }
    List labels = (List) cluster.get("labels");
    assertNotNull("labels is null but it shouldn't be", labels);
    if (hasSubclusters) {
      List subclusters = (List) cluster.get("clusters");
      assertNotNull("subclusters is null but it shouldn't be", subclusters);
    }
  }
  private void checkCluster(NamedList cluster, int expectedDocCount,
                            int expectedLabelCount, int expectedSubclusterCount) {
    checkCluster(cluster, expectedSubclusterCount > 0);
    assertEquals("number of docs in cluster", expectedDocCount,
            ((List) cluster.get("docs")).size());
    assertEquals("number of labels in cluster", expectedLabelCount,
            ((List) cluster.get("labels")).size());
    if (expectedSubclusterCount > 0) {
      List subclusters = (List) cluster.get("clusters");
      assertEquals("numClusters", expectedSubclusterCount, subclusters.size());
      assertEquals("number of subclusters in cluster",
              expectedSubclusterCount, subclusters.size());
    }
  }
}