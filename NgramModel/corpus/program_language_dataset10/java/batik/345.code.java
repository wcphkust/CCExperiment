package org.apache.batik.css.engine.value.css2;
import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.css.engine.CSSStylableElement;
import org.apache.batik.css.engine.StyleMap;
import org.apache.batik.css.engine.value.AbstractValueManager;
import org.apache.batik.css.engine.value.ListValue;
import org.apache.batik.css.engine.value.StringMap;
import org.apache.batik.css.engine.value.URIValue;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueConstants;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGTypes;
import org.w3c.css.sac.LexicalUnit;
import org.w3c.dom.DOMException;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;
public class CursorManager extends AbstractValueManager {
    protected static final StringMap values = new StringMap();
    static {
        values.put(CSSConstants.CSS_AUTO_VALUE,
                   ValueConstants.AUTO_VALUE);
        values.put(CSSConstants.CSS_CROSSHAIR_VALUE,
                   ValueConstants.CROSSHAIR_VALUE);
        values.put(CSSConstants.CSS_DEFAULT_VALUE,
                   ValueConstants.DEFAULT_VALUE);
        values.put(CSSConstants.CSS_E_RESIZE_VALUE,
                   ValueConstants.E_RESIZE_VALUE);
        values.put(CSSConstants.CSS_HELP_VALUE,
                   ValueConstants.HELP_VALUE);
        values.put(CSSConstants.CSS_MOVE_VALUE,
                   ValueConstants.MOVE_VALUE);
        values.put(CSSConstants.CSS_N_RESIZE_VALUE,
                   ValueConstants.N_RESIZE_VALUE);
        values.put(CSSConstants.CSS_NE_RESIZE_VALUE,
                   ValueConstants.NE_RESIZE_VALUE);
        values.put(CSSConstants.CSS_NW_RESIZE_VALUE,
                   ValueConstants.NW_RESIZE_VALUE);
        values.put(CSSConstants.CSS_POINTER_VALUE,
                   ValueConstants.POINTER_VALUE);
        values.put(CSSConstants.CSS_S_RESIZE_VALUE,
                   ValueConstants.S_RESIZE_VALUE);
        values.put(CSSConstants.CSS_SE_RESIZE_VALUE,
                   ValueConstants.SE_RESIZE_VALUE);
        values.put(CSSConstants.CSS_SW_RESIZE_VALUE,
                   ValueConstants.SW_RESIZE_VALUE);
        values.put(CSSConstants.CSS_TEXT_VALUE,
                   ValueConstants.TEXT_VALUE);
        values.put(CSSConstants.CSS_W_RESIZE_VALUE,
                   ValueConstants.W_RESIZE_VALUE);
        values.put(CSSConstants.CSS_WAIT_VALUE,
                   ValueConstants.WAIT_VALUE);
    }
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
        return SVGTypes.TYPE_CURSOR_VALUE;
    }
    public String getPropertyName() {
        return CSSConstants.CSS_CURSOR_PROPERTY;
    }
    public Value getDefaultValue() {
        return ValueConstants.AUTO_VALUE;
    }
    public Value createValue(LexicalUnit lu, CSSEngine engine)
        throws DOMException {
        ListValue result = new ListValue();
        switch (lu.getLexicalUnitType()) {
        case LexicalUnit.SAC_INHERIT:
            return ValueConstants.INHERIT_VALUE;
        case LexicalUnit.SAC_URI:
            do {
                result.append(new URIValue(lu.getStringValue(),
                                           resolveURI(engine.getCSSBaseURI(),
                                                      lu.getStringValue())));
                lu = lu.getNextLexicalUnit();
                if (lu == null) {
                    throw createMalformedLexicalUnitDOMException();
                }
                if (lu.getLexicalUnitType() !=
                    LexicalUnit.SAC_OPERATOR_COMMA) {
                    throw createInvalidLexicalUnitDOMException
                        (lu.getLexicalUnitType());
                }
                lu = lu.getNextLexicalUnit();
                if (lu == null) {
                    throw createMalformedLexicalUnitDOMException();
                }
            } while (lu.getLexicalUnitType() == LexicalUnit.SAC_URI);
            if (lu.getLexicalUnitType() != LexicalUnit.SAC_IDENT) {
                throw createInvalidLexicalUnitDOMException
                    (lu.getLexicalUnitType());
            }
        case LexicalUnit.SAC_IDENT:
            String s = lu.getStringValue().toLowerCase().intern();
            Object v = values.get(s);
            if (v == null) {
                throw createInvalidIdentifierDOMException(lu.getStringValue());
            }
            result.append((Value)v);
            lu = lu.getNextLexicalUnit();
        }
        if (lu != null) {
            throw createInvalidLexicalUnitDOMException
                (lu.getLexicalUnitType());
        }
        return result;
    }
    public Value computeValue(CSSStylableElement elt,
                              String pseudo,
                              CSSEngine engine,
                              int idx,
                              StyleMap sm,
                              Value value) {
        if (value.getCssValueType() == CSSValue.CSS_VALUE_LIST) {
            ListValue lv = (ListValue)value;
            int len = lv.getLength();
            ListValue result = new ListValue(' ');
            for (int i=0; i<len; i++) {
                Value v = lv.item(0);
                if (v.getPrimitiveType() == CSSPrimitiveValue.CSS_URI) {
                    result.append(new URIValue(v.getStringValue(),
                                               v.getStringValue()));
                } else {
                    result.append(v);
                }
            }
            return result;
        }
        return super.computeValue(elt, pseudo, engine, idx, sm, value);
    }
}
