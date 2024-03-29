package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.XMLCatalog;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
public class XmlProperty extends org.apache.tools.ant.Task {
    private Resource src;
    private String prefix = "";
    private boolean keepRoot = true;
    private boolean validate = false;
    private boolean collapseAttributes = false;
    private boolean semanticAttributes = false;
    private boolean includeSemanticAttribute = false;
    private File rootDirectory = null;
    private Hashtable addedAttributes = new Hashtable();
    private XMLCatalog xmlCatalog = new XMLCatalog();
    private String delimiter = ",";
    private static final String ID = "id";
    private static final String REF_ID = "refid";
    private static final String LOCATION = "location";
    private static final String VALUE = "value";
    private static final String PATH = "path";
    private static final String PATHID = "pathid";
    private static final String[] ATTRIBUTES = new String[] {
            ID, REF_ID, LOCATION, VALUE, PATH, PATHID
    };
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    public XmlProperty() {
        super();
    }
    public void init() {
        super.init();
        xmlCatalog.setProject(getProject());
    }
    protected EntityResolver getEntityResolver() {
        return xmlCatalog;
    }
    public void execute() throws BuildException {
        Resource r = getResource();
        if (r == null) {
            throw new BuildException("XmlProperty task requires a source resource");
        }
        try {
            log("Loading " + src, Project.MSG_VERBOSE);
            if (r.isExists()) {
              DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
              factory.setValidating(validate);
              factory.setNamespaceAware(false);
              DocumentBuilder builder = factory.newDocumentBuilder();
              builder.setEntityResolver(getEntityResolver());
              Document document = null;
              FileProvider fp = (FileProvider) src.as(FileProvider.class);
              if (fp != null) {
                  document = builder.parse(fp.getFile());
              } else {
                  document = builder.parse(src.getInputStream());
              }
              Element topElement = document.getDocumentElement();
              addedAttributes = new Hashtable();
              if (keepRoot) {
                  addNodeRecursively(topElement, prefix, null);
              } else {
                  NodeList topChildren = topElement.getChildNodes();
                  int numChildren = topChildren.getLength();
                  for (int i = 0; i < numChildren; i++) {
                    addNodeRecursively(topChildren.item(i), prefix, null);
                  }
              }
            } else {
                log("Unable to find property resource: " + r, Project.MSG_VERBOSE);
            }
        } catch (SAXException sxe) {
            Exception x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            throw new BuildException("Failed to load " + src, x);
        } catch (ParserConfigurationException pce) {
            throw new BuildException(pce);
        } catch (IOException ioe) {
            throw new BuildException("Failed to load " + src, ioe);
        }
    }
    private void addNodeRecursively(Node node, String prefix, Object container) {
        String nodePrefix = prefix;
        if (node.getNodeType() != Node.TEXT_NODE) {
            if (prefix.trim().length() > 0) {
                nodePrefix += ".";
            }
            nodePrefix += node.getNodeName();
        }
        Object nodeObject = processNode(node, nodePrefix, container);
        if (node.hasChildNodes()) {
            NodeList nodeChildren = node.getChildNodes();
            int numChildren = nodeChildren.getLength();
            for (int i = 0; i < numChildren; i++) {
                addNodeRecursively(nodeChildren.item(i), nodePrefix, nodeObject);
            }
        }
    }
    void addNodeRecursively(org.w3c.dom.Node node, String prefix) {
        addNodeRecursively(node, prefix, null);
    }
    public Object processNode (Node node, String prefix, Object container) {
        Object addedPath = null;
        String id = null;
        if (node.hasAttributes()) {
            NamedNodeMap nodeAttributes = node.getAttributes();
            Node idNode = nodeAttributes.getNamedItem(ID);
            id = semanticAttributes && idNode != null ? idNode.getNodeValue() : null;
            for (int i = 0; i < nodeAttributes.getLength(); i++) {
                Node attributeNode = nodeAttributes.item(i);
                if (!semanticAttributes) {
                    String attributeName = getAttributeName(attributeNode);
                    String attributeValue = getAttributeValue(attributeNode);
                    addProperty(prefix + attributeName, attributeValue, null);
                } else {
                    String nodeName = attributeNode.getNodeName();
                    String attributeValue = getAttributeValue(attributeNode);
                    Path containingPath =
                        ((container != null) && (container instanceof Path))
                        ? (Path) container
                        : null;
                    if (nodeName.equals(ID)) {
                        continue;
                    }
                    if (containingPath != null && nodeName.equals(PATH)) {
                        containingPath.setPath(attributeValue);
                    } else if (container instanceof Path && nodeName.equals(REF_ID)) {
                        containingPath.setPath(attributeValue);
                    } else if (container instanceof Path && nodeName.equals(LOCATION)) {
                        containingPath.setLocation(resolveFile(attributeValue));
                    } else if (nodeName.equals(PATHID)) {
                        if (container != null) {
                            throw new BuildException("XmlProperty does not support nested paths");
                        }
                        addedPath = new Path(getProject());
                        getProject().addReference(attributeValue, addedPath);
                    } else {
                        String attributeName = getAttributeName(attributeNode);
                        addProperty(prefix + attributeName, attributeValue, id);
                    }
                }
            }
        }
        String nodeText = null;
        boolean emptyNode = false;
        boolean semanticEmptyOverride = false;
        if (node.getNodeType() == Node.ELEMENT_NODE
                && semanticAttributes
                && node.hasAttributes()
                && (node.getAttributes().getNamedItem(VALUE) != null
                        || node.getAttributes().getNamedItem(LOCATION) != null
                        || node.getAttributes().getNamedItem(REF_ID) != null
                        || node.getAttributes().getNamedItem(PATH) != null || node.getAttributes()
                        .getNamedItem(PATHID) != null)) {
            semanticEmptyOverride = true;
        }
        if (node.getNodeType() == Node.TEXT_NODE) {
            nodeText = getAttributeValue(node);
        } else if (node.getNodeType() == Node.ELEMENT_NODE
                && node.getChildNodes().getLength() == 1
                && node.getFirstChild().getNodeType() == Node.CDATA_SECTION_NODE) {
            nodeText = node.getFirstChild().getNodeValue();
            if ("".equals(nodeText) && !semanticEmptyOverride) {
                emptyNode = true;
            }
        } else if (node.getNodeType() == Node.ELEMENT_NODE
               && node.getChildNodes().getLength() == 0
               && !semanticEmptyOverride) {
            nodeText = "";
            emptyNode = true;
        } else if (node.getNodeType() == Node.ELEMENT_NODE
               && node.getChildNodes().getLength() == 1
               && node.getFirstChild().getNodeType() == Node.TEXT_NODE
               && "".equals(node.getFirstChild().getNodeValue())
               && !semanticEmptyOverride) {
            nodeText = "";
            emptyNode = true;
        }
        if (nodeText != null) {
            if (semanticAttributes && id == null && container instanceof String) {
                id = (String) container;
            }
            if (nodeText.trim().length() != 0 || emptyNode) {
                addProperty(prefix, nodeText, id);
            }
        }
        return (addedPath != null ? addedPath : id);
    }
    private void addProperty (String name, String value, String id) {
        String msg = name + ":" + value;
        if (id != null) {
            msg += ("(id=" + id + ")");
        }
        log(msg, Project.MSG_DEBUG);
        if (addedAttributes.containsKey(name)) {
            value = (String) addedAttributes.get(name) + getDelimiter() + value;
            getProject().setProperty(name, value);
            addedAttributes.put(name, value);
        } else if (getProject().getProperty(name) == null) {
            getProject().setNewProperty(name, value);
            addedAttributes.put(name, value);
        } else {
            log("Override ignored for property " + name, Project.MSG_VERBOSE);
        }
        if (id != null) {
            getProject().addReference(id, value);
        }
    }
    private String getAttributeName (Node attributeNode) {
        String attributeName = attributeNode.getNodeName();
        if (semanticAttributes) {
            if (attributeName.equals(REF_ID)) {
                return "";
            }
            if (!isSemanticAttribute(attributeName) || includeSemanticAttribute) {
                return "." + attributeName;
            }
            return "";
        }
        return collapseAttributes ? "." + attributeName : "(" + attributeName + ")";
    }
    private static boolean isSemanticAttribute (String attributeName) {
        for (int i = 0; i < ATTRIBUTES.length; i++) {
            if (attributeName.equals(ATTRIBUTES[i])) {
                return true;
            }
        }
        return false;
    }
    private String getAttributeValue (Node attributeNode) {
        String nodeValue = attributeNode.getNodeValue().trim();
        if (semanticAttributes) {
            String attributeName = attributeNode.getNodeName();
            nodeValue = getProject().replaceProperties(nodeValue);
            if (attributeName.equals(LOCATION)) {
                File f = resolveFile(nodeValue);
                return f.getPath();
            }
            if (attributeName.equals(REF_ID)) {
                Object ref = getProject().getReference(nodeValue);
                if (ref != null) {
                    return ref.toString();
                }
            }
        }
        return nodeValue;
    }
    public void setFile(File src) {
        setSrcResource(new FileResource(src));
    }
    public void setSrcResource(Resource src) {
        if (src.isDirectory()) {
            throw new BuildException("the source can't be a directory");
        }
        if (src.as(FileProvider.class) != null || supportsNonFileResources()) {
            this.src = src;
        } else {
            throw new BuildException("Only FileSystem resources are supported.");
        }
    }
    public void addConfigured(ResourceCollection a) {
        if (a.size() != 1) {
            throw new BuildException(
                    "only single argument resource collections are supported as archives");
        }
        setSrcResource((Resource) a.iterator().next());
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix.trim();
    }
    public void setKeeproot(boolean keepRoot) {
        this.keepRoot = keepRoot;
    }
    public void setValidate(boolean validate) {
        this.validate = validate;
    }
    public void setCollapseAttributes(boolean collapseAttributes) {
        this.collapseAttributes = collapseAttributes;
    }
    public void setSemanticAttributes(boolean semanticAttributes) {
        this.semanticAttributes = semanticAttributes;
    }
    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }
    public void setIncludeSemanticAttribute(boolean includeSemanticAttribute) {
        this.includeSemanticAttribute = includeSemanticAttribute;
    }
    public void addConfiguredXMLCatalog(XMLCatalog catalog) {
        xmlCatalog.addConfiguredXMLCatalog(catalog);
    }
    protected File getFile () {
        FileProvider fp = (FileProvider) src.as(FileProvider.class);
        return fp != null ? fp.getFile() : null;
    }
    protected Resource getResource() {
        File f = getFile();
        FileProvider fp = (FileProvider) src.as(FileProvider.class);
        return f == null ? src : fp != null
                && fp.getFile().equals(f) ? src : new FileResource(f);
    }
    protected String getPrefix () {
        return this.prefix;
    }
    protected boolean getKeeproot () {
        return this.keepRoot;
    }
    protected boolean getValidate () {
        return this.validate;
    }
    protected boolean getCollapseAttributes () {
        return this.collapseAttributes;
    }
    protected boolean getSemanticAttributes () {
        return this.semanticAttributes;
    }
    protected File getRootDirectory () {
        return this.rootDirectory;
    }
    protected boolean getIncludeSementicAttribute () {
        return this.includeSemanticAttribute;
    }
    private File resolveFile(String fileName) {
        return FILE_UTILS.resolveFile(rootDirectory == null ? getProject().getBaseDir()
                : rootDirectory, fileName);
    }
    protected boolean supportsNonFileResources() {
        return getClass().equals(XmlProperty.class);
    }
    public String getDelimiter() {
        return delimiter;
    }
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
}
