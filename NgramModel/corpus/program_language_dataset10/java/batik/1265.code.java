package org.apache.batik.svggen.font.table;
import java.io.IOException;
import java.io.RandomAccessFile;
public interface LookupSubtableFactory {
    LookupSubtable read(int type, RandomAccessFile raf, int offset)
    throws IOException;
}
