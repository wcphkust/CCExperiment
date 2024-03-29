package org.apache.batik.css.engine.value.svg12;
import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.css.engine.CSSStylableElement;
import org.apache.batik.css.engine.StyleMap;
import org.apache.batik.css.engine.value.LengthManager;
import org.apache.batik.css.engine.value.FloatValue;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.util.SVG12CSSConstants;
import org.apache.batik.util.SVGTypes;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.DOMException;
public class LineHeightManager extends LengthManager {
    public LineHeightManager() { }
    public boolean isInheritedProperty() {
        return true;
    }
    public boolean isAnimatableProperty() {
        return true;
    }
    public boolean isAdditiveProperty() {
        return true;
    }
    public int getPropertyType() {
        return SVGTypes.TYPE_LINE_HEIGHT_VALUE;
    }
    public String getPropertyName() {
        return SVG12CSSConstants.CSS_LINE_HEIGHT_PROPERTY;
    }
    public Value getDefaultValue() {
        return SVG12ValueConstants.NORMAL_VALUE;
    }
    public Value createValue(LexicalUnit lu, CSSEngine engine)
        throws DOMException {
        switch (lu.getLexicalUnitType()) {
        case LexicalUnit.SAC_INHERIT:
            return SVG12ValueConstants.INHERIT_VALUE;
        case LexicalUnit.SAC_IDENT: {
            String s = lu.getStringValue().toLowerCase();
            if (SVG12CSSConstants.CSS_NORMAL_VALUE.equals(s))
                return SVG12ValueConstants.NORMAL_VALUE;
            throw createInvalidIdentifierDOMException(lu.getStringValue());
        }
        default:
            return super.createValue(lu, engine);
        }
    }
    protected int getOrientation() {
        return VERTICAL_ORIENTATION;
    }
    public Value computeValue(CSSStylableElement elt,
                              String pseudo,
                              CSSEngine engine,
                              int idx,
                              StyleMap sm,
                              Value value) {
        if (value.getCssValueType() != CSSValue.CSS_PRIMITIVE_VALUE)
            return value;
        switch (value.getPrimitiveType()) {
        case CSSPrimitiveValue.CSS_NUMBER:
            return new LineHeightValue(CSSPrimitiveValue.CSS_NUMBER,
                                       value.getFloatValue(), true);
        case CSSPrimitiveValue.CSS_PERCENTAGE: {
            float v     = value.getFloatValue();
            int   fsidx = engine.getFontSizeIndex();
            float fs    = engine.getComputedStyle
                (elt, pseudo, fsidx).getFloatValue();
            return new FloatValue(CSSPrimitiveValue.CSS_NUMBER, v * fs * 0.01f);
        }
        default:
            return super.computeValue(elt, pseudo, engine, idx, sm, value);
        }
    }
}
