package org.apache.lucene.queryParser.surround.query;
import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryParser.surround.parser.QueryParser;
import junit.framework.TestCase;
import junit.framework.Assert;
public class BooleanQueryTst {
  String queryText;
  final int[] expectedDocNrs;
  SingleFieldTestDb dBase;
  String fieldName;
  TestCase testCase;
  BasicQueryFactory qf;
  boolean verbose = true;
  public BooleanQueryTst(
      String queryText,
      int[] expectedDocNrs,
      SingleFieldTestDb dBase,
      String fieldName,
      TestCase testCase,
      BasicQueryFactory qf) {
    this.queryText = queryText;
    this.expectedDocNrs = expectedDocNrs;
    this.dBase = dBase;
    this.fieldName = fieldName;
    this.testCase = testCase;
    this.qf = qf;
  }
  public void setVerbose(boolean verbose) {this.verbose = verbose;}
  class TestCollector extends Collector { 
    int totalMatched;
    boolean[] encountered;
    private Scorer scorer = null;
    private int docBase = 0;
    TestCollector() {
      totalMatched = 0;
      encountered = new boolean[expectedDocNrs.length];
    }
    @Override
    public void setScorer(Scorer scorer) throws IOException {
      this.scorer = scorer;
    }
    @Override
    public boolean acceptsDocsOutOfOrder() {
      return true;
    }
    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
      this.docBase = docBase;
    }
    @Override
    public void collect(int docNr) throws IOException {
      float score = scorer.score();
      docNr += docBase;
      Assert.assertTrue(queryText + ": positive score", score > 0.0);
      Assert.assertTrue(queryText + ": too many hits", totalMatched < expectedDocNrs.length);
      int i;
      for (i = 0; i < expectedDocNrs.length; i++) {
        if ((! encountered[i]) && (expectedDocNrs[i] == docNr)) {
          encountered[i] = true;
          break;
        }
      }
      if (i == expectedDocNrs.length) {
        Assert.assertTrue(queryText + ": doc nr for hit not expected: " + docNr, false);
      }
      totalMatched++;
    }
    void checkNrHits() {
      Assert.assertEquals(queryText + ": nr of hits", expectedDocNrs.length, totalMatched);
    }
  }
  public void doTest() throws Exception {
    if (verbose) {    
        System.out.println("");
        System.out.println("Query: " + queryText);
    }
    SrndQuery lq = QueryParser.parse(queryText);
    Query query = lq.makeLuceneQueryField(fieldName, qf);
    TestCollector tc = new TestCollector();
    Searcher searcher = new IndexSearcher(dBase.getDb(), true);
    try {
      searcher.search(query, tc);
    } finally {
      searcher.close();
    }
    tc.checkNrHits();
  }
}
