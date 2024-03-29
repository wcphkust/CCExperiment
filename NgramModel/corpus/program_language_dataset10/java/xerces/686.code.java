package org.apache.xerces.xni.parser;
import java.io.IOException;
import org.apache.xerces.xni.XNIException;
public interface XMLPullParserConfiguration
    extends XMLParserConfiguration {
    public void setInputSource(XMLInputSource inputSource)
        throws XMLConfigurationException, IOException;
    public boolean parse(boolean complete) throws XNIException, IOException;
    public void cleanup();
} 
