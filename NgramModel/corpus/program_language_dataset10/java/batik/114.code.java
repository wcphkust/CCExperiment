package org.apache.batik.bridge;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.batik.dom.AbstractNode;
import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.ext.awt.MultipleGradientPaint;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.ParsedURL;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
public abstract class AbstractSVGGradientElementBridge
        extends AnimatableGenericSVGBridge
        implements PaintBridge, ErrorConstants {
    protected AbstractSVGGradientElementBridge() {}
    public Paint createPaint(BridgeContext ctx,
                             Element paintElement,
                             Element paintedElement,
                             GraphicsNode paintedNode,
                             float opacity) {
        String s;
        List stops = extractStop(paintElement, opacity, ctx);
        if (stops == null) {
            return null;
        }
        int stopLength = stops.size();
        if (stopLength == 1) {
            return ((Stop)stops.get(0)).color;
        }
        float [] offsets = new float[stopLength];
        Color [] colors = new Color[stopLength];
        Iterator iter = stops.iterator();
        for (int i=0; iter.hasNext(); ++i) {
            Stop stop = (Stop)iter.next();
            offsets[i] = stop.offset;
            colors[i] = stop.color;
        }
        MultipleGradientPaint.CycleMethodEnum spreadMethod
            = MultipleGradientPaint.NO_CYCLE;
        s = SVGUtilities.getChainableAttributeNS
            (paintElement, null, SVG_SPREAD_METHOD_ATTRIBUTE, ctx);
        if (s.length() != 0) {
            spreadMethod = convertSpreadMethod(paintElement, s, ctx);
        }
        MultipleGradientPaint.ColorSpaceEnum colorSpace
            = CSSUtilities.convertColorInterpolation(paintElement);
        AffineTransform transform;
        s = SVGUtilities.getChainableAttributeNS
            (paintElement, null, SVG_GRADIENT_TRANSFORM_ATTRIBUTE, ctx);
        if (s.length() != 0) {
            transform = SVGUtilities.convertTransform
                (paintElement, SVG_GRADIENT_TRANSFORM_ATTRIBUTE, s, ctx);
        } else {
            transform = new AffineTransform();
        }
        Paint paint = buildGradient(paintElement,
                                    paintedElement,
                                    paintedNode,
                                    spreadMethod,
                                    colorSpace,
                                    transform,
                                    colors,
                                    offsets,
                                    ctx);
        return paint;
    }
    protected abstract
        Paint buildGradient(Element paintElement,
                            Element paintedElement,
                            GraphicsNode paintedNode,
                            MultipleGradientPaint.CycleMethodEnum spreadMethod,
                            MultipleGradientPaint.ColorSpaceEnum colorSpace,
                            AffineTransform transform,
                            Color [] colors,
                            float [] offsets,
                            BridgeContext ctx);
    protected static MultipleGradientPaint.CycleMethodEnum convertSpreadMethod
        (Element paintElement, String s, BridgeContext ctx) {
        if (SVG_REPEAT_VALUE.equals(s)) {
            return MultipleGradientPaint.REPEAT;
        }
        if (SVG_REFLECT_VALUE.equals(s)) {
            return MultipleGradientPaint.REFLECT;
        }
        if (SVG_PAD_VALUE.equals(s)) {
            return MultipleGradientPaint.NO_CYCLE;
        }
        throw new BridgeException
            (ctx, paintElement, ERR_ATTRIBUTE_VALUE_MALFORMED,
             new Object[] {SVG_SPREAD_METHOD_ATTRIBUTE, s});
    }
    protected static List extractStop(Element paintElement,
                                      float opacity,
                                      BridgeContext ctx) {
        List refs = new LinkedList();
        for (;;) {
            List stops = extractLocalStop(paintElement, opacity, ctx);
            if (stops != null) {
                return stops; 
            }
            String uri = XLinkSupport.getXLinkHref(paintElement);
            if (uri.length() == 0) {
                return null; 
            }
            String baseURI = ((AbstractNode) paintElement).getBaseURI();
            ParsedURL purl = new ParsedURL(baseURI, uri);
            if (contains(refs, purl)) {
                throw new BridgeException(ctx, paintElement,
                                          ERR_XLINK_HREF_CIRCULAR_DEPENDENCIES,
                                          new Object[] {uri});
            }
            refs.add(purl);
            paintElement = ctx.getReferencedElement(paintElement, uri);
        }
    }
    protected static List extractLocalStop(Element gradientElement,
                                           float opacity,
                                           BridgeContext ctx) {
        LinkedList stops = null;
        Stop previous = null;
        for (Node n = gradientElement.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            if ((n.getNodeType() != Node.ELEMENT_NODE)) {
                continue;
            }
            Element e = (Element)n;
            Bridge bridge = ctx.getBridge(e);
            if (bridge == null || !(bridge instanceof SVGStopElementBridge)) {
                continue;
            }
            Stop stop = ((SVGStopElementBridge)bridge).createStop
                (ctx, gradientElement, e, opacity);
            if (stops == null) {
                stops = new LinkedList();
            }
            if (previous != null) {
                if (stop.offset < previous.offset) {
                    stop.offset = previous.offset;
                }
            }
            stops.add(stop);
            previous = stop;
        }
        return stops;
    }
    private static boolean contains(List urls, ParsedURL key) {
        Iterator iter = urls.iterator();
        while (iter.hasNext()) {
            if (key.equals(iter.next()))
                return true;
        }
        return false;
    }
    public static class Stop {
        public Color color;
        public float offset;
        public Stop(Color color, float offset) {
            this.color = color;
            this.offset = offset;
        }
    }
    public static class SVGStopElementBridge extends AnimatableGenericSVGBridge
            implements Bridge {
        public String getLocalName() {
            return SVG_STOP_TAG;
        }
        public Stop createStop(BridgeContext ctx,
                               Element gradientElement,
                               Element stopElement,
                               float opacity) {
            String s = stopElement.getAttributeNS(null, SVG_OFFSET_ATTRIBUTE);
            if (s.length() == 0) {
                throw new BridgeException
                    (ctx, stopElement, ERR_ATTRIBUTE_MISSING,
                     new Object[] {SVG_OFFSET_ATTRIBUTE});
            }
            float offset;
            try {
                offset = SVGUtilities.convertRatio(s);
            } catch (NumberFormatException nfEx ) {
                throw new BridgeException
                    (ctx, stopElement, nfEx, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object[] {SVG_OFFSET_ATTRIBUTE, s, nfEx });
            }
            Color color
                = CSSUtilities.convertStopColor(stopElement, opacity, ctx);
            return new Stop(color, offset);
        }
    }
}
