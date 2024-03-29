package org.apache.lucene.store.instantiated;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
public class TestSerialization extends LuceneTestCase {
  public void test() throws Exception {
    Directory dir = new RAMDirectory();
    IndexWriter iw = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
    Document doc = new Document();
    doc.add(new Field("foo", "bar rab abr bra rba", Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
    doc.add(new Field("moo", "bar rab abr bra rba", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
    iw.addDocument(doc);
    iw.close();
    IndexReader ir = IndexReader.open(dir, false);
    InstantiatedIndex ii = new InstantiatedIndex(ir);
    ir.close();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(ii);
    oos.close();
    baos.close();
  }
}
