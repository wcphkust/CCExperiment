package org.apache.lucene.index;
import java.io.IOException;
import org.apache.lucene.store.RAMOutputStream;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
final class StoredFieldsWriter {
  FieldsWriter fieldsWriter;
  final DocumentsWriter docWriter;
  final FieldInfos fieldInfos;
  int lastDocID;
  PerDoc[] docFreeList = new PerDoc[1];
  int freeCount;
  public StoredFieldsWriter(DocumentsWriter docWriter, FieldInfos fieldInfos) {
    this.docWriter = docWriter;
    this.fieldInfos = fieldInfos;
  }
  public StoredFieldsWriterPerThread addThread(DocumentsWriter.DocState docState) throws IOException {
    return new StoredFieldsWriterPerThread(docState, this);
  }
  synchronized public void flush(SegmentWriteState state) throws IOException {
    if (state.numDocsInStore > 0) {
      initFieldsWriter();
      fill(state.numDocsInStore - docWriter.getDocStoreOffset());
    }
    if (fieldsWriter != null)
      fieldsWriter.flush();
  }
  private void initFieldsWriter() throws IOException {
    if (fieldsWriter == null) {
      final String docStoreSegment = docWriter.getDocStoreSegment();
      if (docStoreSegment != null) {
        fieldsWriter = new FieldsWriter(docWriter.directory,
                                        docStoreSegment,
                                        fieldInfos);
        docWriter.addOpenFile(IndexFileNames.segmentFileName(docStoreSegment, IndexFileNames.FIELDS_EXTENSION));
        docWriter.addOpenFile(IndexFileNames.segmentFileName(docStoreSegment, IndexFileNames.FIELDS_INDEX_EXTENSION));
        lastDocID = 0;
      }
    }
  }
  synchronized public void closeDocStore(SegmentWriteState state) throws IOException {
    final int inc = state.numDocsInStore - lastDocID;
    if (inc > 0) {
      initFieldsWriter();
      fill(state.numDocsInStore - docWriter.getDocStoreOffset());
    }
    if (fieldsWriter != null) {
      fieldsWriter.close();
      fieldsWriter = null;
      lastDocID = 0;
      assert state.docStoreSegmentName != null;
      String fieldsName = IndexFileNames.segmentFileName(state.docStoreSegmentName, IndexFileNames.FIELDS_EXTENSION);
      String fieldsIdxName = IndexFileNames.segmentFileName(state.docStoreSegmentName, IndexFileNames.FIELDS_INDEX_EXTENSION);
      state.flushedFiles.add(fieldsName);
      state.flushedFiles.add(fieldsIdxName);
      state.docWriter.removeOpenFile(fieldsName);
      state.docWriter.removeOpenFile(fieldsIdxName);
      if (4+((long) state.numDocsInStore)*8 != state.directory.fileLength(fieldsIdxName))
        throw new RuntimeException("after flush: fdx size mismatch: " + state.numDocsInStore + " docs vs " + state.directory.fileLength(fieldsIdxName) + " length in bytes of " + fieldsIdxName + " file exists?=" + state.directory.fileExists(fieldsIdxName));
    }
  }
  int allocCount;
  synchronized PerDoc getPerDoc() {
    if (freeCount == 0) {
      allocCount++;
      if (allocCount > docFreeList.length) {
        assert allocCount == 1+docFreeList.length;
        docFreeList = new PerDoc[ArrayUtil.oversize(allocCount, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
      }
      return new PerDoc();
    } else
      return docFreeList[--freeCount];
  }
  synchronized void abort() {
    if (fieldsWriter != null) {
      try {
        fieldsWriter.close();
      } catch (Throwable t) {
      }
      fieldsWriter = null;
      lastDocID = 0;
    }
  }
  void fill(int docID) throws IOException {
    final int docStoreOffset = docWriter.getDocStoreOffset();
    final int end = docID+docStoreOffset;
    while(lastDocID < end) {
      fieldsWriter.skipDocument();
      lastDocID++;
    }
  }
  synchronized void finishDocument(PerDoc perDoc) throws IOException {
    assert docWriter.writer.testPoint("StoredFieldsWriter.finishDocument start");
    initFieldsWriter();
    fill(perDoc.docID);
    fieldsWriter.flushDocument(perDoc.numStoredFields, perDoc.fdt);
    lastDocID++;
    perDoc.reset();
    free(perDoc);
    assert docWriter.writer.testPoint("StoredFieldsWriter.finishDocument end");
  }
  public boolean freeRAM() {
    return false;
  }
  synchronized void free(PerDoc perDoc) {
    assert freeCount < docFreeList.length;
    assert 0 == perDoc.numStoredFields;
    assert 0 == perDoc.fdt.length();
    assert 0 == perDoc.fdt.getFilePointer();
    docFreeList[freeCount++] = perDoc;
  }
  class PerDoc extends DocumentsWriter.DocWriter {
    final DocumentsWriter.PerDocBuffer buffer = docWriter.newPerDocBuffer();
    RAMOutputStream fdt = new RAMOutputStream(buffer);
    int numStoredFields;
    void reset() {
      fdt.reset();
      buffer.recycle();
      numStoredFields = 0;
    }
    @Override
    void abort() {
      reset();
      free(this);
    }
    @Override
    public long sizeInBytes() {
      return buffer.getSizeInBytes();
    }
    @Override
    public void finish() throws IOException {
      finishDocument(this);
    }
  }
}
