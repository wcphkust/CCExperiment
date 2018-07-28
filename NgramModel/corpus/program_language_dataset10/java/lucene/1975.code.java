package org.apache.lucene.search;
import org.apache.lucene.util.LuceneTestCase;
import java.io.IOException;
import java.util.Collection;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Explanation.IDFExplanation;
public class TestSimilarity extends LuceneTestCase {
  public TestSimilarity(String name) {
    super(name);
  }
  public static class SimpleSimilarity extends Similarity {
    @Override public float lengthNorm(String field, int numTerms) { return 1.0f; }
    @Override public float queryNorm(float sumOfSquaredWeights) { return 1.0f; }
    @Override public float tf(float freq) { return freq; }
    @Override public float sloppyFreq(int distance) { return 2.0f; }
    @Override public float idf(int docFreq, int numDocs) { return 1.0f; }
    @Override public float coord(int overlap, int maxOverlap) { return 1.0f; }
    @Override public IDFExplanation idfExplain(Collection<Term> terms, Searcher searcher) throws IOException {
      return new IDFExplanation() {
        @Override
        public float getIdf() {
          return 1.0f;
        }
        @Override
        public String explain() {
          return "Inexplicable";
        }
      };
    }
  }
  public void testSimilarity() throws Exception {
    RAMDirectory store = new RAMDirectory();
    IndexWriter writer = new IndexWriter(store, new IndexWriterConfig(
        TEST_VERSION_CURRENT, new SimpleAnalyzer(TEST_VERSION_CURRENT)).setSimilarity(new SimpleSimilarity()));
    Document d1 = new Document();
    d1.add(new Field("field", "a c", Field.Store.YES, Field.Index.ANALYZED));
    Document d2 = new Document();
    d2.add(new Field("field", "a b c", Field.Store.YES, Field.Index.ANALYZED));
    writer.addDocument(d1);
    writer.addDocument(d2);
    writer.optimize();
    writer.close();
    Searcher searcher = new IndexSearcher(store, true);
    searcher.setSimilarity(new SimpleSimilarity());
    Term a = new Term("field", "a");
    Term b = new Term("field", "b");
    Term c = new Term("field", "c");
    searcher.search(new TermQuery(b), new Collector() {
         private Scorer scorer;
         @Override
        public void setScorer(Scorer scorer) throws IOException {
           this.scorer = scorer; 
         }
         @Override
        public final void collect(int doc) throws IOException {
           assertEquals(1.0f, scorer.score());
         }
         @Override
        public void setNextReader(IndexReader reader, int docBase) {}
         @Override
        public boolean acceptsDocsOutOfOrder() {
           return true;
         }
       });
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(a), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(b), BooleanClause.Occur.SHOULD);
    searcher.search(bq, new Collector() {
         private int base = 0;
         private Scorer scorer;
         @Override
        public void setScorer(Scorer scorer) throws IOException {
           this.scorer = scorer; 
         }
         @Override
        public final void collect(int doc) throws IOException {
           assertEquals((float)doc+base+1, scorer.score());
         }
         @Override
        public void setNextReader(IndexReader reader, int docBase) {
           base = docBase;
         }
         @Override
        public boolean acceptsDocsOutOfOrder() {
           return true;
         }
       });
    PhraseQuery pq = new PhraseQuery();
    pq.add(a);
    pq.add(c);
    searcher.search(pq,
       new Collector() {
         private Scorer scorer;
         @Override
         public void setScorer(Scorer scorer) throws IOException {
          this.scorer = scorer; 
         }
         @Override
         public final void collect(int doc) throws IOException {
           assertEquals(1.0f, scorer.score());
         }
         @Override
         public void setNextReader(IndexReader reader, int docBase) {}
         @Override
         public boolean acceptsDocsOutOfOrder() {
           return true;
         }
       });
    pq.setSlop(2);
    searcher.search(pq, new Collector() {
      private Scorer scorer;
      @Override
      public void setScorer(Scorer scorer) throws IOException {
        this.scorer = scorer; 
      }
      @Override
      public final void collect(int doc) throws IOException {
        assertEquals(2.0f, scorer.score());
      }
      @Override
      public void setNextReader(IndexReader reader, int docBase) {}
      @Override
      public boolean acceptsDocsOutOfOrder() {
        return true;
      }
    });
  }
}