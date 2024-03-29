package org.apache.batik.dom.svg12;
import org.apache.batik.dom.AbstractDocument;
import org.apache.batik.dom.svg.SVGStylableElement;
import org.apache.batik.util.SVG12Constants;
import org.w3c.dom.Node;
public class SVGOMMultiImageElement
    extends    SVGStylableElement {
    protected SVGOMMultiImageElement() {
    }
    public SVGOMMultiImageElement(String prefix, AbstractDocument owner) {
        super(prefix, owner);
    }
    public String getLocalName() {
        return SVG12Constants.SVG_MULTI_IMAGE_TAG;
    }
    protected Node newNode() {
        return new SVGOMMultiImageElement();
    }
}
