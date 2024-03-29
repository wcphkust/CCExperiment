package org.apache.solr.handler.dataimport;
import static org.apache.solr.handler.dataimport.DataImportHandlerException.SEVERE;
import static org.apache.solr.handler.dataimport.DataImportHandlerException.wrapAndThrow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Properties;
public class FieldReaderDataSource extends DataSource<Reader> {
  private static final Logger LOG = LoggerFactory.getLogger(FieldReaderDataSource.class);
  protected VariableResolver vr;
  protected String dataField;
  private String encoding;
  private EntityProcessorWrapper entityProcessor;
  public void init(Context context, Properties initProps) {
    dataField = context.getEntityAttribute("dataField");
    encoding = context.getEntityAttribute("encoding");
    entityProcessor = (EntityProcessorWrapper) context.getEntityProcessor();
  }
  public Reader getData(String query) {
    Object o = entityProcessor.getVariableResolver().resolve(dataField);
    if (o == null) {
       throw new DataImportHandlerException (SEVERE, "No field available for name : " +dataField);
    }
    if (o instanceof String) {
      return new StringReader((String) o);
    } else if (o instanceof Clob) {
      Clob clob = (Clob) o;
      try {
        return readCharStream(clob);
      } catch (Exception e) {
        LOG.info("Unable to get data from CLOB");
        return null;
      }
    } else if (o instanceof Blob) {
      Blob blob = (Blob) o;
      try {
        Method m = blob.getClass().getDeclaredMethod("getBinaryStream");
        if (Modifier.isPublic(m.getModifiers())) {
          return getReader(m, blob);
        } else {
          m.setAccessible(true);
          return getReader(m, blob);
        }
      } catch (Exception e) {
        LOG.info("Unable to get data from BLOB");
        return null;
      }
    } else {
      return new StringReader(o.toString());
    }
  }
  static Reader readCharStream(Clob clob) {
    try {
      Method m = clob.getClass().getDeclaredMethod("getCharacterStream");
      if (Modifier.isPublic(m.getModifiers())) {
        return (Reader) m.invoke(clob);
      } else {
        m.setAccessible(true);
        return (Reader) m.invoke(clob);
      }
    } catch (Exception e) {
      wrapAndThrow(SEVERE, e,"Unable to get reader from clob");
      return null;
    }
  }
  private Reader getReader(Method m, Blob blob)
          throws IllegalAccessException, InvocationTargetException, UnsupportedEncodingException {
    InputStream is = (InputStream) m.invoke(blob);
    if (encoding == null) {
      return (new InputStreamReader(is));
    } else {
      return (new InputStreamReader(is, encoding));
    }
  }
  public void close() {
  }
}
