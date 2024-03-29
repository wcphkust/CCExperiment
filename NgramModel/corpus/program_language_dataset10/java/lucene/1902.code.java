package org.apache.lucene.index;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util._TestUtil;
public class TestPayloads extends LuceneTestCase {
    public void testPayload() throws Exception {
        rnd = newRandom();
        byte[] testData = "This is a test!".getBytes();
        Payload payload = new Payload(testData);
        assertEquals("Wrong payload length.", testData.length, payload.length());
        byte[] target = new byte[testData.length - 1];
        try {
            payload.copyTo(target, 0);
            fail("Expected exception not thrown");
        } catch (Exception expected) {
        }
        target = new byte[testData.length + 3];
        payload.copyTo(target, 3);
        for (int i = 0; i < testData.length; i++) {
            assertEquals(testData[i], target[i + 3]);
        }
        target = payload.toByteArray();
        assertByteArrayEquals(testData, target);
        for (int i = 0; i < testData.length; i++) {
            assertEquals(payload.byteAt(i), testData[i]);
        }
        try {
            payload.byteAt(testData.length + 1);
            fail("Expected exception not thrown");
        } catch (Exception expected) {
        }
        Payload clone = (Payload) payload.clone();
        assertEquals(payload.length(), clone.length());
        for (int i = 0; i < payload.length(); i++) {
          assertEquals(payload.byteAt(i), clone.byteAt(i));
        }
    }
    public void testPayloadFieldBit() throws Exception {
        rnd = newRandom();
        Directory ram = new RAMDirectory();
        PayloadAnalyzer analyzer = new PayloadAnalyzer();
        IndexWriter writer = new IndexWriter(ram, new IndexWriterConfig(TEST_VERSION_CURRENT, analyzer));
        Document d = new Document();
        d.add(new Field("f1", "This field has no payloads", Field.Store.NO, Field.Index.ANALYZED));
        d.add(new Field("f2", "This field has payloads in all docs", Field.Store.NO, Field.Index.ANALYZED));
        d.add(new Field("f2", "This field has payloads in all docs", Field.Store.NO, Field.Index.ANALYZED));
        d.add(new Field("f3", "This field has payloads in some docs", Field.Store.NO, Field.Index.ANALYZED));
        analyzer.setPayloadData("f2", 1, "somedata".getBytes(), 0, 1);
        writer.addDocument(d);
        writer.close();        
        SegmentReader reader = SegmentReader.getOnlySegmentReader(ram);
        FieldInfos fi = reader.fieldInfos();
        assertFalse("Payload field bit should not be set.", fi.fieldInfo("f1").storePayloads);
        assertTrue("Payload field bit should be set.", fi.fieldInfo("f2").storePayloads);
        assertFalse("Payload field bit should not be set.", fi.fieldInfo("f3").storePayloads);
        reader.close();
        writer = new IndexWriter(ram, new IndexWriterConfig(TEST_VERSION_CURRENT,
            analyzer).setOpenMode(OpenMode.CREATE));
        d = new Document();
        d.add(new Field("f1", "This field has no payloads", Field.Store.NO, Field.Index.ANALYZED));
        d.add(new Field("f2", "This field has payloads in all docs", Field.Store.NO, Field.Index.ANALYZED));
        d.add(new Field("f2", "This field has payloads in all docs", Field.Store.NO, Field.Index.ANALYZED));
        d.add(new Field("f3", "This field has payloads in some docs", Field.Store.NO, Field.Index.ANALYZED));
        analyzer.setPayloadData("f2", "somedata".getBytes(), 0, 1);
        analyzer.setPayloadData("f3", "somedata".getBytes(), 0, 3);
        writer.addDocument(d);
        writer.optimize();
        writer.close();
        reader = SegmentReader.getOnlySegmentReader(ram);
        fi = reader.fieldInfos();
        assertFalse("Payload field bit should not be set.", fi.fieldInfo("f1").storePayloads);
        assertTrue("Payload field bit should be set.", fi.fieldInfo("f2").storePayloads);
        assertTrue("Payload field bit should be set.", fi.fieldInfo("f3").storePayloads);
        reader.close();        
    }
    public void testPayloadsEncoding() throws Exception {
        rnd = newRandom();
        Directory dir = new RAMDirectory();
        performTest(dir);
        File dirName = _TestUtil.getTempDir("test_payloads");
        dir = FSDirectory.open(dirName);
        performTest(dir);
       _TestUtil.rmDir(dirName);
    }
    private void performTest(Directory dir) throws Exception {
        PayloadAnalyzer analyzer = new PayloadAnalyzer();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
            TEST_VERSION_CURRENT, analyzer)
            .setOpenMode(OpenMode.CREATE));
        final int skipInterval = 16;
        final int numTerms = 5;
        final String fieldName = "f1";
        int numDocs = skipInterval + 1; 
        Term[] terms = generateTerms(fieldName, numTerms);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms.length; i++) {
            sb.append(terms[i].text);
            sb.append(" ");
        }
        String content = sb.toString();
        int payloadDataLength = numTerms * numDocs * 2 + numTerms * numDocs * (numDocs - 1) / 2;
        byte[] payloadData = generateRandomData(payloadDataLength);
        Document d = new Document();
        d.add(new Field(fieldName, content, Field.Store.NO, Field.Index.ANALYZED));
        int offset = 0;
        for (int i = 0; i < 2 * numDocs; i++) {
            analyzer.setPayloadData(fieldName, payloadData, offset, 1);
            offset += numTerms;
            writer.addDocument(d);
        }
        writer.commit();
        for (int i = 0; i < numDocs; i++) {
            analyzer.setPayloadData(fieldName, payloadData, offset, i);
            offset += i * numTerms;
            writer.addDocument(d);
        }
        writer.optimize();
        writer.close();
        IndexReader reader = IndexReader.open(dir, true);
        byte[] verifyPayloadData = new byte[payloadDataLength];
        offset = 0;
        TermPositions[] tps = new TermPositions[numTerms];
        for (int i = 0; i < numTerms; i++) {
            tps[i] = reader.termPositions(terms[i]);
        }
        while (tps[0].next()) {
            for (int i = 1; i < numTerms; i++) {
                tps[i].next();
            }
            int freq = tps[0].freq();
            for (int i = 0; i < freq; i++) {
                for (int j = 0; j < numTerms; j++) {
                    tps[j].nextPosition();
                    tps[j].getPayload(verifyPayloadData, offset);
                    offset += tps[j].getPayloadLength();
                }
            }
        }
        for (int i = 0; i < numTerms; i++) {
            tps[i].close();
        }
        assertByteArrayEquals(payloadData, verifyPayloadData);
        TermPositions tp = reader.termPositions(terms[0]);
        tp.next();
        tp.nextPosition();
        tp.nextPosition();
        assertEquals("Wrong payload length.", 1, tp.getPayloadLength());
        byte[] payload = tp.getPayload(null, 0);
        assertEquals(payload[0], payloadData[numTerms]);
        tp.nextPosition();
        tp.skipTo(5);
        tp.nextPosition();
        assertEquals("Wrong payload length.", 1, tp.getPayloadLength());
        payload = tp.getPayload(null, 0);
        assertEquals(payload[0], payloadData[5 * numTerms]);
        tp.seek(terms[1]);
        tp.next();
        tp.nextPosition();
        assertEquals("Wrong payload length.", 1, tp.getPayloadLength());
        tp.skipTo(skipInterval - 1);
        tp.nextPosition();
        assertEquals("Wrong payload length.", 1, tp.getPayloadLength());
        tp.skipTo(2 * skipInterval - 1);
        tp.nextPosition();
        assertEquals("Wrong payload length.", 1, tp.getPayloadLength());
        tp.skipTo(3 * skipInterval - 1);
        tp.nextPosition();
        assertEquals("Wrong payload length.", 3 * skipInterval - 2 * numDocs - 1, tp.getPayloadLength());
        tp.getPayload(null, 0);
        try {
            tp.getPayload(null, 0);
            fail("Expected exception not thrown");
        } catch (Exception expected) {
        }
        reader.close();
        analyzer = new PayloadAnalyzer();
        writer = new IndexWriter(dir, new IndexWriterConfig(TEST_VERSION_CURRENT,
            analyzer).setOpenMode(OpenMode.CREATE));
        String singleTerm = "lucene";
        d = new Document();
        d.add(new Field(fieldName, singleTerm, Field.Store.NO, Field.Index.ANALYZED));
        payloadData = generateRandomData(2000);
        analyzer.setPayloadData(fieldName, payloadData, 100, 1500);
        writer.addDocument(d);
        writer.optimize();
        writer.close();
        reader = IndexReader.open(dir, true);
        tp = reader.termPositions(new Term(fieldName, singleTerm));
        tp.next();
        tp.nextPosition();
        verifyPayloadData = new byte[tp.getPayloadLength()];
        tp.getPayload(verifyPayloadData, 0);
        byte[] portion = new byte[1500];
        System.arraycopy(payloadData, 100, portion, 0, 1500);
        assertByteArrayEquals(portion, verifyPayloadData);
        reader.close();
    }
    private Random rnd;
    private void generateRandomData(byte[] data) {
        rnd.nextBytes(data);
    }
    private byte[] generateRandomData(int n) {
        byte[] data = new byte[n];
        generateRandomData(data);
        return data;
    }
    private Term[] generateTerms(String fieldName, int n) {
        int maxDigits = (int) (Math.log(n) / Math.log(10));
        Term[] terms = new Term[n];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.setLength(0);
            sb.append("t");
            int zeros = maxDigits - (int) (Math.log(i) / Math.log(10));
            for (int j = 0; j < zeros; j++) {
                sb.append("0");
            }
            sb.append(i);
            terms[i] = new Term(fieldName, sb.toString());
        }
        return terms;
    }
    void assertByteArrayEquals(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
          fail("Byte arrays have different lengths: " + b1.length + ", " + b2.length);
        }
        for (int i = 0; i < b1.length; i++) {
          if (b1[i] != b2[i]) {
            fail("Byte arrays different at index " + i + ": " + b1[i] + ", " + b2[i]);
          }
        }
      }    
    private static class PayloadAnalyzer extends Analyzer {
        Map<String,PayloadData> fieldToData = new HashMap<String,PayloadData>();
        void setPayloadData(String field, byte[] data, int offset, int length) {
            fieldToData.put(field, new PayloadData(0, data, offset, length));
        }
        void setPayloadData(String field, int numFieldInstancesToSkip, byte[] data, int offset, int length) {
            fieldToData.put(field, new PayloadData(numFieldInstancesToSkip, data, offset, length));
        }
        @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
            PayloadData payload =  fieldToData.get(fieldName);
            TokenStream ts = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
            if (payload != null) {
                if (payload.numFieldInstancesToSkip == 0) {
                    ts = new PayloadFilter(ts, payload.data, payload.offset, payload.length);
                } else {
                    payload.numFieldInstancesToSkip--;
                }
            }
            return ts;
        }
        private static class PayloadData {
            byte[] data;
            int offset;
            int length;
            int numFieldInstancesToSkip;
            PayloadData(int skip, byte[] data, int offset, int length) {
                numFieldInstancesToSkip = skip;
                this.data = data;
                this.offset = offset;
                this.length = length;
            }
        }
    }
    private static class PayloadFilter extends TokenFilter {
        private byte[] data;
        private int length;
        private int offset;
        Payload payload = new Payload();
        PayloadAttribute payloadAtt;
        public PayloadFilter(TokenStream in, byte[] data, int offset, int length) {
            super(in);
            this.data = data;
            this.length = length;
            this.offset = offset;
            payloadAtt = addAttribute(PayloadAttribute.class);
        }
        @Override
        public boolean incrementToken() throws IOException {
            boolean hasNext = input.incrementToken();
            if (hasNext) {
                if (offset + length <= data.length) {
                    Payload p = new Payload();
                    payloadAtt.setPayload(p);
                    p.setData(data, offset, length);
                    offset += length;                
                } else {
                    payloadAtt.setPayload(null);
                }
            }
            return hasNext;
        }
    }
    public void testThreadSafety() throws Exception {
        rnd = newRandom();
        final int numThreads = 5;
        final int numDocs = 50;
        final ByteArrayPool pool = new ByteArrayPool(numThreads, 5);
        Directory dir = new RAMDirectory();
        final IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
            TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
        final String field = "test";
        Thread[] ingesters = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            ingesters[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < numDocs; j++) {
                            Document d = new Document();
                            d.add(new Field(field, new PoolingPayloadTokenStream(pool)));
                            writer.addDocument(d);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail(e.toString());
                    }
                }
            };
            ingesters[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
          ingesters[i].join();
        }
        writer.close();
        IndexReader reader = IndexReader.open(dir, true);
        TermEnum terms = reader.terms();
        while (terms.next()) {
            TermPositions tp = reader.termPositions(terms.term());
            while(tp.next()) {
                int freq = tp.freq();
                for (int i = 0; i < freq; i++) {
                    tp.nextPosition();
                    assertEquals(pool.bytesToString(tp.getPayload(new byte[5], 0)), terms.term().text);
                }
            }
            tp.close();
        }
        terms.close();
        reader.close();
        assertEquals(pool.size(), numThreads);
    }
    private class PoolingPayloadTokenStream extends TokenStream {
        private byte[] payload;
        private boolean first;
        private ByteArrayPool pool;
        private String term;
        TermAttribute termAtt;
        PayloadAttribute payloadAtt;
        PoolingPayloadTokenStream(ByteArrayPool pool) {
            this.pool = pool;
            payload = pool.get();
            generateRandomData(payload);
            term = pool.bytesToString(payload);
            first = true;
            payloadAtt = addAttribute(PayloadAttribute.class);
            termAtt = addAttribute(TermAttribute.class);
        }
        @Override
        public boolean incrementToken() throws IOException {
            if (!first) return false;
            first = false;
            clearAttributes();
            termAtt.setTermBuffer(term);
            payloadAtt.setPayload(new Payload(payload));
            return true;
        }
        @Override
        public void close() throws IOException {
            pool.release(payload);
        }
    }
    private static class ByteArrayPool {
        private List<byte[]> pool;
        ByteArrayPool(int capacity, int size) {
            pool = new ArrayList<byte[]>();
            for (int i = 0; i < capacity; i++) {
                pool.add(new byte[size]);
            }
        }
        private UnicodeUtil.UTF8Result utf8Result = new UnicodeUtil.UTF8Result();
        synchronized String bytesToString(byte[] bytes) {
            String s = new String(bytes);
            UnicodeUtil.UTF16toUTF8(s, 0, s.length(), utf8Result);
            try {
                return new String(utf8Result.result, 0, utf8Result.length, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                return null;
            }
        }
        synchronized byte[] get() {
            return pool.remove(0);
        }
        synchronized void release(byte[] b) {
            pool.add(b);
        }
        synchronized int size() {
            return pool.size();
        }
    }
}
