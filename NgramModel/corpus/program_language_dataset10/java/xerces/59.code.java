package org.apache.html.dom;
import org.w3c.dom.html.HTMLButtonElement;
public class HTMLButtonElementImpl
    extends HTMLElementImpl
    implements HTMLButtonElement, HTMLFormControl
{
    private static final long serialVersionUID = -753685852948076730L;
    public String getAccessKey()
    {
        String    accessKey;
        accessKey = getAttribute( "accesskey" );
        if ( accessKey != null && accessKey.length() > 1 )
            accessKey = accessKey.substring( 0, 1 );
        return accessKey;
    }
    public void setAccessKey( String accessKey )
    {
        if ( accessKey != null && accessKey.length() > 1 )
            accessKey = accessKey.substring( 0, 1 );
        setAttribute( "accesskey", accessKey );
    }
    public boolean getDisabled()
    {
        return getBinary( "disabled" );
    }
    public void setDisabled( boolean disabled )
    {
        setAttribute( "disabled", disabled );
    }
    public String getName()
    {
        return getAttribute( "name" );
    }
    public void setName( String name )
    {
        setAttribute( "name", name );
    }
    public int getTabIndex()
    {
        try
        {
            return Integer.parseInt( getAttribute( "tabindex" ) );
        }
        catch ( NumberFormatException except )
        {
            return 0;
        }
    }
    public void setTabIndex( int tabIndex )
    {
        setAttribute( "tabindex", String.valueOf( tabIndex ) );
    }
    public String getType()
    {
        return capitalize( getAttribute( "type" ) );
    }
      public String getValue()
    {
        return getAttribute( "value" );
    }
    public void setValue( String value )
    {
        setAttribute( "value", value );
    }
    public HTMLButtonElementImpl( HTMLDocumentImpl owner, String name )
    {
        super( owner, name );
    }
}
