package org.apache.lucene.index;
import java.io.IOException;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
final class FreqProxTermsWriterPerField extends TermsHashConsumerPerField implements Comparable<FreqProxTermsWriterPerField> {
  final FreqProxTermsWriterPerThread perThread;
  final TermsHashPerField termsHashPerField;
  final FieldInfo fieldInfo;
  final DocumentsWriter.DocState docState;
  final FieldInvertState fieldState;
  boolean omitTermFreqAndPositions;
  PayloadAttribute payloadAttribute;
  public FreqProxTermsWriterPerField(TermsHashPerField termsHashPerField, FreqProxTermsWriterPerThread perThread, FieldInfo fieldInfo) {
    this.termsHashPerField = termsHashPerField;
    this.perThread = perThread;
    this.fieldInfo = fieldInfo;
    docState = termsHashPerField.docState;
    fieldState = termsHashPerField.fieldState;
    omitTermFreqAndPositions = fieldInfo.omitTermFreqAndPositions;
  }
  @Override
  int getStreamCount() {
    if (fieldInfo.omitTermFreqAndPositions)
      return 1;
    else
      return 2;
  }
  @Override
  void finish() {}
  boolean hasPayloads;
  @Override
  void skippingLongTerm() throws IOException {}
  public int compareTo(FreqProxTermsWriterPerField other) {
    return fieldInfo.name.compareTo(other.fieldInfo.name);
  }
  void reset() {
    omitTermFreqAndPositions = fieldInfo.omitTermFreqAndPositions;
    payloadAttribute = null;
  }
  @Override
  boolean start(Fieldable[] fields, int count) {
    for(int i=0;i<count;i++)
      if (fields[i].isIndexed())
        return true;
    return false;
  }     
  @Override
  void start(Fieldable f) {
    if (fieldState.attributeSource.hasAttribute(PayloadAttribute.class)) {
      payloadAttribute = fieldState.attributeSource.getAttribute(PayloadAttribute.class);
    } else {
      payloadAttribute = null;
    }
  }
  final void writeProx(FreqProxTermsWriter.PostingList p, int proxCode) {
    final Payload payload;
    if (payloadAttribute == null) {
      payload = null;
    } else {
      payload = payloadAttribute.getPayload();
    }
    if (payload != null && payload.length > 0) {
      termsHashPerField.writeVInt(1, (proxCode<<1)|1);
      termsHashPerField.writeVInt(1, payload.length);
      termsHashPerField.writeBytes(1, payload.data, payload.offset, payload.length);
      hasPayloads = true;      
    } else
      termsHashPerField.writeVInt(1, proxCode<<1);
    p.lastPosition = fieldState.position;
  }
  @Override
  final void newTerm(RawPostingList p0) {
    assert docState.testPoint("FreqProxTermsWriterPerField.newTerm start");
    FreqProxTermsWriter.PostingList p = (FreqProxTermsWriter.PostingList) p0;
    p.lastDocID = docState.docID;
    if (omitTermFreqAndPositions) {
      p.lastDocCode = docState.docID;
    } else {
      p.lastDocCode = docState.docID << 1;
      p.docFreq = 1;
      writeProx(p, fieldState.position);
    }
  }
  @Override
  final void addTerm(RawPostingList p0) {
    assert docState.testPoint("FreqProxTermsWriterPerField.addTerm start");
    FreqProxTermsWriter.PostingList p = (FreqProxTermsWriter.PostingList) p0;
    assert omitTermFreqAndPositions || p.docFreq > 0;
    if (omitTermFreqAndPositions) {
      if (docState.docID != p.lastDocID) {
        assert docState.docID > p.lastDocID;
        termsHashPerField.writeVInt(0, p.lastDocCode);
        p.lastDocCode = docState.docID - p.lastDocID;
        p.lastDocID = docState.docID;
      }
    } else {
      if (docState.docID != p.lastDocID) {
        assert docState.docID > p.lastDocID;
        if (1 == p.docFreq)
          termsHashPerField.writeVInt(0, p.lastDocCode|1);
        else {
          termsHashPerField.writeVInt(0, p.lastDocCode);
          termsHashPerField.writeVInt(0, p.docFreq);
        }
        p.docFreq = 1;
        p.lastDocCode = (docState.docID - p.lastDocID) << 1;
        p.lastDocID = docState.docID;
        writeProx(p, fieldState.position);
      } else {
        p.docFreq++;
        writeProx(p, fieldState.position-p.lastPosition);
      }
    }
  }
  public void abort() {}
}
