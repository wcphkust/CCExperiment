package org.apache.batik.css.engine.value.svg;
import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.css.engine.value.LengthManager;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGTypes;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSPrimitiveValue;
public class KerningManager extends LengthManager {
    public boolean isInheritedProperty() {
        return true;
    }
    public String getPropertyName() {
        return CSSConstants.CSS_KERNING_PROPERTY;
    }
    public boolean isAnimatableProperty() {
        return true;
    }
    public boolean isAdditiveProperty() {
        return true;
    }
    public int getPropertyType() {
        return SVGTypes.TYPE_KERNING_VALUE;
    }
    public Value getDefaultValue() {
        return SVGValueConstants.AUTO_VALUE;
    }
    public Value createValue(LexicalUnit lu, CSSEngine engine)
        throws DOMException {
        switch (lu.getLexicalUnitType()) {
        case LexicalUnit.SAC_INHERIT:
            return SVGValueConstants.INHERIT_VALUE;
        case LexicalUnit.SAC_IDENT:
            if (lu.getStringValue().equalsIgnoreCase
                (CSSConstants.CSS_AUTO_VALUE)) {
                return SVGValueConstants.AUTO_VALUE;
            }
            throw createInvalidIdentifierDOMException(lu.getStringValue());
        }
        return super.createValue(lu, engine);
    }
    public Value createStringValue(short type, String value, CSSEngine engine)
        throws DOMException {
        if (type != CSSPrimitiveValue.CSS_IDENT) {
            throw createInvalidStringTypeDOMException(type);
        }
        if (value.equalsIgnoreCase(CSSConstants.CSS_AUTO_VALUE)) {
            return SVGValueConstants.AUTO_VALUE;
        }
        throw createInvalidIdentifierDOMException(value);
    }
    protected int getOrientation() {
        return HORIZONTAL_ORIENTATION;
    }
}
