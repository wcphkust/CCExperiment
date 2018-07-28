package org.apache.xml.serialize;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Locale;
import org.apache.xerces.dom.DOMMessageFormatter;
public final class HTMLdtd
{
    public static final String HTMLPublicId = "-//W3C//DTD HTML 4.01//EN";
    public static final String HTMLSystemId =
        "http://www.w3.org/TR/html4/strict.dtd";
    public static final String XHTMLPublicId =
        "-//W3C//DTD XHTML 1.0 Strict//EN";
    public static final String XHTMLSystemId =
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";
    private static Hashtable        _byChar;
    private static Hashtable        _byName;
    private static Hashtable        _boolAttrs;
    private static Hashtable        _elemDefs;
    private static final String     ENTITIES_RESOURCE = "HTMLEntities.res";
    private static final int ONLY_OPENING = 0x0001;
    private static final int ELEM_CONTENT = 0x0002;
    private static final int PRESERVE     = 0x0004;
    private static final int OPT_CLOSING  = 0x0008;
    private static final int EMPTY        = 0x0010 | ONLY_OPENING;
    private static final int ALLOWED_HEAD = 0x0020;
    private static final int CLOSE_P      = 0x0040;
    private static final int CLOSE_DD_DT  = 0x0080;
    private static final int CLOSE_SELF   = 0x0100;
    private static final int CLOSE_TABLE  = 0x0200;
    private static final int CLOSE_TH_TD  = 0x04000;
    public static boolean isEmptyTag( String tagName )
    {
        return isElement( tagName, EMPTY );
    }
    public static boolean isElementContent( String tagName )
    {
        return isElement( tagName, ELEM_CONTENT );
    }
    public static boolean isPreserveSpace( String tagName )
    {
        return isElement( tagName, PRESERVE );
    }
    public static boolean isOptionalClosing( String tagName )
    {
        return isElement( tagName, OPT_CLOSING );
    }
    public static boolean isOnlyOpening( String tagName )
    {
        return isElement( tagName, ONLY_OPENING );
    }
    public static boolean isClosing( String tagName, String openTag )
    {
        if ( openTag.equalsIgnoreCase( "HEAD" ) )
            return ! isElement( tagName, ALLOWED_HEAD );
        if ( openTag.equalsIgnoreCase( "P" ) )
            return isElement( tagName, CLOSE_P );
        if ( openTag.equalsIgnoreCase( "DT" ) || openTag.equalsIgnoreCase( "DD" ) )
            return isElement( tagName, CLOSE_DD_DT );
        if ( openTag.equalsIgnoreCase( "LI" ) || openTag.equalsIgnoreCase( "OPTION" ) )
            return isElement( tagName, CLOSE_SELF );
        if ( openTag.equalsIgnoreCase( "THEAD" ) || openTag.equalsIgnoreCase( "TFOOT" ) ||
             openTag.equalsIgnoreCase( "TBODY" ) || openTag.equalsIgnoreCase( "TR" ) ||
             openTag.equalsIgnoreCase( "COLGROUP" ) )
            return isElement( tagName, CLOSE_TABLE );
        if ( openTag.equalsIgnoreCase( "TH" ) || openTag.equalsIgnoreCase( "TD" ) )
            return isElement( tagName, CLOSE_TH_TD );
        return false;
    }
    public static boolean isURI( String tagName, String attrName )
    {
        return ( attrName.equalsIgnoreCase( "href" ) || attrName.equalsIgnoreCase( "src" ) );
    }
    public static boolean isBoolean( String tagName, String attrName )
    {
        String[] attrNames;
        attrNames = (String[]) _boolAttrs.get( tagName.toUpperCase(Locale.ENGLISH) );
        if ( attrNames == null )
            return false;
        for ( int i = 0 ; i < attrNames.length ; ++i )
            if ( attrNames[ i ].equalsIgnoreCase( attrName ) )
                return true;
        return false;
    }
    public static int charFromName( String name )
    {
        Object    value;
        initialize();
        value = _byName.get( name );
        if ( value != null && value instanceof Integer ) {
            return ( (Integer) value ).intValue();
        }
        return -1;
    }
    public static String fromChar(int value )
    {
       if (value > 0xffff)
            return null;
        String name;
        initialize();
        name = (String) _byChar.get( new Integer( value ) );
        return name;
    }
    private static void initialize()
    {
        InputStream     is = null;
        BufferedReader  reader = null;
        int             index;
        String          name;
        String          value;
        int             code;
        String          line;
        if ( _byName != null )
            return;
        try {
            _byName = new Hashtable();
            _byChar = new Hashtable();
            is = HTMLdtd.class.getResourceAsStream( ENTITIES_RESOURCE );
            if ( is == null ) {
            	throw new RuntimeException( 
				    DOMMessageFormatter.formatMessage(
				    DOMMessageFormatter.SERIALIZER_DOMAIN,
                    "ResourceNotFound", new Object[] {ENTITIES_RESOURCE}));
            }    
            reader = new BufferedReader( new InputStreamReader( is, "ASCII" ) );
            line = reader.readLine();
            while ( line != null ) {
                if ( line.length() == 0 || line.charAt( 0 ) == '#' ) {
                    line = reader.readLine();
                    continue;
                }
                index = line.indexOf( ' ' );
                if ( index > 1 ) {
                    name = line.substring( 0, index );
                    ++index;
                    if ( index < line.length() ) {
                        value = line.substring( index );
                        index = value.indexOf( ' ' );
                        if ( index > 0 )
                            value = value.substring( 0, index );
                        code = Integer.parseInt( value );
                                        defineEntity( name, (char) code );
                    }
                }
                line = reader.readLine();
            }
            is.close();
        }  catch ( Exception except ) {
			throw new RuntimeException( 
				DOMMessageFormatter.formatMessage(
				DOMMessageFormatter.SERIALIZER_DOMAIN,
                "ResourceNotLoaded", new Object[] {ENTITIES_RESOURCE, except.toString()}));        	
        } finally {
            if ( is != null ) {
                try {
                    is.close();
                } catch ( Exception except ) { }
            }
        }
    }
    private static void defineEntity( String name, char value )
    {
        if ( _byName.get( name ) == null ) {
            _byName.put( name, new Integer( value ) );
            _byChar.put( new Integer( value ), name );
        }
    }
    private static void defineElement( String name, int flags )
    {
        _elemDefs.put( name, new Integer( flags ) );
    }
    private static void defineBoolean( String tagName, String attrName )
    {
        defineBoolean( tagName, new String[] { attrName } );
    }
    private static void defineBoolean( String tagName, String[] attrNames )
    {
        _boolAttrs.put( tagName, attrNames );
    }
    private static boolean isElement( String name, int flag )
    {
        Integer flags;
        flags = (Integer) _elemDefs.get( name.toUpperCase(Locale.ENGLISH) );
        if ( flags == null ) {
            return false;
        }
        return ( ( flags.intValue() & flag ) == flag );
    }
    static
    {
        _elemDefs = new Hashtable();
        defineElement( "ADDRESS", CLOSE_P );
        defineElement( "AREA", EMPTY );
        defineElement( "BASE",  EMPTY | ALLOWED_HEAD );
        defineElement( "BASEFONT", EMPTY );
        defineElement( "BLOCKQUOTE", CLOSE_P );
        defineElement( "BODY", OPT_CLOSING );
        defineElement( "BR", EMPTY );
        defineElement( "COL", EMPTY );
        defineElement( "COLGROUP", ELEM_CONTENT | OPT_CLOSING | CLOSE_TABLE );
        defineElement( "DD", OPT_CLOSING | ONLY_OPENING | CLOSE_DD_DT );
        defineElement( "DIV", CLOSE_P );
        defineElement( "DL", ELEM_CONTENT | CLOSE_P );
        defineElement( "DT", OPT_CLOSING | ONLY_OPENING | CLOSE_DD_DT );
        defineElement( "FIELDSET", CLOSE_P );
        defineElement( "FORM", CLOSE_P );
        defineElement( "FRAME", EMPTY | OPT_CLOSING );
        defineElement( "H1", CLOSE_P );
        defineElement( "H2", CLOSE_P );
        defineElement( "H3", CLOSE_P );
        defineElement( "H4", CLOSE_P );
        defineElement( "H5", CLOSE_P );
        defineElement( "H6", CLOSE_P );
        defineElement( "HEAD", ELEM_CONTENT | OPT_CLOSING );
        defineElement( "HR", EMPTY | CLOSE_P );
        defineElement( "HTML", ELEM_CONTENT | OPT_CLOSING );
        defineElement( "IMG", EMPTY );
        defineElement( "INPUT", EMPTY );
        defineElement( "ISINDEX", EMPTY | ALLOWED_HEAD );
        defineElement( "LI", OPT_CLOSING | ONLY_OPENING | CLOSE_SELF );
        defineElement( "LINK", EMPTY | ALLOWED_HEAD );
        defineElement( "MAP", ALLOWED_HEAD );
        defineElement( "META", EMPTY | ALLOWED_HEAD );
        defineElement( "OL", ELEM_CONTENT | CLOSE_P );
        defineElement( "OPTGROUP", ELEM_CONTENT );
        defineElement( "OPTION", OPT_CLOSING | ONLY_OPENING | CLOSE_SELF );
        defineElement( "P", OPT_CLOSING | CLOSE_P | CLOSE_SELF );
        defineElement( "PARAM", EMPTY );
        defineElement( "PRE", PRESERVE | CLOSE_P );
        defineElement( "SCRIPT", ALLOWED_HEAD | PRESERVE );
        defineElement( "NOSCRIPT", ALLOWED_HEAD | PRESERVE );
        defineElement( "SELECT", ELEM_CONTENT );
        defineElement( "STYLE", ALLOWED_HEAD | PRESERVE );
        defineElement( "TABLE", ELEM_CONTENT | CLOSE_P );
        defineElement( "TBODY", ELEM_CONTENT | OPT_CLOSING | CLOSE_TABLE );
        defineElement( "TD", OPT_CLOSING | CLOSE_TH_TD );
        defineElement( "TEXTAREA", PRESERVE );
        defineElement( "TFOOT", ELEM_CONTENT | OPT_CLOSING | CLOSE_TABLE );
        defineElement( "TH", OPT_CLOSING | CLOSE_TH_TD );
        defineElement( "THEAD", ELEM_CONTENT | OPT_CLOSING | CLOSE_TABLE );
        defineElement( "TITLE", ALLOWED_HEAD );
        defineElement( "TR", ELEM_CONTENT | OPT_CLOSING | CLOSE_TABLE );
        defineElement( "UL", ELEM_CONTENT | CLOSE_P );
        _boolAttrs = new Hashtable();
        defineBoolean( "AREA", "href" );
        defineBoolean( "BUTTON", "disabled" );
        defineBoolean( "DIR", "compact" );
        defineBoolean( "DL", "compact" );
        defineBoolean( "FRAME", "noresize" );
        defineBoolean( "HR", "noshade" );
        defineBoolean( "IMAGE", "ismap" );
        defineBoolean( "INPUT", new String[] { "defaultchecked", "checked", "readonly", "disabled" } );
        defineBoolean( "LINK", "link" );
        defineBoolean( "MENU", "compact" );
        defineBoolean( "OBJECT", "declare" );
        defineBoolean( "OL", "compact" );
        defineBoolean( "OPTGROUP", "disabled" );
        defineBoolean( "OPTION", new String[] { "default-selected", "selected", "disabled" } );
        defineBoolean( "SCRIPT", "defer" );
        defineBoolean( "SELECT", new String[] { "multiple", "disabled" } );
        defineBoolean( "STYLE", "disabled" );
        defineBoolean( "TD", "nowrap" );
        defineBoolean( "TH", "nowrap" );
        defineBoolean( "TEXTAREA", new String[] { "disabled", "readonly" } );
        defineBoolean( "UL", "compact" );
        initialize();
    }
}