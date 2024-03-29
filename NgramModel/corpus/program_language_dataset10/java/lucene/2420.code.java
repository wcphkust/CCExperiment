package org.apache.solr.response;
import java.io.Writer;
import java.io.IOException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
public class PythonResponseWriter implements QueryResponseWriter {
  static String CONTENT_TYPE_PYTHON_ASCII="text/x-python;charset=US-ASCII";
  public void init(NamedList n) {
  }
  public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
    PythonWriter w = new PythonWriter(writer, req, rsp);
    try {
      w.writeResponse();
    } finally {
      w.close();
    }
  }
  public String getContentType(SolrQueryRequest request, SolrQueryResponse response) {
    return CONTENT_TYPE_TEXT_ASCII;
  }
}
class PythonWriter extends NaNFloatWriter {
  protected String getNaN() { return "float('NaN')"; }
  protected String getInf() { return "float('Inf')"; }
  public PythonWriter(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) {
    super(writer, req, rsp);
  }
  @Override
  public void writeNull(String name) throws IOException {
    writer.write("None");
  }
  @Override
  public void writeBool(String name, boolean val) throws IOException {
    writer.write(val ? "True" : "False");
  }
  @Override
  public void writeBool(String name, String val) throws IOException {
    writeBool(name,val.charAt(0)=='t');
  }
  @Override
  public void writeStr(String name, String val, boolean needsEscaping) throws IOException {
    if (!needsEscaping) {
      writer.write('\'');
      writer.write(val);
      writer.write('\'');
      return;
    }
    StringBuilder sb = new StringBuilder(val.length());
    boolean needUnicode=false;
    for (int i=0; i<val.length(); i++) {
      char ch = val.charAt(i);
      switch(ch) {
        case '\'':
        case '\\': sb.append('\\'); sb.append(ch); break;
        case '\r': sb.append("\\r"); break;
        case '\n': sb.append("\\n"); break;
        case '\t': sb.append("\\t"); break;
        default:
          if (ch<' ' || ch>127) {
            unicodeEscape(sb, ch);
            needUnicode=true;
          } else {
            sb.append(ch);
          }
      }
    }
    if (needUnicode) {
      writer.write('u');
    }
    writer.write('\'');
    writer.append(sb);
    writer.write('\'');
  }
}
