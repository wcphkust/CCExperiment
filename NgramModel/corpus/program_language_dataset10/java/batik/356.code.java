package org.apache.batik.css.engine.value.css2;
import org.apache.batik.css.engine.value.IdentifierManager;
import org.apache.batik.css.engine.value.StringMap;
import org.apache.batik.css.engine.value.Value;
import org.apache.batik.css.engine.value.ValueConstants;
import org.apache.batik.css.engine.value.ValueManager;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGTypes;
public class OverflowManager extends IdentifierManager {
    protected static final StringMap values = new StringMap();
    static {
        values.put(CSSConstants.CSS_AUTO_VALUE,
                   ValueConstants.AUTO_VALUE);
        values.put(CSSConstants.CSS_HIDDEN_VALUE,
                   ValueConstants.HIDDEN_VALUE);
        values.put(CSSConstants.CSS_SCROLL_VALUE,
                   ValueConstants.SCROLL_VALUE);
        values.put(CSSConstants.CSS_VISIBLE_VALUE,
                   ValueConstants.VISIBLE_VALUE);
    }
    public boolean isInheritedProperty() {
        return false;
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
        return CSSConstants.CSS_OVERFLOW_PROPERTY;
    }
    public Value getDefaultValue() {
        return ValueConstants.VISIBLE_VALUE;
    }
    public StringMap getIdentifiers() {
        return values;
    }
}
