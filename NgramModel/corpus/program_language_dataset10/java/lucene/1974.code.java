package org.apache.lucene.search;
import java.io.IOException;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;
public class TestSetNorm extends LuceneTestCase {
  public TestSetNorm(String name) {
    super(name);
  }
  public void testSetNorm() throws Exception {
    RAMDirectory store = new RAMDirectory();
    IndexWriter writer = new IndexWriter(store, new IndexWriterConfig(TEST_VERSION_CURRENT, new SimpleAnalyzer(TEST_VERSION_CURRENT)));
    Fieldable f1 = new Field("field", "word", Field.Store.YES, Field.Index.ANALYZED);
    Document d1 = new Document();
    d1.add(f1);
    writer.addDocument(d1);
    writer.addDocument(d1);
    writer.addDocument(d1);
    writer.addDocument(d1);
    writer.close();
    IndexReader reader = IndexReader.open(store, false);
    reader.setNorm(0, "field", 1.0f);
    reader.setNorm(1, "field", 2.0f);
    reader.setNorm(2, "field", 4.0f);
    reader.setNorm(3, "field", 16.0f);
    reader.close();
    final float[] scores = new float[4];
    new IndexSearcher(store, true).search
      (new TermQuery(new Term("field", "word")),
       new Collector() {
         private int base = 0;
         private Scorer scorer;
         @Override
         public void setScorer(Scorer scorer) throws IOException {
          this.scorer = scorer;
         }
         @Override
         public final void collect(int doc) throws IOException {
           scores[doc + base] = scorer.score();
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
    float lastScore = 0.0f;
    for (int i = 0; i < 4; i++) {
      assertTrue(scores[i] > lastScore);
      lastScore = scores[i];
    }
  }
}
