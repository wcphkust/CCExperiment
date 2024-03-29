package org.apache.wml.dom;
import org.apache.wml.WMLTdElement;
public class WMLTdElementImpl extends WMLElementImpl implements WMLTdElement {
    private static final long serialVersionUID = 6074218675876025710L;
    public WMLTdElementImpl (WMLDocumentImpl owner, String tagName) {
        super( owner, tagName);
    }
    public void setClassName(String newValue) {
        setAttribute("class", newValue);
    }
    public String getClassName() {
        return getAttribute("class");
    }
    public void setXmlLang(String newValue) {
        setAttribute("xml:lang", newValue);
    }
    public String getXmlLang() {
        return getAttribute("xml:lang");
    }
    public void setId(String newValue) {
        setAttribute("id", newValue);
    }
    public String getId() {
        return getAttribute("id");
    }
}
