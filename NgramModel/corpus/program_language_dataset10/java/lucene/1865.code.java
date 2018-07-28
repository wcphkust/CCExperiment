package org.apache.lucene.index;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;
public class TestBackwardsCompatibility extends LuceneTestCase {
  public void unzip(String zipName, String destDirName) throws IOException {
    ZipFile zipFile = new ZipFile(zipName + ".zip");
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    String dirName = fullDir(destDirName);
    File fileDir = new File(dirName);
    rmDir(destDirName);
    fileDir.mkdir();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      InputStream in = zipFile.getInputStream(entry);
      OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(fileDir, entry.getName())));
      byte[] buffer = new byte[8192];
      int len;
      while((len = in.read(buffer)) >= 0) {
        out.write(buffer, 0, len);
      }
      in.close();
      out.close();
    }
    zipFile.close();
  }
  public void testCreateCFS() throws IOException {
    String dirName = "testindex.cfs";
    createIndex(dirName, true);
    rmDir(dirName);
  }
  public void testCreateNoCFS() throws IOException {
    String dirName = "testindex.nocfs";
    createIndex(dirName, true);
    rmDir(dirName);
  }
  final String[] oldNames = {"19.cfs",
                             "19.nocfs",
                             "20.cfs",
                             "20.nocfs",
                             "21.cfs",
                             "21.nocfs",
                             "22.cfs",
                             "22.nocfs",
                             "23.cfs",
                             "23.nocfs",
                             "24.cfs",
                             "24.nocfs",
                             "29.cfs",
                             "29.nocfs",
  };
  private void assertCompressedFields29(Directory dir, boolean shouldStillBeCompressed) throws IOException {
    int count = 0;
    final int TEXT_PLAIN_LENGTH = TEXT_TO_COMPRESS.length() * 2;
    final int BINARY_PLAIN_LENGTH = BINARY_TO_COMPRESS.length;
    IndexReader reader = IndexReader.open(dir, true);
    try {
      List<IndexReader> readers = new ArrayList<IndexReader>();
      ReaderUtil.gatherSubReaders(readers, reader);
      for (IndexReader ir : readers) {
        final FieldsReader fr = ((SegmentReader) ir).getFieldsReader();
        assertTrue("for a 2.9 index, FieldsReader.canReadRawDocs() must be false and other way round for a trunk index",
          shouldStillBeCompressed != fr.canReadRawDocs());
      }
      for(int i=0; i<reader.maxDoc(); i++) {
        if (!reader.isDeleted(i)) {
          Document d = reader.document(i);
          if (d.get("content3") != null) continue;
          count++;
          Fieldable compressed = d.getFieldable("compressed");
          if (Integer.parseInt(d.get("id")) % 2 == 0) {
            assertFalse(compressed.isBinary());
            assertEquals("incorrectly decompressed string", TEXT_TO_COMPRESS, compressed.stringValue());
          } else {
            assertTrue(compressed.isBinary());
            assertTrue("incorrectly decompressed binary", Arrays.equals(BINARY_TO_COMPRESS, compressed.getBinaryValue()));
          }
        }
      }
      for(int i=0; i<reader.maxDoc(); i++) {
        if (!reader.isDeleted(i)) {
          Document d = reader.document(i, new FieldSelector() {
            public FieldSelectorResult accept(String fieldName) {
              return ("compressed".equals(fieldName)) ? FieldSelectorResult.SIZE : FieldSelectorResult.LOAD;
            }
          });
          if (d.get("content3") != null) continue;
          count++;
          final DataInputStream ds = new DataInputStream(new ByteArrayInputStream(d.getFieldable("compressed").getBinaryValue()));
          final int actualSize = ds.readInt();
          ds.close();
          final int compressedSize = Integer.parseInt(d.get("compressedSize"));
          final boolean binary = Integer.parseInt(d.get("id")) % 2 > 0;
          final int shouldSize = shouldStillBeCompressed ?
            compressedSize :
            (binary ? BINARY_PLAIN_LENGTH : TEXT_PLAIN_LENGTH);
          assertEquals("size incorrect", shouldSize, actualSize);
          if (!shouldStillBeCompressed) {
            assertFalse("uncompressed field should have another size than recorded in index", compressedSize == actualSize);
          }
        }
      }
      assertEquals("correct number of tests", 34 * 2, count);
    } finally {
      reader.close();
    }
  }
  public void testOptimizeOldIndex() throws IOException {
    int hasTested29 = 0;
    for(int i=0;i<oldNames.length;i++) {
      String dirName = "src/test/org/apache/lucene/index/index." + oldNames[i];
      unzip(dirName, oldNames[i]);
      String fullPath = fullDir(oldNames[i]);
      Directory dir = FSDirectory.open(new File(fullPath));
      if (oldNames[i].startsWith("29.")) {
        assertCompressedFields29(dir, true);
        hasTested29++;
      }
      IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(
          TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
      w.optimize();
      w.close();
      _TestUtil.checkIndex(dir);
      if (oldNames[i].startsWith("29.")) {
        assertCompressedFields29(dir, false);
        hasTested29++;
      }
      dir.close();
      rmDir(oldNames[i]);
    }
    assertEquals("test for compressed field should have run 4 times", 4, hasTested29);
  }
  public void testSearchOldIndex() throws IOException {
    for(int i=0;i<oldNames.length;i++) {
      String dirName = "src/test/org/apache/lucene/index/index." + oldNames[i];
      unzip(dirName, oldNames[i]);
      searchIndex(oldNames[i], oldNames[i]);
      rmDir(oldNames[i]);
    }
  }
  public void testIndexOldIndexNoAdds() throws IOException {
    for(int i=0;i<oldNames.length;i++) {
      String dirName = "src/test/org/apache/lucene/index/index." + oldNames[i];
      unzip(dirName, oldNames[i]);
      changeIndexNoAdds(oldNames[i]);
      rmDir(oldNames[i]);
    }
  }
  public void testIndexOldIndex() throws IOException {
    for(int i=0;i<oldNames.length;i++) {
      String dirName = "src/test/org/apache/lucene/index/index." + oldNames[i];
      unzip(dirName, oldNames[i]);
      changeIndexWithAdds(oldNames[i]);
      rmDir(oldNames[i]);
    }
  }
  private void testHits(ScoreDoc[] hits, int expectedCount, IndexReader reader) throws IOException {
    final int hitCount = hits.length;
    assertEquals("wrong number of hits", expectedCount, hitCount);
    for(int i=0;i<hitCount;i++) {
      reader.document(hits[i].doc);
      reader.getTermFreqVectors(hits[i].doc);
    }
  }
  public void searchIndex(String dirName, String oldName) throws IOException {
    dirName = fullDir(dirName);
    Directory dir = FSDirectory.open(new File(dirName));
    IndexSearcher searcher = new IndexSearcher(dir, true);
    IndexReader reader = searcher.getIndexReader();
    _TestUtil.checkIndex(dir);
    for(int i=0;i<35;i++) {
      if (!reader.isDeleted(i)) {
        Document d = reader.document(i);
        List<Fieldable> fields = d.getFields();
        if (!oldName.startsWith("19.") &&
            !oldName.startsWith("20.") &&
            !oldName.startsWith("21.") &&
            !oldName.startsWith("22.")) {
          if (d.getField("content3") == null) {
            final int numFields = oldName.startsWith("29.") ? 7 : 5;
            assertEquals(numFields, fields.size());
            Field f =  d.getField("id");
            assertEquals(""+i, f.stringValue());
            f = d.getField("utf8");
            assertEquals("Lu\uD834\uDD1Ece\uD834\uDD60ne \u0000 \u2620 ab\ud917\udc17cd", f.stringValue());
            f =  d.getField("autf8");
            assertEquals("Lu\uD834\uDD1Ece\uD834\uDD60ne \u0000 \u2620 ab\ud917\udc17cd", f.stringValue());
            f = d.getField("content2");
            assertEquals("here is more content with aaa aaa aaa", f.stringValue());
            f = d.getField("fie\u2C77ld");
            assertEquals("field with non-ascii name", f.stringValue());
          }
        }       
      } else
        assertEquals(7, i);
    }
    ScoreDoc[] hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    Document d = searcher.doc(hits[0].doc);
    assertEquals("didn't get the right document first", "21", d.get("id"));
    testHits(hits, 34, searcher.getIndexReader());
    if (!oldName.startsWith("19.") &&
        !oldName.startsWith("20.") &&
        !oldName.startsWith("21.") &&
        !oldName.startsWith("22.")) {
      hits = searcher.search(new TermQuery(new Term("utf8", "\u0000")), null, 1000).scoreDocs;
      assertEquals(34, hits.length);
      hits = searcher.search(new TermQuery(new Term("utf8", "Lu\uD834\uDD1Ece\uD834\uDD60ne")), null, 1000).scoreDocs;
      assertEquals(34, hits.length);
      hits = searcher.search(new TermQuery(new Term("utf8", "ab\ud917\udc17cd")), null, 1000).scoreDocs;
      assertEquals(34, hits.length);
    }
    searcher.close();
    dir.close();
  }
  private int compare(String name, String v) {
    int v0 = Integer.parseInt(name.substring(0, 2));
    int v1 = Integer.parseInt(v);
    return v0 - v1;
  }
  public void changeIndexWithAdds(String dirName) throws IOException {
    String origDirName = dirName;
    dirName = fullDir(dirName);
    Directory dir = FSDirectory.open(new File(dirName));
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setOpenMode(OpenMode.APPEND));
    for(int i=0;i<10;i++) {
      addDoc(writer, 35+i);
    }
    final int expected;
    if (compare(origDirName, "24") < 0) {
      expected = 45;
    } else {
      expected = 46;
    }
    assertEquals("wrong doc count", expected, writer.maxDoc());
    writer.close();
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    Document d = searcher.doc(hits[0].doc);
    assertEquals("wrong first document", "21", d.get("id"));
    testHits(hits, 44, searcher.getIndexReader());
    searcher.close();
    IndexReader reader = IndexReader.open(dir, false);
    Term searchTerm = new Term("id", "6");
    int delCount = reader.deleteDocuments(searchTerm);
    assertEquals("wrong delete count", 1, delCount);
    reader.setNorm(22, "content", (float) 2.0);
    reader.close();
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    assertEquals("wrong number of hits", 43, hits.length);
    d = searcher.doc(hits[0].doc);
    assertEquals("wrong first document", "22", d.get("id"));
    testHits(hits, 43, searcher.getIndexReader());
    searcher.close();
    writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setOpenMode(OpenMode.APPEND));
    writer.optimize();
    writer.close();
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    assertEquals("wrong number of hits", 43, hits.length);
    d = searcher.doc(hits[0].doc);
    testHits(hits, 43, searcher.getIndexReader());
    assertEquals("wrong first document", "22", d.get("id"));
    searcher.close();
    dir.close();
  }
  public void changeIndexNoAdds(String dirName) throws IOException {
    dirName = fullDir(dirName);
    Directory dir = FSDirectory.open(new File(dirName));
    IndexSearcher searcher = new IndexSearcher(dir, true);
    ScoreDoc[] hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    assertEquals("wrong number of hits", 34, hits.length);
    Document d = searcher.doc(hits[0].doc);
    assertEquals("wrong first document", "21", d.get("id"));
    searcher.close();
    IndexReader reader = IndexReader.open(dir, false);
    Term searchTerm = new Term("id", "6");
    int delCount = reader.deleteDocuments(searchTerm);
    assertEquals("wrong delete count", 1, delCount);
    reader.setNorm(22, "content", (float) 2.0);
    reader.close();
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    assertEquals("wrong number of hits", 33, hits.length);
    d = searcher.doc(hits[0].doc);
    assertEquals("wrong first document", "22", d.get("id"));
    testHits(hits, 33, searcher.getIndexReader());
    searcher.close();
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setOpenMode(OpenMode.APPEND));
    writer.optimize();
    writer.close();
    searcher = new IndexSearcher(dir, true);
    hits = searcher.search(new TermQuery(new Term("content", "aaa")), null, 1000).scoreDocs;
    assertEquals("wrong number of hits", 33, hits.length);
    d = searcher.doc(hits[0].doc);
    assertEquals("wrong first document", "22", d.get("id"));
    testHits(hits, 33, searcher.getIndexReader());
    searcher.close();
    dir.close();
  }
  public void createIndex(String dirName, boolean doCFS) throws IOException {
    rmDir(dirName);
    dirName = fullDir(dirName);
    Directory dir = FSDirectory.open(new File(dirName));
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setMaxBufferedDocs(10));
    ((LogMergePolicy) writer.getMergePolicy()).setUseCompoundFile(doCFS);
    ((LogMergePolicy) writer.getMergePolicy()).setUseCompoundDocStore(doCFS);
    for(int i=0;i<35;i++) {
      addDoc(writer, i);
    }
    assertEquals("wrong doc count", 35, writer.maxDoc());
    writer.close();
    writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setMaxBufferedDocs(10));
    ((LogMergePolicy) writer.getMergePolicy()).setUseCompoundFile(doCFS);
    ((LogMergePolicy) writer.getMergePolicy()).setUseCompoundDocStore(doCFS);
    addNoProxDoc(writer);
    writer.close();
    IndexReader reader = IndexReader.open(dir, false);
    Term searchTerm = new Term("id", "7");
    int delCount = reader.deleteDocuments(searchTerm);
    assertEquals("didn't delete the right number of documents", 1, delCount);
    reader.setNorm(21, "content", (float) 1.5);
    reader.close();
  }
  public void testExactFileNames() throws IOException {
    String outputDir = "lucene.backwardscompat0.index";
    rmDir(outputDir);
    try {
      Directory dir = FSDirectory.open(new File(fullDir(outputDir)));
      IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
      for(int i=0;i<35;i++) {
        addDoc(writer, i);
      }
      assertEquals("wrong doc count", 35, writer.maxDoc());
      writer.close();
      IndexReader reader = IndexReader.open(dir, false);
      Term searchTerm = new Term("id", "7");
      int delCount = reader.deleteDocuments(searchTerm);
      assertEquals("didn't delete the right number of documents", 1, delCount);
      reader.setNorm(21, "content", (float) 1.5);
      reader.close();
      CompoundFileReader cfsReader = new CompoundFileReader(dir, "_0.cfs");
      FieldInfos fieldInfos = new FieldInfos(cfsReader, "_0.fnm");
      int contentFieldIndex = -1;
      for(int i=0;i<fieldInfos.size();i++) {
        FieldInfo fi = fieldInfos.fieldInfo(i);
        if (fi.name.equals("content")) {
          contentFieldIndex = i;
          break;
        }
      }
      cfsReader.close();
      assertTrue("could not locate the 'content' field number in the _2.cfs segment", contentFieldIndex != -1);
      String[] expected;
      expected = new String[] {"_0.cfs",
                               "_0_1.del",
                               "_0_1.s" + contentFieldIndex,
                               "segments_3",
                               "segments.gen"};
      String[] actual = dir.listAll();
      Arrays.sort(expected);
      Arrays.sort(actual);
      if (!Arrays.equals(expected, actual)) {
        fail("incorrect filenames in index: expected:\n    " + asString(expected) + "\n  actual:\n    " + asString(actual));
      }
      dir.close();
    } finally {
      rmDir(outputDir);
    }
  }
  private String asString(String[] l) {
    String s = "";
    for(int i=0;i<l.length;i++) {
      if (i > 0) {
        s += "\n    ";
      }
      s += l[i];
    }
    return s;
  }
  private void addDoc(IndexWriter writer, int id) throws IOException
  {
    Document doc = new Document();
    doc.add(new Field("content", "aaa", Field.Store.NO, Field.Index.ANALYZED));
    doc.add(new Field("id", Integer.toString(id), Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("autf8", "Lu\uD834\uDD1Ece\uD834\uDD60ne \u0000 \u2620 ab\ud917\udc17cd", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
    doc.add(new Field("utf8", "Lu\uD834\uDD1Ece\uD834\uDD60ne \u0000 \u2620 ab\ud917\udc17cd", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
    doc.add(new Field("content2", "here is more content with aaa aaa aaa", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
    doc.add(new Field("fie\u2C77ld", "field with non-ascii name", Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
    writer.addDocument(doc);
  }
  private void addNoProxDoc(IndexWriter writer) throws IOException {
    Document doc = new Document();
    Field f = new Field("content3", "aaa", Field.Store.YES, Field.Index.ANALYZED);
    f.setOmitTermFreqAndPositions(true);
    doc.add(f);
    f = new Field("content4", "aaa", Field.Store.YES, Field.Index.NO);
    f.setOmitTermFreqAndPositions(true);
    doc.add(f);
    writer.addDocument(doc);
  }
  private void rmDir(String dir) throws IOException {
    File fileDir = new File(fullDir(dir));
    if (fileDir.exists()) {
      File[] files = fileDir.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          files[i].delete();
        }
      }
      fileDir.delete();
    }
  }
  public static String fullDir(String dirName) throws IOException {
    return new File(TEMP_DIR, dirName).getCanonicalPath();
  }
  static final String TEXT_TO_COMPRESS = "this is a compressed field and should appear in 3.0 as an uncompressed field after merge";
  static final byte[] BINARY_TO_COMPRESS = new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
}