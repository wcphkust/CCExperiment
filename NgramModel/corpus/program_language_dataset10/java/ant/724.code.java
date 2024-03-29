package org.apache.tools.ant.types.selectors;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Parameter;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.util.regexp.Regexp;
import org.apache.tools.ant.util.regexp.RegexpUtil;
public class ContainsRegexpSelector extends BaseExtendSelector
        implements ResourceSelector {
    private String userProvidedExpression = null;
    private RegularExpression myRegExp = null;
    private Regexp myExpression = null;
    private boolean caseSensitive = true;
    private boolean multiLine = false;
    private boolean singleLine = false;
    public static final String EXPRESSION_KEY = "expression";
    private static final String CS_KEY = "casesensitive";
    private static final String ML_KEY = "multiline";
    private static final String SL_KEY = "singleline";
    public ContainsRegexpSelector() {
    }
    public String toString() {
        StringBuffer buf = new StringBuffer(
                "{containsregexpselector expression: ");
        buf.append(userProvidedExpression);
        buf.append("}");
        return buf.toString();
    }
    public void setExpression(String theexpression) {
        this.userProvidedExpression = theexpression;
    }
    public void setCaseSensitive(boolean b) {
        caseSensitive = b;
    }
    public void setMultiLine(boolean b) {
        multiLine = b;
    }
    public void setSingleLine(boolean b) {
        singleLine = b;
    }
    public void setParameters(Parameter[] parameters) {
        super.setParameters(parameters);
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                String paramname = parameters[i].getName();
                if (EXPRESSION_KEY.equalsIgnoreCase(paramname)) {
                    setExpression(parameters[i].getValue());
                } else if (CS_KEY.equalsIgnoreCase(paramname)) {
                    setCaseSensitive(Project
                                     .toBoolean(parameters[i].getValue()));
                } else if (ML_KEY.equalsIgnoreCase(paramname)) {
                    setMultiLine(Project.toBoolean(parameters[i].getValue()));
                } else if (SL_KEY.equalsIgnoreCase(paramname)) {
                    setSingleLine(Project.toBoolean(parameters[i].getValue()));
                } else {
                    setError("Invalid parameter " + paramname);
                }
            }
        }
    }
    public void verifySettings() {
        if (userProvidedExpression == null) {
            setError("The expression attribute is required");
        }
    }
    public boolean isSelected(File basedir, String filename, File file) {
        return isSelected(new FileResource(file));
    }
    public boolean isSelected(Resource r) {
        String teststr = null;
        BufferedReader in = null;
        validate();
        if (r.isDirectory()) {
            return true;
        }
        if (myRegExp == null) {
            myRegExp = new RegularExpression();
            myRegExp.setPattern(userProvidedExpression);
            myExpression = myRegExp.getRegexp(getProject());
        }
        try {
            in = new BufferedReader(new InputStreamReader(r.getInputStream()));
        } catch (Exception e) {
            throw new BuildException("Could not get InputStream from "
                    + r.toLongString(), e);
        }
        try {
            teststr = in.readLine();
            while (teststr != null) {
                if (myExpression.matches(teststr,
                                         RegexpUtil.asOptions(caseSensitive,
                                                              multiLine,
                                                              singleLine))) {
                    return true;
                }
                teststr = in.readLine();
            }
            return false;
        } catch (IOException ioe) {
            throw new BuildException("Could not read " + r.toLongString());
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                throw new BuildException("Could not close "
                                         + r.toLongString());
            }
        }
    }
}
