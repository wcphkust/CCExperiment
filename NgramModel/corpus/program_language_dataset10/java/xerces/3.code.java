package dom;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.traversal.NodeFilter;
public class DOM3 implements DOMErrorHandler, LSParserFilter {
    protected static final boolean DEFAULT_NAMESPACES = true;
    protected static final boolean DEFAULT_VALIDATION = false;
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;
    static LSParser builder;
    public static void main( String[] argv) {
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }
        try {
            DOMImplementationRegistry registry =
                DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = 
                (DOMImplementationLS)registry.getDOMImplementation("LS");
            builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
            DOMConfiguration config = builder.getDomConfig();
            DOMErrorHandler errorHandler = new DOM3();
            LSParserFilter filter = new DOM3();
            builder.setFilter(filter);
            config.setParameter("error-handler", errorHandler);
            config.setParameter("validate",Boolean.TRUE);
            config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
            config.setParameter("schema-location","personal.xsd");
            System.out.println("Parsing "+argv[0]+"...");
            Document doc = builder.parseURI(argv[0]);
            config = doc.getDomConfig();
            config.setParameter("error-handler", errorHandler);
            config.setParameter("validate", Boolean.TRUE);
            config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
            config.setParameter("schema-location","data/personal.xsd");
            config.setParameter("comments", Boolean.FALSE);
            System.out.println("Normalizing document... ");
            doc.normalizeDocument();
            LSSerializer domWriter = impl.createLSSerializer();
            System.out.println("Serializing document... ");
            config = domWriter.getDomConfig();
            config.setParameter("xml-declaration", Boolean.FALSE);
            LSOutput dOut = impl.createLSOutput();
            dOut.setByteStream(System.out);
            domWriter.write(doc,dOut);
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }
    private static void printUsage() {
        System.err.println("usage: java dom.DOM3 uri ...");
        System.err.println();
        System.err.println("NOTE: You can only validate DOM tree against XML Schemas.");
    } 
    public boolean handleError(DOMError error){
        short severity = error.getSeverity();
        if (severity == DOMError.SEVERITY_ERROR) {
            System.out.println("[dom3-error]: "+error.getMessage());
        }
        if (severity == DOMError.SEVERITY_WARNING) {
            System.out.println("[dom3-warning]: "+error.getMessage());
        }
        return true;
    }
	public short acceptNode(Node enode) {
        return NodeFilter.FILTER_ACCEPT;
	}
	public int getWhatToShow() {
		return NodeFilter.SHOW_ELEMENT;
	}
	public short startElement(Element elt) {
		return LSParserFilter.FILTER_ACCEPT;
	}
}
