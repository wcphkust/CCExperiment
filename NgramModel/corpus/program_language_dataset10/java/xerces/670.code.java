package org.apache.xerces.xni.parser;
import org.apache.xerces.xni.XNIException;
public interface XMLComponent {
    public void reset(XMLComponentManager componentManager) 
        throws XMLConfigurationException;
    public String[] getRecognizedFeatures();
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException;
    public String[] getRecognizedProperties();
    public void setProperty(String propertyId, Object value)
       throws XMLConfigurationException;
    public Boolean getFeatureDefault(String featureId);
    public Object getPropertyDefault(String propertyId);
} 
