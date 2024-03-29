package org.apache.batik.css.engine.value;
import java.util.StringTokenizer;
import org.w3c.css.sac.LexicalUnit;
import org.apache.batik.css.engine.value.svg.MarkerManager;
import org.apache.batik.css.engine.value.svg.OpacityManager;
import org.apache.batik.css.engine.value.svg.SVGColorManager;
import org.apache.batik.css.engine.value.svg.SVGPaintManager;
import org.apache.batik.css.engine.value.svg.SpacingManager;
import org.apache.batik.css.parser.Parser;
import org.apache.batik.test.AbstractTest;
import org.apache.batik.test.DefaultTestReport;
import org.apache.batik.test.TestReport;
import org.apache.batik.util.CSSConstants;
public class PropertyManagerTest extends AbstractTest {
    public static final String ERROR_IS_INHERITED =
        "PropertyManagerTest.error.inherited";
    public static final String ERROR_INHERIT_VALUE =
        "PropertyManagerTest.error.inherit.value";
    public static final String ERROR_INVALID_DEFAULT_VALUE =
        "PropertyManagerTest.error.invalid.default.value";
    public static final String ERROR_INVALID_VALUE =
        "PropertyManagerTest.error.invalid.value";
    public static final String ERROR_INSTANTIATION =
        "PropertyManagerTest.error.instantiation";
    protected String managerClassName;
    protected Boolean isInherited;
    protected String [] identValues;
    protected String defaultValue;
    public PropertyManagerTest(String managerClassName,
                               Boolean isInherited,
                               String defaultValue,
                               String identValueList) {
        this.managerClassName = managerClassName;
        this.isInherited = isInherited;
        this.defaultValue = defaultValue;
        StringTokenizer tokens = new StringTokenizer(identValueList, "|");
        int nbIdentValue = tokens.countTokens();
        if (nbIdentValue > 0) {
            identValues = new String[nbIdentValue];
            for (int i=0; tokens.hasMoreTokens(); ++i) {
                identValues[i] = tokens.nextToken().trim();
            }
        }
    }
    protected ValueManager createValueManager() throws Exception {
        return (ValueManager)Class.forName(managerClassName).newInstance();
    }
    public TestReport runImpl() throws Exception {
        DefaultTestReport report = new DefaultTestReport(this);
        ValueManager manager;
        try {
            manager = createValueManager();
        } catch (Exception ex) {
            report.setErrorCode(ERROR_INSTANTIATION);
            report.setPassed(false);
            report.addDescriptionEntry(ERROR_INSTANTIATION, ex.getMessage());
            return report;
        }
        if (!defaultValue.equals("__USER_AGENT__")) {
            String s = manager.getDefaultValue().getCssText();
            if (!defaultValue.equalsIgnoreCase(s)) {
                report.setErrorCode(ERROR_INVALID_DEFAULT_VALUE);
                report.setPassed(false);
                report.addDescriptionEntry(ERROR_INVALID_DEFAULT_VALUE,
                                           "should be: "+defaultValue);
            }
        }
        if (isInherited.booleanValue() != manager.isInheritedProperty()) {
            report.setErrorCode(ERROR_IS_INHERITED);
            report.setPassed(false);
            report.addDescriptionEntry(ERROR_IS_INHERITED, "");
        }
        Parser cssParser = new Parser();
        try {
            LexicalUnit lu = cssParser.parsePropertyValue("inherit");
            Value v = manager.createValue(lu, null);
            String s = v.getCssText();
            if (!"inherit".equalsIgnoreCase(s)) {
                report.setErrorCode(ERROR_INHERIT_VALUE);
                report.setPassed(false);
                report.addDescriptionEntry(ERROR_INHERIT_VALUE, "inherit");
            }
        } catch (Exception ex) {
            report.setErrorCode(ERROR_INHERIT_VALUE);
            report.setPassed(false);
            report.addDescriptionEntry(ERROR_INHERIT_VALUE, ex.getMessage());
        }
        if (identValues != null) {
            try {
                for (int i=0; i < identValues.length; ++i) {
                    LexicalUnit lu = cssParser.parsePropertyValue(identValues[i]);
                    Value v = manager.createValue(lu, null);
                    String s = v.getCssText();
                    if (!identValues[i].equalsIgnoreCase(s)) {
                        report.setErrorCode(ERROR_INVALID_VALUE);
                        report.setPassed(false);
                        report.addDescriptionEntry(ERROR_INVALID_VALUE,
                                                   identValues[i]+ '/' +s);
                    }
                }
            } catch (Exception ex) {
                report.setErrorCode(ERROR_INVALID_VALUE);
                report.setPassed(false);
                report.addDescriptionEntry(ERROR_INVALID_VALUE,
                                           ex.getMessage());
            }
        }
        return report;
    }
    public static class FillManager extends SVGPaintManager {
        public FillManager() {
            super(CSSConstants.CSS_FILL_PROPERTY);
        }
    }
    public static class FillOpacityManager extends OpacityManager {
        public FillOpacityManager() {
            super(CSSConstants.CSS_FILL_OPACITY_PROPERTY, true);
        }
    }
    public static class FloodColorManager extends SVGColorManager {
        public FloodColorManager() {
            super(CSSConstants.CSS_FLOOD_COLOR_PROPERTY);
        }
    }
    public static class FloodOpacityManager extends OpacityManager {
        public FloodOpacityManager() {
            super(CSSConstants.CSS_FLOOD_OPACITY_PROPERTY, false);
        }
    }
    public static class LetterSpacingManager extends SpacingManager {
        public LetterSpacingManager() {
            super(CSSConstants.CSS_LETTER_SPACING_PROPERTY);
        }
    }
    public static class LightingColorManager extends SVGColorManager {
        public LightingColorManager() {
            super(CSSConstants.CSS_LIGHTING_COLOR_PROPERTY, ValueConstants.WHITE_RGB_VALUE);
        }
    }
    public static class MarkerEndManager extends MarkerManager {
        public MarkerEndManager() {
            super(CSSConstants.CSS_MARKER_END_PROPERTY);
        }
    }
    public static class MarkerMidManager extends MarkerManager {
        public MarkerMidManager() {
            super(CSSConstants.CSS_MARKER_MID_PROPERTY);
        }
    }
    public static class MarkerStartManager extends MarkerManager {
        public MarkerStartManager() {
            super(CSSConstants.CSS_MARKER_START_PROPERTY);
        }
    }
    public static class DefaultOpacityManager extends OpacityManager {
        public DefaultOpacityManager() {
            super(CSSConstants.CSS_OPACITY_PROPERTY, false);
        }
    }
    public static class StopColorManager extends SVGColorManager {
        public StopColorManager() {
            super(CSSConstants.CSS_STOP_COLOR_PROPERTY);
        }
    }
    public static class StopOpacityManager extends OpacityManager {
        public StopOpacityManager() {
            super(CSSConstants.CSS_STOP_OPACITY_PROPERTY, false);
        }
    }
    public static class StrokeManager extends SVGPaintManager {
        public StrokeManager() {
            super(CSSConstants.CSS_STROKE_PROPERTY, ValueConstants.NONE_VALUE);
        }
    }
    public static class StrokeOpacityManager extends OpacityManager {
        public StrokeOpacityManager() {
            super(CSSConstants.CSS_STROKE_OPACITY_PROPERTY, true);
        }
    }
    public static class WordSpacingManager extends SpacingManager {
        public WordSpacingManager() {
            super(CSSConstants.CSS_WORD_SPACING_PROPERTY);
        }
    }
}
