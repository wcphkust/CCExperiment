package org.apache.tools.ant.taskdefs.condition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
public class ParserSupports extends ProjectComponent implements Condition {
    private String feature;
    private String property;
    private String value;
    public static final String ERROR_BOTH_ATTRIBUTES =
            "Property and feature attributes are exclusive";
    public static final String FEATURE = "feature";
    public static final String PROPERTY = "property";
    public static final String NOT_RECOGNIZED =
            " not recognized: ";
    public static final String NOT_SUPPORTED =
            " not supported: ";
    public static final String ERROR_NO_ATTRIBUTES =
        "Neither feature or property are set";
    public static final String ERROR_NO_VALUE =
        "A value is needed when testing for property support";
    public void setFeature(String feature) {
        this.feature = feature;
    }
    public void setProperty(String property) {
        this.property = property;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public boolean eval() throws BuildException {
        if (feature != null && property != null) {
            throw new BuildException(ERROR_BOTH_ATTRIBUTES);
        }
        if (feature == null && property == null) {
            throw new BuildException(ERROR_NO_ATTRIBUTES);
        }
        if (feature != null) {
            return evalFeature();
        }
        if (value == null) {
            throw new BuildException(ERROR_NO_VALUE);
        }
        return evalProperty();
    }
    private XMLReader getReader() {
        JAXPUtils.getParser();
        return JAXPUtils.getXMLReader();
    }
    public boolean evalFeature() {
        XMLReader reader = getReader();
        if (value == null) {
            value = "true";
        }
        boolean v = Project.toBoolean(value);
        try {
            reader.setFeature(feature, v);
        } catch (SAXNotRecognizedException e) {
            log(FEATURE + NOT_RECOGNIZED + feature, Project.MSG_VERBOSE);
            return false;
        } catch (SAXNotSupportedException e) {
            log(FEATURE + NOT_SUPPORTED + feature, Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
    public boolean evalProperty() {
        XMLReader reader = getReader();
        try {
            reader.setProperty(property, value);
        } catch (SAXNotRecognizedException e) {
            log(PROPERTY + NOT_RECOGNIZED + property, Project.MSG_VERBOSE);
            return false;
        } catch (SAXNotSupportedException e) {
            log(PROPERTY + NOT_SUPPORTED + property, Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
}
