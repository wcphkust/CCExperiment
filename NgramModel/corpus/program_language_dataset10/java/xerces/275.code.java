package org.apache.xerces.dom3.as;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public interface ElementEditAS extends NodeEditAS {
    public NodeList getDefinedElementTypes();
    public short contentType();
    public boolean canSetAttribute(String attrname, 
                                   String attrval);
    public boolean canSetAttributeNode(Attr attrNode);
    public boolean canSetAttributeNS(String name, 
                                     String attrval, 
                                     String namespaceURI);
    public boolean canRemoveAttribute(String attrname);
    public boolean canRemoveAttributeNS(String attrname, 
                                        String namespaceURI);
    public boolean canRemoveAttributeNode(Node attrNode);
    public NodeList getChildElements();
    public NodeList getParentElements();
    public NodeList getAttributeList();
    public boolean isElementDefined(String elemTypeName);
    public boolean isElementDefinedNS(String elemTypeName, 
                                      String namespaceURI, 
                                      String name);
}
