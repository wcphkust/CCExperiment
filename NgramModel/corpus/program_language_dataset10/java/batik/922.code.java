package org.apache.batik.extension.svg;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import org.apache.batik.bridge.AbstractSVGFilterPrimitiveElementBridge;
import org.apache.batik.bridge.Bridge;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.BridgeException;
import org.apache.batik.bridge.SVGUtilities;
import org.apache.batik.ext.awt.image.PadMode;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.PadRable8Bit;
import org.apache.batik.gvt.GraphicsNode;
import org.w3c.dom.Element;
public class BatikHistogramNormalizationElementBridge
    extends AbstractSVGFilterPrimitiveElementBridge
    implements BatikExtConstants {
    public BatikHistogramNormalizationElementBridge() {  }
    public String getNamespaceURI() {
        return BATIK_EXT_NAMESPACE_URI;
    }
    public String getLocalName() {
        return BATIK_EXT_HISTOGRAM_NORMALIZATION_TAG;
    }
    public Bridge getInstance() {
        return new BatikHistogramNormalizationElementBridge();
    }
    public Filter createFilter(BridgeContext ctx,
                               Element filterElement,
                               Element filteredElement,
                               GraphicsNode filteredNode,
                               Filter inputFilter,
                               Rectangle2D filterRegion,
                               Map filterMap) {
        Filter in = getIn(filterElement,
                          filteredElement,
                          filteredNode,
                          inputFilter,
                          filterMap,
                          ctx);
        if (in == null) {
            return null; 
        }
        Filter sourceGraphics = (Filter)filterMap.get(SVG_SOURCE_GRAPHIC_VALUE);
        Rectangle2D defaultRegion;
        if (in == sourceGraphics) {
            defaultRegion = filterRegion;
        } else {
            defaultRegion = in.getBounds2D();
        }
        Rectangle2D primitiveRegion
            = SVGUtilities.convertFilterPrimitiveRegion(filterElement,
                                                        filteredElement,
                                                        filteredNode,
                                                        defaultRegion,
                                                        filterRegion,
                                                        ctx);
        float trim = 1;
        String s = filterElement.getAttributeNS
            (null, BATIK_EXT_TRIM_ATTRIBUTE);
        if (s.length() != 0) {
            try {
                trim = SVGUtilities.convertSVGNumber(s);
            } catch (NumberFormatException nfEx ) {
                throw new BridgeException
                    (ctx, filterElement, nfEx, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object[] {BATIK_EXT_TRIM_ATTRIBUTE, s});
            }
        }
        if (trim < 0) trim =0;
        else if (trim > 100) trim=100;
        Filter filter = in;
        filter = new BatikHistogramNormalizationFilter8Bit(filter, trim/100);
        filter = new PadRable8Bit(filter, primitiveRegion, PadMode.ZERO_PAD);
        updateFilterMap(filterElement, filter, filterMap);
        handleColorInterpolationFilters(filter, filterElement);
        return filter;
    }
    protected static int convertSides(Element filterElement,
                                      String attrName,
                                      int defaultValue,
                                      BridgeContext ctx) {
        String s = filterElement.getAttributeNS(null, attrName);
        if (s.length() == 0) {
            return defaultValue;
        } else {
            int ret = 0;
            try {
                ret = SVGUtilities.convertSVGInteger(s);
            } catch (NumberFormatException nfEx ) {
                throw new BridgeException
                    (ctx, filterElement, nfEx, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object[] {attrName, s});
            }
            if (ret <3)
                throw new BridgeException
                    (ctx, filterElement, ERR_ATTRIBUTE_VALUE_MALFORMED,
                     new Object[] {attrName, s});
            return ret;
        }
    }
}
