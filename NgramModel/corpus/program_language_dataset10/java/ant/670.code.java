package org.apache.tools.ant.types.resources;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
public class GZipResource extends CompressedResource {
    public GZipResource() {
    }
    public GZipResource(org.apache.tools.ant.types.ResourceCollection other) {
        super(other);
    }
    protected InputStream wrapStream(InputStream in) throws IOException {
        return new GZIPInputStream(in);
    }
     protected OutputStream wrapStream(OutputStream out) throws IOException {
        return new GZIPOutputStream(out);
    }
    protected String getCompressionName() {
        return "GZip";
    }
}