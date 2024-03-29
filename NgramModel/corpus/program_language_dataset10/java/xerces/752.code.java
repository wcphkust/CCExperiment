package org.apache.xml.serialize;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Hashtable;
import java.util.StringTokenizer;
public abstract class SerializerFactory {
    public static final String FactoriesProperty = "org.apache.xml.serialize.factories";
    private static Hashtable  _factories = new Hashtable();
    static
    {
        SerializerFactory factory;
        String            list;
        StringTokenizer   token;
        String            className;
        factory =  new SerializerFactoryImpl( Method.XML );
        registerSerializerFactory( factory );
        factory =  new SerializerFactoryImpl( Method.HTML );
        registerSerializerFactory( factory );
        factory =  new SerializerFactoryImpl( Method.XHTML );
        registerSerializerFactory( factory );
        factory =  new SerializerFactoryImpl( Method.TEXT );
        registerSerializerFactory( factory );
        list = SecuritySupport.getSystemProperty( FactoriesProperty );
        if ( list != null ) {
            token = new StringTokenizer( list, " ;,:" );
            while ( token.hasMoreTokens() ) {
                className = token.nextToken();
                try {
                    factory = (SerializerFactory) ObjectFactory.newInstance( className,
                        SerializerFactory.class.getClassLoader(), true);
                    if ( _factories.containsKey( factory.getSupportedMethod() ) )
                        _factories.put( factory.getSupportedMethod(), factory );
                } catch ( Exception except ) { }
            }
        }
    }
    public static void registerSerializerFactory( SerializerFactory factory )
    {
        String method;
        synchronized ( _factories ) {
            method = factory.getSupportedMethod();
            _factories.put( method, factory );
        }
    }
    public static SerializerFactory getSerializerFactory( String method )
    {
        return (SerializerFactory) _factories.get( method );
    }
    protected abstract String getSupportedMethod();
    public abstract Serializer makeSerializer(OutputFormat format);
    public abstract Serializer makeSerializer( Writer writer,
                                               OutputFormat format );
    public abstract Serializer makeSerializer( OutputStream output,
                                               OutputFormat format )
        throws UnsupportedEncodingException;
}
