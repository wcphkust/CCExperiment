package org.apache.batik.css.engine.value.svg;
import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.css.engine.CSSStylableElement;
import org.apache.batik.css.engine.StyleMap;
import org.apache.batik.css.engine.value.LengthManager;
import org.apache.batik.css.engine.value.ListValue;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGTypes;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;
public class StrokeDasharrayManager extends LengthManager {
    public boolean isInheritedProperty() {
        return true;
    }
    public boolean isAnimatableProperty() {
        return true;
    }
    public boolean isAdditiveProperty() {
        return false;
    }
    public int getPropertyType() {
        return SVGTypes.TYPE_LENGTH_LIST_OR_IDENT;
    }
    public String getPropertyName() {
        return CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY;
    }
    public Value getDefaultValue() {
        return SVGValueConstants.NONE_VALUE;
    }
    public Value createValue(LexicalUnit lu, CSSEngine engine)
        throws DOMException {
        switch (lu.getLexicalUnitType()) {
        case LexicalUnit.SAC_INHERIT:
            return SVGValueConstants.INHERIT_VALUE;
        case LexicalUnit.SAC_IDENT:
            if (lu.getStringValue().equalsIgnoreCase
                (CSSConstants.CSS_NONE_VALUE)) {
                return SVGValueConstants.NONE_VALUE;
            }
            throw createInvalidIdentifierDOMException(lu.getStringValue());
        default:
            ListValue lv = new ListValue(' ');
            do {
                Value v = super.createValue(lu, engine);
                lv.append(v);
                lu = lu.getNextLexicalUnit();
                if (lu != null &&
                    lu.getLexicalUnitType() ==
                    LexicalUnit.SAC_OPERATOR_COMMA) {
                    lu = lu.getNextLexicalUnit();
                }
            } while (lu != null);
            return lv;
        }
    }
    public Value createStringValue(short type, String value, CSSEngine engine)
        throws DOMException {
        if (type != CSSPrimitiveValue.CSS_IDENT) {
            throw createInvalidStringTypeDOMException(type);
        }
        if (value.equalsIgnoreCase(CSSConstants.CSS_NONE_VALUE)) {
            return SVGValueConstants.NONE_VALUE;
        }
        throw createInvalidIdentifierDOMException(value);
    }
    public Value computeValue(CSSStylableElement elt,
                              String pseudo,
                              CSSEngine engine,
                              int idx,
                              StyleMap sm,
                              Value value) {
        switch (value.getCssValueType()) {
        case CSSValue.CSS_PRIMITIVE_VALUE:
            return value;
        }
        ListValue lv = (ListValue)value;
        ListValue result = new ListValue(' ');
        for (int i = 0; i < lv.getLength(); i++) {
            result.append(super.computeValue(elt, pseudo, engine, idx, sm,
                                             lv.item(i)));
        }
        return result;
    }
    protected int getOrientation() {
        return BOTH_ORIENTATION;
    }
}
