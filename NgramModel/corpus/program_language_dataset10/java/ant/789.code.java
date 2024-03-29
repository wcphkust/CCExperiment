package org.apache.tools.ant.util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
public abstract class LineOrientedOutputStream extends OutputStream {
    private static final int INTIAL_SIZE = 132;
    private static final int CR = 0x0d;
    private static final int LF = 0x0a;
    private ByteArrayOutputStream buffer
        = new ByteArrayOutputStream(INTIAL_SIZE);
    private boolean skip = false;
    public final void write(int cc) throws IOException {
        final byte c = (byte) cc;
        if ((c == LF) || (c == CR)) {
            if (!skip) {
              processBuffer();
            }
        } else {
            buffer.write(cc);
        }
        skip = (c == CR);
    }
    public final void flush() throws IOException {
        if (buffer.size() > 0) {
            processBuffer();
        }
    }
    protected void processBuffer() throws IOException {
        try {
            processLine(buffer.toString());
        } finally {
            buffer.reset();
        }
    }
    protected abstract void processLine(String line) throws IOException;
    public final void close() throws IOException {
        if (buffer.size() > 0) {
            processBuffer();
        }
        super.close();
    }
    public final void write(byte[] b, int off, int len) throws IOException {
        int offset = off;
        int blockStartOffset = offset;
        int remaining = len;
        while (remaining > 0) {
            while (remaining > 0 && b[offset] != LF && b[offset] != CR) {
                offset++;
                remaining--;
            }
            int blockLength = offset - blockStartOffset;
            if (blockLength > 0) {
                buffer.write(b, blockStartOffset, blockLength);
            }
            while (remaining > 0 && (b[offset] == LF || b[offset] == CR)) {
                write(b[offset]);
                offset++;
                remaining--;
            }
            blockStartOffset = offset;
        }
    }
}
