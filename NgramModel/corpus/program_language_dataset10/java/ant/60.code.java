package org.apache.tools.ant;
public interface DynamicAttributeNS {
    void setDynamicAttribute(
        String uri, String localName, String qName, String value)
            throws BuildException;
}