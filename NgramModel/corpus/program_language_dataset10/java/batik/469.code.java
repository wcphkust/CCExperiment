package org.apache.batik.dom;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
public class GenericElement extends AbstractElement {
    protected String nodeName;
    protected boolean readonly;
    protected GenericElement() {
    }
    public GenericElement(String name, AbstractDocument owner)
        throws DOMException {
        super(name, owner);
        nodeName = name;
    }
    public void setNodeName(String v) {
        nodeName = v;
    }
    public String getNodeName() {
        return nodeName;
    }
    public boolean isReadonly() {
        return readonly;
    }
    public void setReadonly(boolean v) {
        readonly = v;
    }
    protected Node export(Node n, AbstractDocument d) {
        super.export(n, d);
        GenericElement ge = (GenericElement)n;
        ge.nodeName = nodeName;
        return n;
    }
    protected Node deepExport(Node n, AbstractDocument d) {
        super.deepExport(n, d);
        GenericElement ge = (GenericElement)n;
        ge.nodeName = nodeName;
        return n;
    }
    protected Node copyInto(Node n) {
        GenericElement ge = (GenericElement)super.copyInto(n);
        ge.nodeName = nodeName;
        return n;
    }
    protected Node deepCopyInto(Node n) {
        GenericElement ge = (GenericElement)super.deepCopyInto(n);
        ge.nodeName = nodeName;
        return n;
    }
    protected Node newNode() {
        return new GenericElement();
    }
}
