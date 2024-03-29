package org.apache.lucene.store;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.NIOFSDirectory.NIOFSIndexInput;
import org.apache.lucene.store.SimpleFSDirectory.SimpleFSIndexInput;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;
import org.apache.lucene.util.ArrayUtil;
public class TestBufferedIndexInput extends LuceneTestCase {
  private static void writeBytes(File aFile, long size) throws IOException{
    OutputStream stream = null;
    try {
      stream = new FileOutputStream(aFile);
      for (int i = 0; i < size; i++) {
        stream.write(byten(i));  
      }
      stream.flush();
    } finally {
      if (stream != null) {
        stream.close();
      }
    }
  }
  private static final long TEST_FILE_LENGTH = 100*1024;
  public void testReadByte() throws Exception {
    MyBufferedIndexInput input = new MyBufferedIndexInput();
    for (int i = 0; i < BufferedIndexInput.BUFFER_SIZE * 10; i++) {
      assertEquals(input.readByte(), byten(i));
    }
  }
  public void testReadBytes() throws Exception {
    final Random r = newRandom();
    MyBufferedIndexInput input = new MyBufferedIndexInput();
    runReadBytes(input, BufferedIndexInput.BUFFER_SIZE, r);
    final int inputBufferSize = 128;
    File tmpInputFile = File.createTempFile("IndexInput", "tmpFile");
    tmpInputFile.deleteOnExit();
    writeBytes(tmpInputFile, TEST_FILE_LENGTH);
    runReadBytesAndClose(new SimpleFSIndexInput(tmpInputFile,
                                                inputBufferSize, 10), inputBufferSize, r);
    runReadBytesAndClose(new NIOFSIndexInput(tmpInputFile,
                                             inputBufferSize, 10), inputBufferSize, r);
  }
  private void runReadBytesAndClose(IndexInput input, int bufferSize, Random r)
      throws IOException {
    try {
      runReadBytes(input, bufferSize, r);
    } finally {
      input.close();
    }
  }
  private void runReadBytes(IndexInput input, int bufferSize, Random r)
      throws IOException {
    int pos = 0;
    for (int size = 1; size < bufferSize * 10; size = size + size / 200 + 1) {
      checkReadBytes(input, size, pos);
      pos += size;
      if (pos >= TEST_FILE_LENGTH) {
        pos = 0;
        input.seek(0L);
      }
    }
    for (long i = 0; i < 100; i++) {
      final int size = r.nextInt(10000);
      checkReadBytes(input, 1+size, pos);
      pos += 1+size;
      if (pos >= TEST_FILE_LENGTH) {
        pos = 0;
        input.seek(0L);
      }
    }
    for (int i = 0; i < bufferSize; i++) {
      checkReadBytes(input, 7, pos);
      pos += 7;
      if (pos >= TEST_FILE_LENGTH) {
        pos = 0;
        input.seek(0L);
      }
    }
  }
  private byte[] buffer = new byte[10];
  private void checkReadBytes(IndexInput input, int size, int pos) throws IOException{
    int offset = size % 10; 
    buffer = ArrayUtil.grow(buffer, offset+size);
    assertEquals(pos, input.getFilePointer());
    long left = TEST_FILE_LENGTH - input.getFilePointer();
    if (left <= 0) {
      return;
    } else if (left < size) {
      size = (int) left;
    }
    input.readBytes(buffer, offset, size);
    assertEquals(pos+size, input.getFilePointer());
    for(int i=0; i<size; i++) {
      assertEquals("pos=" + i + " filepos=" + (pos+i), byten(pos+i), buffer[offset+i]);
    }
  }
  public void testEOF() throws Exception {
     MyBufferedIndexInput input = new MyBufferedIndexInput(1024);
     checkReadBytes(input, (int)input.length(), 0);  
     int pos = (int)input.length()-10;
     input.seek(pos);
     checkReadBytes(input, 10, pos);  
     input.seek(pos);
     try {
       checkReadBytes(input, 11, pos);
           fail("Block read past end of file");
       } catch (IOException e) {
       }
     input.seek(pos);
     try {
       checkReadBytes(input, 50, pos);
           fail("Block read past end of file");
       } catch (IOException e) {
       }
     input.seek(pos);
     try {
       checkReadBytes(input, 100000, pos);
           fail("Block read past end of file");
       } catch (IOException e) {
       }
  }
    private static byte byten(long n){
      return (byte)(n*n%256);
    }
    private static class MyBufferedIndexInput extends BufferedIndexInput {
      private long pos;
      private long len;
      public MyBufferedIndexInput(long len){
        this.len = len;
        this.pos = 0;
      }
      public MyBufferedIndexInput(){
        this(Long.MAX_VALUE);
      }
      @Override
      protected void readInternal(byte[] b, int offset, int length) throws IOException {
        for(int i=offset; i<offset+length; i++)
          b[i] = byten(pos++);
      }
      @Override
      protected void seekInternal(long pos) throws IOException {
        this.pos = pos;
      }
      @Override
      public void close() throws IOException {
      }
      @Override
      public long length() {
        return len;
      }
    }
    public void testSetBufferSize() throws IOException {
      File indexDir = new File(TEMP_DIR, "testSetBufferSize");
      MockFSDirectory dir = new MockFSDirectory(indexDir, newRandom());
      try {
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
          TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT))
          .setOpenMode(OpenMode.CREATE));
        ((LogMergePolicy) writer.getMergePolicy()).setUseCompoundFile(false);
        for(int i=0;i<37;i++) {
          Document doc = new Document();
          doc.add(new Field("content", "aaa bbb ccc ddd" + i, Field.Store.YES, Field.Index.ANALYZED));
          doc.add(new Field("id", "" + i, Field.Store.YES, Field.Index.ANALYZED));
          writer.addDocument(doc);
        }
        writer.close();
        dir.allIndexInputs.clear();
        IndexReader reader = IndexReader.open(dir, false);
        Term aaa = new Term("content", "aaa");
        Term bbb = new Term("content", "bbb");
        Term ccc = new Term("content", "ccc");
        assertEquals(37, reader.docFreq(ccc));
        reader.deleteDocument(0);
        assertEquals(37, reader.docFreq(aaa));
        dir.tweakBufferSizes();
        reader.deleteDocument(4);
        assertEquals(reader.docFreq(bbb), 37);
        dir.tweakBufferSizes();
        IndexSearcher searcher = new IndexSearcher(reader);
        ScoreDoc[] hits = searcher.search(new TermQuery(bbb), null, 1000).scoreDocs;
        dir.tweakBufferSizes();
        assertEquals(35, hits.length);
        dir.tweakBufferSizes();
        hits = searcher.search(new TermQuery(new Term("id", "33")), null, 1000).scoreDocs;
        dir.tweakBufferSizes();
        assertEquals(1, hits.length);
        hits = searcher.search(new TermQuery(aaa), null, 1000).scoreDocs;
        dir.tweakBufferSizes();
        assertEquals(35, hits.length);
        searcher.close();
        reader.close();
      } finally {
        _TestUtil.rmDir(indexDir);
      }
    }
    private static class MockFSDirectory extends Directory {
      List<IndexInput> allIndexInputs = new ArrayList<IndexInput>();
      Random rand;
      private Directory dir;
      public MockFSDirectory(File path, Random rand) throws IOException {
        this.rand = rand;
        lockFactory = NoLockFactory.getNoLockFactory();
        dir = new SimpleFSDirectory(path, null);
      }
      @Override
      public IndexInput openInput(String name) throws IOException {
        return openInput(name, BufferedIndexInput.BUFFER_SIZE);
      }
      public void tweakBufferSizes() {
        for (final IndexInput ip : allIndexInputs) {
          BufferedIndexInput bii = (BufferedIndexInput) ip;
          int bufferSize = 1024+Math.abs(rand.nextInt() % 32768);
          bii.setBufferSize(bufferSize);
        }
      }
      @Override
      public IndexInput openInput(String name, int bufferSize) throws IOException {
        bufferSize = 1+Math.abs(rand.nextInt() % 10);
        IndexInput f = dir.openInput(name, bufferSize);
        allIndexInputs.add(f);
        return f;
      }
      @Override
      public IndexOutput createOutput(String name) throws IOException {
        return dir.createOutput(name);
      }
      @Override
      public void close() throws IOException {
        dir.close();
      }
      @Override
      public void deleteFile(String name)
        throws IOException
      {
        dir.deleteFile(name);
      }
      @Override
      public void touchFile(String name)
        throws IOException
      {
        dir.touchFile(name);
      }
      @Override
      public long fileModified(String name)
        throws IOException
      {
        return dir.fileModified(name);
      }
      @Override
      public boolean fileExists(String name)
        throws IOException
      {
        return dir.fileExists(name);
      }
      @Override
      public String[] listAll()
        throws IOException
      {
        return dir.listAll();
      }
      @Override
      public long fileLength(String name) throws IOException {
        return dir.fileLength(name);
      }
    }
}
