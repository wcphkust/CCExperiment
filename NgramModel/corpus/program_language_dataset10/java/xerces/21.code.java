package sax;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.ParserAdapter;
import org.xml.sax.helpers.ParserFactory;
import org.xml.sax.helpers.XMLReaderFactory;
import sax.helpers.AttributesImpl;
public class Writer
    extends DefaultHandler
    implements LexicalHandler {
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    protected static final String NAMESPACE_PREFIXES_FEATURE_ID = "http://xml.org/sax/features/namespace-prefixes";
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
    protected static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID = "http://apache.org/xml/features/honour-all-schemaLocations";
    protected static final String VALIDATE_ANNOTATIONS_ID = "http://apache.org/xml/features/validate-annotations";
    protected static final String GENERATE_SYNTHETIC_ANNOTATIONS_ID = "http://apache.org/xml/features/generate-synthetic-annotations";
    protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";
    protected static final String LOAD_EXTERNAL_DTD_FEATURE_ID = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    protected static final String XINCLUDE_FEATURE_ID = "http://apache.org/xml/features/xinclude";
    protected static final String XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID = "http://apache.org/xml/features/xinclude/fixup-base-uris";
    protected static final String XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID = "http://apache.org/xml/features/xinclude/fixup-language";
    protected static final String LEXICAL_HANDLER_PROPERTY_ID = "http://xml.org/sax/properties/lexical-handler";
    protected static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
    protected static final boolean DEFAULT_NAMESPACES = true;
    protected static final boolean DEFAULT_NAMESPACE_PREFIXES = false;
    protected static final boolean DEFAULT_VALIDATION = false;
    protected static final boolean DEFAULT_LOAD_EXTERNAL_DTD = true;
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;
    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;
    protected static final boolean DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS = false;
    protected static final boolean DEFAULT_VALIDATE_ANNOTATIONS = false;
    protected static final boolean DEFAULT_GENERATE_SYNTHETIC_ANNOTATIONS = false;
    protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;
    protected static final boolean DEFAULT_XINCLUDE = false;
    protected static final boolean DEFAULT_XINCLUDE_FIXUP_BASE_URIS = true;
    protected static final boolean DEFAULT_XINCLUDE_FIXUP_LANGUAGE = true;
    protected static final boolean DEFAULT_CANONICAL = false;
    protected PrintWriter fOut;
    protected boolean fCanonical;
    protected int fElementDepth;
    protected Locator fLocator;
    protected boolean fXML11;
    protected boolean fInCDATA;
    public Writer() {
    } 
    public void setCanonical(boolean canonical) {
        fCanonical = canonical;
    } 
    public void setOutput(OutputStream stream, String encoding)
        throws UnsupportedEncodingException {
        if (encoding == null) {
            encoding = "UTF8";
        }
        java.io.Writer writer = new OutputStreamWriter(stream, encoding);
        fOut = new PrintWriter(writer);
    } 
    public void setOutput(java.io.Writer writer) {
        fOut = writer instanceof PrintWriter
             ? (PrintWriter)writer : new PrintWriter(writer);
    } 
    public void setDocumentLocator(Locator locator) {
    	fLocator = locator;
    } 
    public void startDocument() throws SAXException {
        fElementDepth = 0;
        fXML11 = false;
        fInCDATA = false;
    } 
    public void processingInstruction(String target, String data)
        throws SAXException {
        if (fElementDepth > 0) {
            fOut.print("<?");
            fOut.print(target);
            if (data != null && data.length() > 0) {
                fOut.print(' ');
                fOut.print(data);
            }
            fOut.print("?>");
            fOut.flush();
        }
    } 
    public void startElement(String uri, String local, String raw,
                             Attributes attrs) throws SAXException {
        if (fElementDepth == 0) {
            String encoding = "UTF-8";
            if (fLocator != null) {
                if (fLocator instanceof Locator2) {
                    Locator2 locator2 = (Locator2) fLocator;
                    fXML11 = "1.1".equals(locator2.getXMLVersion());
                    encoding = locator2.getEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }
                }
                fLocator = null;
            }
            if (!fCanonical) {
                fOut.print("<?xml version=\"");
                fOut.print(fXML11 ? "1.1" : "1.0");
                fOut.print("\" encoding=\"");
                fOut.print(encoding);
                fOut.println("\"?>");
                fOut.flush();
            }
        }
        fElementDepth++;
        fOut.print('<');
        fOut.print(raw);
        if (attrs != null) {
            attrs = sortAttributes(attrs);
            int len = attrs.getLength();
            for (int i = 0; i < len; i++) {
                fOut.print(' ');
                fOut.print(attrs.getQName(i));
                fOut.print("=\"");
                normalizeAndPrint(attrs.getValue(i), true);
                fOut.print('"');
            }
        }
        fOut.print('>');
        fOut.flush();
    } 
    public void characters(char ch[], int start, int length)
        throws SAXException {
        if (!fInCDATA) {
            normalizeAndPrint(ch, start, length, false);
        }
        else {
            for (int i = 0; i < length; ++i) {
            	fOut.print(ch[start+i]);
            }
        }
        fOut.flush();
    } 
    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {
        characters(ch, start, length);
        fOut.flush();
    } 
    public void endElement(String uri, String local, String raw)
        throws SAXException {
        fElementDepth--;
        fOut.print("</");
        fOut.print(raw);
        fOut.print('>');
        fOut.flush();
    } 
    public void warning(SAXParseException ex) throws SAXException {
        printError("Warning", ex);
    } 
    public void error(SAXParseException ex) throws SAXException {
        printError("Error", ex);
    } 
    public void fatalError(SAXParseException ex) throws SAXException {
        printError("Fatal Error", ex);
        throw ex;
    } 
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException {
    } 
    public void endDTD() throws SAXException {
    } 
    public void startEntity(String name) throws SAXException {
    } 
    public void endEntity(String name) throws SAXException {
    } 
    public void startCDATA() throws SAXException {
        if (!fCanonical) {
            fOut.print("<![CDATA[");
            fInCDATA = true;
        }
    } 
    public void endCDATA() throws SAXException {
        if (!fCanonical) {
            fInCDATA = false;
            fOut.print("]]>");
        }
    } 
    public void comment(char ch[], int start, int length) throws SAXException {
        if (!fCanonical && fElementDepth > 0) {
            fOut.print("<!--");
            for (int i = 0; i < length; ++i) {
                fOut.print(ch[start+i]);
            }
            fOut.print("-->");
            fOut.flush();
        }
    } 
    protected Attributes sortAttributes(Attributes attrs) {
        AttributesImpl attributes = new AttributesImpl();
        int len = (attrs != null) ? attrs.getLength() : 0;
        for (int i = 0; i < len; i++) {
            String name = attrs.getQName(i);
            int count = attributes.getLength();
            int j = 0;
            while (j < count) {
                if (name.compareTo(attributes.getQName(j)) < 0) {
                    break;
                }
                j++;
            }
            attributes.insertAttributeAt(j, name, attrs.getType(i),
                                         attrs.getValue(i));
        }
        return attributes;
    } 
    protected void normalizeAndPrint(String s, boolean isAttValue) {
        int len = (s != null) ? s.length() : 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            normalizeAndPrint(c, isAttValue);
        }
    } 
    protected void normalizeAndPrint(char[] ch, int offset, int length, boolean isAttValue) {
        for (int i = 0; i < length; i++) {
            normalizeAndPrint(ch[offset + i], isAttValue);
        }
    } 
    protected void normalizeAndPrint(char c, boolean isAttValue) {
        switch (c) {
            case '<': {
                fOut.print("&lt;");
                break;
            }
            case '>': {
                fOut.print("&gt;");
                break;
            }
            case '&': {
                fOut.print("&amp;");
                break;
            }
            case '"': {
                if (isAttValue) {
                    fOut.print("&quot;");
                }
                else {
                    fOut.print("\"");
                }
                break;
            }
            case '\r': {
            	fOut.print("&#xD;");
            	break;
            }
            case '\n': {
                if (fCanonical) {
                    fOut.print("&#xA;");
                    break;
                }
            }
            default: {
            	if (fXML11 && ((c >= 0x01 && c <= 0x1F && c != 0x09 && c != 0x0A) 
            	    || (c >= 0x7F && c <= 0x9F) || c == 0x2028)
            	    || isAttValue && (c == 0x09 || c == 0x0A)) {
            	    fOut.print("&#x");
            	    fOut.print(Integer.toHexString(c).toUpperCase());
            	    fOut.print(";");
                }
                else {
                    fOut.print(c);
                }        
            }
        }
    } 
    protected void printError(String type, SAXParseException ex) {
        System.err.print("[");
        System.err.print(type);
        System.err.print("] ");
        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1)
                systemId = systemId.substring(index + 1);
            System.err.print(systemId);
        }
        System.err.print(':');
        System.err.print(ex.getLineNumber());
        System.err.print(':');
        System.err.print(ex.getColumnNumber());
        System.err.print(": ");
        System.err.print(ex.getMessage());
        System.err.println();
        System.err.flush();
    } 
    public static void main(String argv[]) {
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }
        Writer writer = null;
        XMLReader parser = null;
        boolean namespaces = DEFAULT_NAMESPACES;
        boolean namespacePrefixes = DEFAULT_NAMESPACE_PREFIXES;
        boolean validation = DEFAULT_VALIDATION;
        boolean externalDTD = DEFAULT_LOAD_EXTERNAL_DTD;
        boolean schemaValidation = DEFAULT_SCHEMA_VALIDATION;
        boolean schemaFullChecking = DEFAULT_SCHEMA_FULL_CHECKING;
        boolean honourAllSchemaLocations = DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS;
        boolean validateAnnotations = DEFAULT_VALIDATE_ANNOTATIONS;
        boolean generateSyntheticAnnotations = DEFAULT_GENERATE_SYNTHETIC_ANNOTATIONS;
        boolean dynamicValidation = DEFAULT_DYNAMIC_VALIDATION;
        boolean xincludeProcessing = DEFAULT_XINCLUDE;
        boolean xincludeFixupBaseURIs = DEFAULT_XINCLUDE_FIXUP_BASE_URIS;
        boolean xincludeFixupLanguage = DEFAULT_XINCLUDE_FIXUP_LANGUAGE;
        boolean canonical = DEFAULT_CANONICAL;
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if (option.equals("p")) {
                    if (++i == argv.length) {
                        System.err.println("error: Missing argument to -p option.");
                    }
                    String parserName = argv[i];
                    try {
                        parser = XMLReaderFactory.createXMLReader(parserName);
                    }
                    catch (Exception e) {
                        try {
                            Parser sax1Parser = ParserFactory.makeParser(parserName);
                            parser = new ParserAdapter(sax1Parser);
                            System.err.println("warning: Features and properties not supported on SAX1 parsers.");
                        }
                        catch (Exception ex) {
                            parser = null;
                            System.err.println("error: Unable to instantiate parser ("+parserName+")");
                            e.printStackTrace(System.err);
                        }
                    }
                    continue;
                }
                if (option.equalsIgnoreCase("n")) {
                    namespaces = option.equals("n");
                    continue;
                }
                if (option.equalsIgnoreCase("np")) {
                    namespacePrefixes = option.equals("np");
                    continue;
                }
                if (option.equalsIgnoreCase("v")) {
                    validation = option.equals("v");
                    continue;
                }
                if (option.equalsIgnoreCase("xd")) {
                    externalDTD = option.equals("xd");
                    continue;
                }
                if (option.equalsIgnoreCase("s")) {
                    schemaValidation = option.equals("s");
                    continue;
                }
                if (option.equalsIgnoreCase("f")) {
                    schemaFullChecking = option.equals("f");
                    continue;
                }
                if (option.equalsIgnoreCase("hs")) {
                    honourAllSchemaLocations = option.equals("hs");
                    continue;
                }
                if (option.equalsIgnoreCase("va")) {
                    validateAnnotations = option.equals("va");
                    continue;
                }
                if (option.equalsIgnoreCase("ga")) {
                    generateSyntheticAnnotations = option.equals("ga");
                    continue;
                }
                if (option.equalsIgnoreCase("dv")) {
                    dynamicValidation = option.equals("dv");
                    continue;
                }
                if (option.equalsIgnoreCase("xi")) {
                    xincludeProcessing = option.equals("xi");
                    continue;
                }
                if (option.equalsIgnoreCase("xb")) {
                    xincludeFixupBaseURIs = option.equals("xb");
                    continue;
                }
                if (option.equalsIgnoreCase("xl")) {
                    xincludeFixupLanguage = option.equals("xl");
                    continue;
                }
                if (option.equalsIgnoreCase("c")) {
                    canonical = option.equals("c");
                    continue;
                }
                if (option.equals("h")) {
                    printUsage();
                    continue;
                }
            }
            if (parser == null) {
                try {
                    parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
                }
                catch (Exception e) {
                    System.err.println("error: Unable to instantiate parser ("+DEFAULT_PARSER_NAME+")");
                    e.printStackTrace(System.err);
                    continue;
                }
            }
            try {
                parser.setFeature(NAMESPACES_FEATURE_ID, namespaces);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+NAMESPACES_FEATURE_ID+")");
            }
            try {
                parser.setFeature(NAMESPACE_PREFIXES_FEATURE_ID, namespacePrefixes);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+NAMESPACE_PREFIXES_FEATURE_ID+")");
            }
            try {
                parser.setFeature(VALIDATION_FEATURE_ID, validation);
            }
            catch (SAXException e) {
                System.err.println("warning: Parser does not support feature ("+VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(LOAD_EXTERNAL_DTD_FEATURE_ID, externalDTD);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+LOAD_EXTERNAL_DTD_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+LOAD_EXTERNAL_DTD_FEATURE_ID+")");
            }
            try {
                parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, schemaValidation);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");
            }
            try {
                parser.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, honourAllSchemaLocations);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+HONOUR_ALL_SCHEMA_LOCATIONS_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+HONOUR_ALL_SCHEMA_LOCATIONS_ID+")");
            }
            try {
                parser.setFeature(VALIDATE_ANNOTATIONS_ID, validateAnnotations);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+VALIDATE_ANNOTATIONS_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+VALIDATE_ANNOTATIONS_ID+")");
            }
            try {
                parser.setFeature(GENERATE_SYNTHETIC_ANNOTATIONS_ID, generateSyntheticAnnotations);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+GENERATE_SYNTHETIC_ANNOTATIONS_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+GENERATE_SYNTHETIC_ANNOTATIONS_ID+")");
            }
            try {
                parser.setFeature(DYNAMIC_VALIDATION_FEATURE_ID, dynamicValidation);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+DYNAMIC_VALIDATION_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+DYNAMIC_VALIDATION_FEATURE_ID+")");
            }
            try {
                parser.setFeature(XINCLUDE_FEATURE_ID, xincludeProcessing);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+XINCLUDE_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+XINCLUDE_FEATURE_ID+")");
            }
            try {
                parser.setFeature(XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID, xincludeFixupBaseURIs);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID+")");
            }
            try {
                parser.setFeature(XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID, xincludeFixupLanguage);
            }
            catch (SAXNotRecognizedException e) {
                System.err.println("warning: Parser does not recognize feature ("+XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID+")");
            }
            catch (SAXNotSupportedException e) {
                System.err.println("warning: Parser does not support feature ("+XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID+")");
            }
            if (writer == null) {
                writer = new Writer();
                try {
                    writer.setOutput(System.out, "UTF8");
                }
                catch (UnsupportedEncodingException e) {
                    System.err.println("error: Unable to set output. Exiting.");
                    System.exit(1);
                }
            }
            parser.setContentHandler(writer);
            parser.setErrorHandler(writer);
            try {
                parser.setProperty(LEXICAL_HANDLER_PROPERTY_ID, writer);
            }
            catch (SAXException e) {
            }
            writer.setCanonical(canonical);
            try {
                parser.parse(arg);
            }
            catch (SAXParseException e) {
            }
            catch (Exception e) {
                System.err.println("error: Parse error occurred - "+e.getMessage());
                if (e instanceof SAXException) {
                    Exception nested = ((SAXException)e).getException();
                    if (nested != null) {
                        e = nested;
                    } 
                }
                e.printStackTrace(System.err);
            }
        }
    } 
    private static void printUsage() {
        System.err.println("usage: java sax.Writer (options) uri ...");
        System.err.println();
        System.err.println("options:");
        System.err.println("  -p name     Select parser by name.");
        System.err.println("  -n  | -N    Turn on/off namespace processing.");
        System.err.println("  -np | -NP   Turn on/off namespace prefixes.");
        System.err.println("              NOTE: Requires use of -n.");
        System.err.println("  -v  | -V    Turn on/off validation.");
        System.err.println("  -xd | -XD   Turn on/off loading of external DTDs.");
        System.err.println("              NOTE: Always on when -v in use and not supported by all parsers.");
        System.err.println("  -s  | -S    Turn on/off Schema validation support.");
        System.err.println("              NOTE: Not supported by all parsers.");
        System.err.println("  -f  | -F    Turn on/off Schema full checking.");
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.");
        System.err.println("  -hs | -HS   Turn on/off honouring of all schema locations.");
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.");
        System.err.println("  -va | -VA   Turn on/off validation of schema annotations.");
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.");
        System.err.println("  -ga | -GA   Turn on/off generation of synthetic schema annotations.");
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.");
        System.err.println("  -dv | -DV   Turn on/off dynamic validation.");
        System.err.println("              NOTE: Not supported by all parsers.");
        System.err.println("  -xi | -XI   Turn on/off XInclude processing.");
        System.err.println("              NOTE: Not supported by all parsers.");
        System.err.println("  -xb | -XB   Turn on/off base URI fixup during XInclude processing.");
        System.err.println("              NOTE: Requires use of -xi and not supported by all parsers.");
        System.err.println("  -xl | -XL   Turn on/off language fixup during XInclude processing.");
        System.err.println("              NOTE: Requires use of -xi and not supported by all parsers.");
        System.err.println("  -c | -C     Turn on/off Canonical XML output.");
        System.err.println("              NOTE: This is not W3C canonical output.");
        System.err.println("  -h          This help screen.");
        System.err.println();
        System.err.println("defaults:");
        System.err.println("  Parser:     "+DEFAULT_PARSER_NAME);
        System.err.print("  Namespaces: ");
        System.err.println(DEFAULT_NAMESPACES ? "on" : "off");
        System.err.print("  Prefixes:   ");
        System.err.println(DEFAULT_NAMESPACE_PREFIXES ? "on" : "off");
        System.err.print("  Validation: ");
        System.err.println(DEFAULT_VALIDATION ? "on" : "off");
        System.err.print("  Load External DTD: ");
        System.err.println(DEFAULT_LOAD_EXTERNAL_DTD ? "on" : "off");
        System.err.print("  Schema:     ");
        System.err.println(DEFAULT_SCHEMA_VALIDATION ? "on" : "off");
        System.err.print("  Schema full checking:     ");
        System.err.println(DEFAULT_SCHEMA_FULL_CHECKING ? "on" : "off");
        System.err.print("  Dynamic:    ");
        System.err.println(DEFAULT_DYNAMIC_VALIDATION ? "on" : "off");
        System.err.print("  Canonical:  ");
        System.err.println(DEFAULT_CANONICAL ? "on" : "off");
        System.err.print("  Honour all schema locations:       ");
        System.err.println(DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS ? "on" : "off");
        System.err.print("  Validate Annotations:              ");
        System.err.println(DEFAULT_VALIDATE_ANNOTATIONS ? "on" : "off");
        System.err.print("  Generate Synthetic Annotations:    ");
        System.err.println(DEFAULT_GENERATE_SYNTHETIC_ANNOTATIONS ? "on" : "off");
        System.err.print("  XInclude:   ");
        System.err.println(DEFAULT_XINCLUDE ? "on" : "off");
        System.err.print("  XInclude base URI fixup:  ");
        System.err.println(DEFAULT_XINCLUDE_FIXUP_BASE_URIS ? "on" : "off");
        System.err.print("  XInclude language fixup:  ");
        System.err.println(DEFAULT_XINCLUDE_FIXUP_LANGUAGE ? "on" : "off");
    } 
} 
