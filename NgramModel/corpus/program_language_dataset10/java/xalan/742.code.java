package org.apache.xml.utils;
public class SerializableLocatorImpl
implements org.xml.sax.Locator, java.io.Serializable
{
    static final long serialVersionUID = -2660312888446371460L;
    public SerializableLocatorImpl ()
    {
    }
    public SerializableLocatorImpl (org.xml.sax.Locator locator)
    {
        setPublicId(locator.getPublicId());
        setSystemId(locator.getSystemId());
        setLineNumber(locator.getLineNumber());
        setColumnNumber(locator.getColumnNumber());
    }
    public String getPublicId ()
    {
        return publicId;
    }
    public String getSystemId ()
    {
        return systemId;
    }
    public int getLineNumber ()
    {
        return lineNumber;
    }
    public int getColumnNumber ()
    {
        return columnNumber;
    }
    public void setPublicId (String publicId)
    {
        this.publicId = publicId;
    }
    public void setSystemId (String systemId)
    {
        this.systemId = systemId;
    }
    public void setLineNumber (int lineNumber)
    {
        this.lineNumber = lineNumber;
    }
    public void setColumnNumber (int columnNumber)
    {
        this.columnNumber = columnNumber;
    }
    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;
}
