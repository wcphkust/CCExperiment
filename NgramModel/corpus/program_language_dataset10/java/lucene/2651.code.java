package org.apache.solr.client.solrj.request;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.nio.charset.Charset;
public class RequestWriter {
  public static final Charset UTF_8 = Charset.forName("UTF-8");
  public Collection<ContentStream> getContentStreams(SolrRequest req) throws IOException {
    if (req instanceof UpdateRequest) {
      UpdateRequest updateRequest = (UpdateRequest) req;
      if (isEmpty(updateRequest)) return null;
      List<ContentStream> l = new ArrayList<ContentStream>();
      l.add(new LazyContentStream(updateRequest));
      return l;
    }
    return req.getContentStreams();
  }
  private boolean isEmpty(UpdateRequest updateRequest) {
    return isNull(updateRequest.getDocuments()) &&
            isNull(updateRequest.getDeleteById()) &&
            isNull(updateRequest.getDeleteQuery()) &&
            updateRequest.getDocIterator() == null;
  }
  public String getPath(SolrRequest req) {
    return req.getPath();
  }
  public ContentStream getContentStream(UpdateRequest req) throws IOException {
    return new ContentStreamBase.StringStream(req.getXML());
  }
  public void write(SolrRequest request, OutputStream os) throws IOException {
    if (request instanceof UpdateRequest) {
      UpdateRequest updateRequest = (UpdateRequest) request;
      OutputStreamWriter writer = new OutputStreamWriter(os, UTF_8);
      updateRequest.writeXML(writer);
      writer.flush();
    }
  }
  public String getUpdateContentType() {
    return ClientUtils.TEXT_XML;
  }
  public class LazyContentStream implements ContentStream {
    ContentStream contentStream = null;
    UpdateRequest req = null;
    public LazyContentStream(UpdateRequest req) {
      this.req = req;
    }
    private ContentStream getDelegate() {
      if (contentStream == null) {
        try {
          contentStream = getContentStream(req);
        } catch (IOException e) {
          throw new RuntimeException("Unable to write xml into a stream", e);
        }
      }
      return contentStream;
    }
    public String getName() {
      return getDelegate().getName();
    }
    public String getSourceInfo() {
      return getDelegate().getSourceInfo();
    }
    public String getContentType() {
      return getUpdateContentType();
    }
    public Long getSize() {
      return getDelegate().getSize();
    }
    public InputStream getStream() throws IOException {
      return getDelegate().getStream();
    }
    public Reader getReader() throws IOException {
      return getDelegate().getReader();
    }
    public void writeTo(OutputStream os) throws IOException {
      write(req, os);
    }
  }
  protected boolean isNull(List l) {
    return l == null || l.isEmpty();
  }
}
