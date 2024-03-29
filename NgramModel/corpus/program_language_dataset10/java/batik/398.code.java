package org.apache.batik.css.engine.value.svg;
import org.apache.batik.css.engine.value.IdentifierManager;
import org.apache.batik.css.engine.value.StringMap;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGTypes;
public class TextRenderingManager extends IdentifierManager {
    protected static final StringMap values = new StringMap();
    static {
        values.put(CSSConstants.CSS_AUTO_VALUE,
                   SVGValueConstants.AUTO_VALUE);
        values.put(CSSConstants.CSS_OPTIMIZESPEED_VALUE,
                   SVGValueConstants.OPTIMIZESPEED_VALUE);
        values.put(CSSConstants.CSS_GEOMETRICPRECISION_VALUE,
                   SVGValueConstants.GEOMETRICPRECISION_VALUE);
        values.put(CSSConstants.CSS_OPTIMIZELEGIBILITY_VALUE,
                   SVGValueConstants.OPTIMIZELEGIBILITY_VALUE);
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
        return SVGTypes.TYPE_IDENT;
    }
    public String getPropertyName() {
        return CSSConstants.CSS_TEXT_RENDERING_PROPERTY;
    }
    public Value getDefaultValue() {
        return SVGValueConstants.AUTO_VALUE;
    }
    public StringMap getIdentifiers() {
        return values;
    }
}
