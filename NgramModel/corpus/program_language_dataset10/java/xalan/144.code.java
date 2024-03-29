package org.apache.xalan.serialize;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
public abstract class SerializerFactory
{
    private SerializerFactory()
    {
    }
    public static Serializer getSerializer(Properties format)
    {
        org.apache.xml.serializer.Serializer ser;
        ser = org.apache.xml.serializer.SerializerFactory.getSerializer(format);
        SerializerFactory.SerializerWrapper si = new SerializerWrapper(ser);
        return si;
    }
    private static class SerializerWrapper implements Serializer
    {
        private final org.apache.xml.serializer.Serializer m_serializer;
        private DOMSerializer m_old_DOMSerializer;
        SerializerWrapper(org.apache.xml.serializer.Serializer ser)
        {
            m_serializer = ser;
        }
        public void setOutputStream(OutputStream output)
        {
            m_serializer.setOutputStream(output);
        }
        public OutputStream getOutputStream()
        {
            return m_serializer.getOutputStream();
        }
        public void setWriter(Writer writer)
        {
            m_serializer.setWriter(writer);
        }
        public Writer getWriter()
        {
            return m_serializer.getWriter();
        }
        public void setOutputFormat(Properties format)
        {
            m_serializer.setOutputFormat(format);
        }
        public Properties getOutputFormat()
        {
            return m_serializer.getOutputFormat();
        }
        public ContentHandler asContentHandler() throws IOException
        {
            return m_serializer.asContentHandler();
        }
        public DOMSerializer asDOMSerializer() throws IOException
        {
            if (m_old_DOMSerializer == null)
            {
                m_old_DOMSerializer =
                    new DOMSerializerWrapper(m_serializer.asDOMSerializer());
            }
            return m_old_DOMSerializer;
        }
        public boolean reset()
        {
            return m_serializer.reset();
        }
    }
    private static class DOMSerializerWrapper implements DOMSerializer
    {
        private final org.apache.xml.serializer.DOMSerializer m_dom;
        DOMSerializerWrapper(org.apache.xml.serializer.DOMSerializer domser)
        {
            m_dom = domser;
        }
        public void serialize(Node node) throws IOException
        {
            m_dom.serialize(node);
        }
    }
}
