package org.apache.batik.dom.svg;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.apache.batik.css.engine.SVGCSSEngine;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGException;
import org.w3c.dom.svg.SVGFitToViewBox;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGRect;
public class SVGLocatableSupport {
    public SVGLocatableSupport() {
    }
    public static SVGElement getNearestViewportElement(Element e) {
        Element elt = e;
        while (elt != null) {
            elt = SVGCSSEngine.getParentCSSStylableElement(elt);
            if (elt instanceof SVGFitToViewBox) {
                break;
            }
        }
        return (SVGElement)elt;
    }
    public static SVGElement getFarthestViewportElement(Element elt) {
        Element rootSVG = elt.getOwnerDocument().getDocumentElement();
        if (elt == rootSVG) {
            return null;
        }
        return (SVGElement) rootSVG;
    }
    public static SVGRect getBBox(Element elt) {
        final SVGOMElement svgelt = (SVGOMElement)elt;
        SVGContext svgctx = svgelt.getSVGContext();
        if (svgctx == null) return null;
        if (svgctx.getBBox() == null) return null;
        return new SVGRect() {
                public float getX() {
                    return (float)svgelt.getSVGContext().getBBox().getX();
                }
                public void setX(float x) throws DOMException {
                    throw svgelt.createDOMException
                        (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                         "readonly.rect", null);
                }
                public float getY() {
                    return (float)svgelt.getSVGContext().getBBox().getY();
                }
                public void setY(float y) throws DOMException {
                    throw svgelt.createDOMException
                        (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                         "readonly.rect", null);
                }
                public float getWidth() {
                    return (float)svgelt.getSVGContext().getBBox().getWidth();
                }
                public void setWidth(float width) throws DOMException {
                    throw svgelt.createDOMException
                        (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                         "readonly.rect", null);
                }
                public float getHeight() {
                    return (float)svgelt.getSVGContext().getBBox().getHeight();
                }
                public void setHeight(float height) throws DOMException {
                    throw svgelt.createDOMException
                        (DOMException.NO_MODIFICATION_ALLOWED_ERR,
                         "readonly.rect", null);
                }
            };
    }
    public static SVGMatrix getCTM(Element elt) {
        final SVGOMElement svgelt = (SVGOMElement)elt;
        return new AbstractSVGMatrix() {
                protected AffineTransform getAffineTransform() {
                    return svgelt.getSVGContext().getCTM();
            }
        };
    }
    public static SVGMatrix getScreenCTM(Element elt) {
        final SVGOMElement svgelt  = (SVGOMElement)elt;
        return new AbstractSVGMatrix() {
                protected AffineTransform getAffineTransform() {
                    SVGContext context = svgelt.getSVGContext();
                    AffineTransform ret = context.getGlobalTransform();
                    AffineTransform scrnTrans = context.getScreenTransform();
                    if (scrnTrans != null)
                        ret.preConcatenate(scrnTrans);
                    return ret;
                }
            };
    }
    public static SVGMatrix getTransformToElement(Element elt,
                                                  SVGElement element)
        throws SVGException {
        final SVGOMElement currentElt = (SVGOMElement)elt;
        final SVGOMElement targetElt = (SVGOMElement)element;
        return new AbstractSVGMatrix() {
                protected AffineTransform getAffineTransform() {
                    AffineTransform cat = 
                        currentElt.getSVGContext().getGlobalTransform();
                    if (cat == null) {
                        cat = new AffineTransform();
                    }
                    AffineTransform tat = 
                        targetElt.getSVGContext().getGlobalTransform();
                    if (tat == null) {
                        tat = new AffineTransform();
                    }
                    AffineTransform at = new AffineTransform(cat);
                    try {
                        at.preConcatenate(tat.createInverse());
                        return at;
                    } catch (NoninvertibleTransformException ex) {
                        throw currentElt.createSVGException
                            (SVGException.SVG_MATRIX_NOT_INVERTABLE,
                             "noninvertiblematrix",
                             null);
                    }
                }
            };
    }
}
