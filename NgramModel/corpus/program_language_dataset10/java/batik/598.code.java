package org.apache.batik.dom.svg;
import org.apache.batik.dom.AbstractDocument;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGFontFaceNameElement;
public class SVGOMFontFaceNameElement
    extends    SVGOMElement
    implements SVGFontFaceNameElement {
    protected SVGOMFontFaceNameElement() {
    }
    public SVGOMFontFaceNameElement(String prefix, AbstractDocument owner) {
        super(prefix, owner);
    }
    public String getLocalName() {
        return SVG_FONT_FACE_NAME_TAG;
    }
    protected Node newNode() {
        return new SVGOMFontFaceNameElement();
    }
}
