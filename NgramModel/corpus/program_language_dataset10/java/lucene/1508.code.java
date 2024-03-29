package org.apache.lucene.index;
import java.io.IOException;
abstract class DocConsumerPerThread {
  abstract DocumentsWriter.DocWriter processDocument() throws IOException;
  abstract void abort();
}
