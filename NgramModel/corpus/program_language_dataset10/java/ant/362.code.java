package org.apache.tools.ant.taskdefs.optional;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.XmlConstants;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Iterator;
import java.util.HashMap;
import java.io.File;
import java.net.MalformedURLException;
public class SchemaValidate extends XMLValidateTask {
    private HashMap schemaLocations = new HashMap();
    private boolean fullChecking = true;
    private boolean disableDTD = false;
    private SchemaLocation anonymousSchema;
    public static final String ERROR_SAX_1 = "SAX1 parsers are not supported";
    public static final String ERROR_NO_XSD_SUPPORT
        = "Parser does not support Xerces or JAXP schema features";
    public static final String ERROR_TOO_MANY_DEFAULT_SCHEMAS
        = "Only one of defaultSchemaFile and defaultSchemaURL allowed";
    public static final String ERROR_PARSER_CREATION_FAILURE
        = "Could not create parser";
    public static final String MESSAGE_ADDING_SCHEMA = "Adding schema ";
    public static final String ERROR_DUPLICATE_SCHEMA
        = "Duplicate declaration of schema ";
    public void init() throws BuildException {
        super.init();
        setLenient(false);
    }
    public boolean enableXercesSchemaValidation() {
        try {
            setFeature(XmlConstants.FEATURE_XSD, true);
            setNoNamespaceSchemaProperty(XmlConstants.PROPERTY_NO_NAMESPACE_SCHEMA_LOCATION);
        } catch (BuildException e) {
            log(e.toString(), Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
    private void setNoNamespaceSchemaProperty(String property) {
        String anonSchema = getNoNamespaceSchemaURL();
        if (anonSchema != null) {
            setProperty(property, anonSchema);
        }
    }
    public boolean enableJAXP12SchemaValidation() {
        try {
            setProperty(XmlConstants.FEATURE_JAXP12_SCHEMA_LANGUAGE, XmlConstants.URI_XSD);
            setNoNamespaceSchemaProperty(XmlConstants.FEATURE_JAXP12_SCHEMA_SOURCE);
        } catch (BuildException e) {
            log(e.toString(), Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
    public void addConfiguredSchema(SchemaLocation location) {
        log("adding schema " + location, Project.MSG_DEBUG);
        location.validateNamespace();
        SchemaLocation old = (SchemaLocation) schemaLocations.get(location.getNamespace());
        if (old != null && !old.equals(location)) {
            throw new BuildException(ERROR_DUPLICATE_SCHEMA + location);
        }
        schemaLocations.put(location.getNamespace(), location);
    }
    public void setFullChecking(boolean fullChecking) {
        this.fullChecking = fullChecking;
    }
    protected void createAnonymousSchema() {
        if (anonymousSchema == null) {
            anonymousSchema = new SchemaLocation();
        }
        anonymousSchema.setNamespace("(no namespace)");
    }
    public void setNoNamespaceURL(String defaultSchemaURL) {
        createAnonymousSchema();
        this.anonymousSchema.setUrl(defaultSchemaURL);
    }
    public void setNoNamespaceFile(File defaultSchemaFile) {
        createAnonymousSchema();
        this.anonymousSchema.setFile(defaultSchemaFile);
    }
    public void setDisableDTD(boolean disableDTD) {
        this.disableDTD = disableDTD;
    }
    protected void initValidator() {
        super.initValidator();
        if (isSax1Parser()) {
            throw new BuildException(ERROR_SAX_1);
        }
        setFeature(XmlConstants.FEATURE_NAMESPACES, true);
        if (!enableXercesSchemaValidation() && !enableJAXP12SchemaValidation()) {
            throw new BuildException(ERROR_NO_XSD_SUPPORT);
        }
        setFeature(XmlConstants.FEATURE_XSD_FULL_VALIDATION, fullChecking);
        setFeatureIfSupported(XmlConstants.FEATURE_DISALLOW_DTD, disableDTD);
        addSchemaLocations();
    }
    protected XMLReader createDefaultReader() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        XMLReader reader = null;
        try {
            SAXParser saxParser = factory.newSAXParser();
            reader = saxParser.getXMLReader();
        } catch (ParserConfigurationException e) {
            throw new BuildException(ERROR_PARSER_CREATION_FAILURE, e);
        } catch (SAXException e) {
            throw new BuildException(ERROR_PARSER_CREATION_FAILURE, e);
        }
        return reader;
    }
    protected void addSchemaLocations() {
        Iterator it = schemaLocations.values().iterator();
        StringBuffer buffer = new StringBuffer();
        int count = 0;
        while (it.hasNext()) {
            if (count > 0) {
                buffer.append(' ');
            }
            SchemaLocation schemaLocation = (SchemaLocation) it.next();
            String tuple = schemaLocation.getURIandLocation();
            buffer.append(tuple);
            log("Adding schema " + tuple, Project.MSG_VERBOSE);
            count++;
        }
        if (count > 0) {
            setProperty(XmlConstants.PROPERTY_SCHEMA_LOCATION, buffer.toString());
        }
    }
    protected String getNoNamespaceSchemaURL() {
        if (anonymousSchema == null) {
            return null;
        } else {
            return anonymousSchema.getSchemaLocationURL();
        }
    }
    protected void setFeatureIfSupported(String feature, boolean value) {
        try {
            getXmlReader().setFeature(feature, value);
        } catch (SAXNotRecognizedException e) {
            log("Not recognizied: " + feature, Project.MSG_VERBOSE);
        } catch (SAXNotSupportedException e) {
            log("Not supported: " + feature, Project.MSG_VERBOSE);
        }
    }
    protected void onSuccessfulValidation(int fileProcessed) {
        log(fileProcessed + MESSAGE_FILES_VALIDATED, Project.MSG_VERBOSE);
    }
    public static class SchemaLocation {
        private String namespace;
        private File file;
        private String url;
        public static final String ERROR_NO_URI = "No namespace URI";
        public static final String ERROR_TWO_LOCATIONS
            = "Both URL and File were given for schema ";
        public static final String ERROR_NO_FILE = "File not found: ";
        public static final String ERROR_NO_URL_REPRESENTATION
            = "Cannot make a URL of ";
        public static final String ERROR_NO_LOCATION
            = "No file or URL supplied for the schema ";
        public SchemaLocation() {
        }
        public String getNamespace() {
            return namespace;
        }
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
        public File getFile() {
            return file;
        }
        public void setFile(File file) {
            this.file = file;
        }
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
        public String getSchemaLocationURL() {
            boolean hasFile = file != null;
            boolean hasURL = isSet(url);
            if (!hasFile && !hasURL) {
                throw new BuildException(ERROR_NO_LOCATION + namespace);
            }
            if (hasFile && hasURL) {
                throw new BuildException(ERROR_TWO_LOCATIONS + namespace);
            }
            String schema = url;
            if (hasFile) {
                if (!file.exists()) {
                    throw new BuildException(ERROR_NO_FILE + file);
                }
                try {
                    schema = FileUtils.getFileUtils().getFileURL(file).toString();
                } catch (MalformedURLException e) {
                    throw new BuildException(ERROR_NO_URL_REPRESENTATION + file, e);
                }
            }
            return schema;
        }
        public String getURIandLocation() throws BuildException {
            validateNamespace();
            StringBuffer buffer = new StringBuffer();
            buffer.append(namespace);
            buffer.append(' ');
            buffer.append(getSchemaLocationURL());
            return new String(buffer);
        }
        public void validateNamespace() {
            if (!isSet(getNamespace())) {
                throw new BuildException(ERROR_NO_URI);
            }
        }
        private boolean isSet(String property) {
            return property != null && property.length() != 0;
        }
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SchemaLocation)) {
                return false;
            }
            final SchemaLocation schemaLocation = (SchemaLocation) o;
            if (file != null ? !file.equals(schemaLocation.file) : schemaLocation.file != null) {
                return false;
            }
            if (namespace != null ? !namespace.equals(schemaLocation.namespace)
                    : schemaLocation.namespace != null) {
                return false;
            }
            if (url != null ? !url.equals(schemaLocation.url) : schemaLocation.url != null) {
                return false;
            }
            return true;
        }
        public int hashCode() {
            int result;
            result = (namespace != null ? namespace.hashCode() : 0);
            result = 29 * result + (file != null ? file.hashCode() : 0);
            result = 29 * result + (url != null ? url.hashCode() : 0);
            return result;
        }
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(namespace != null ? namespace : "(anonymous)");
            buffer.append(' ');
            buffer.append(url != null ? (url + " ") : "");
            buffer.append(file != null ? file.getAbsolutePath() : "");
            return buffer.toString();
        }
    } 
}
