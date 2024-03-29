package org.apache.xalan.xsltc.trax;
import java.io.IOException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.apache.xml.serializer.SerializationHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.apache.xml.serializer.NamespaceMappings;
public class DOM2TO implements XMLReader, Locator {
    private final static String EMPTYSTRING = "";
    private static final String XMLNS_PREFIX = "xmlns";
    private Node _dom;
    private SerializationHandler _handler;
    public DOM2TO(Node root, SerializationHandler handler) {
	_dom = root;
	_handler = handler;
    }
    public ContentHandler getContentHandler() { 
	return null;
    }
    public void setContentHandler(ContentHandler handler) {
    }
    public void parse(InputSource unused) throws IOException, SAXException {
        parse(_dom);
    }
    public void parse() throws IOException, SAXException {
	if (_dom != null) {
	    boolean isIncomplete = 
		(_dom.getNodeType() != org.w3c.dom.Node.DOCUMENT_NODE);
	    if (isIncomplete) {
		_handler.startDocument();
		parse(_dom);
		_handler.endDocument();
	    }
	    else {
		parse(_dom);
	    }
	}
    }
    private void parse(Node node) 
	throws IOException, SAXException 
    {
 	if (node == null) return;
        switch (node.getNodeType()) {
	case Node.ATTRIBUTE_NODE:         
	case Node.DOCUMENT_TYPE_NODE :
	case Node.ENTITY_NODE :
	case Node.ENTITY_REFERENCE_NODE:
	case Node.NOTATION_NODE :
	    break;
	case Node.CDATA_SECTION_NODE:
	    _handler.startCDATA();
	    _handler.characters(node.getNodeValue());
	    _handler.endCDATA();
	    break;
	case Node.COMMENT_NODE:           
	    _handler.comment(node.getNodeValue());
	    break;
	case Node.DOCUMENT_NODE:
	    _handler.startDocument();
	    Node next = node.getFirstChild();
	    while (next != null) {
		parse(next);
		next = next.getNextSibling();
	    }
	    _handler.endDocument();
	    break;
	case Node.DOCUMENT_FRAGMENT_NODE:
	    next = node.getFirstChild();
	    while (next != null) {
		parse(next);
		next = next.getNextSibling();
	    }
	    break;
	case Node.ELEMENT_NODE:
	    final String qname = node.getNodeName();
	    _handler.startElement(null, null, qname);
            int colon;
	    String prefix;
	    final NamedNodeMap map = node.getAttributes();
	    final int length = map.getLength();
	    for (int i = 0; i < length; i++) {
		final Node attr = map.item(i);
		final String qnameAttr = attr.getNodeName();
		if (qnameAttr.startsWith(XMLNS_PREFIX)) {
		    final String uriAttr = attr.getNodeValue();
		    colon = qnameAttr.lastIndexOf(':');
		    prefix = (colon > 0) ? qnameAttr.substring(colon + 1) 
			                 : EMPTYSTRING;
		    _handler.namespaceAfterStartElement(prefix, uriAttr);
		}
	    }
            NamespaceMappings nm = new NamespaceMappings();
	    for (int i = 0; i < length; i++) {
		final Node attr = map.item(i);
		final String qnameAttr = attr.getNodeName();
		if (!qnameAttr.startsWith(XMLNS_PREFIX)) {
		    final String uriAttr = attr.getNamespaceURI();
		    if (uriAttr != null && !uriAttr.equals(EMPTYSTRING) ) {	
			colon = qnameAttr.lastIndexOf(':');
                        String newPrefix = nm.lookupPrefix(uriAttr);
                        if (newPrefix == null) 
                            newPrefix = nm.generateNextPrefix();
			prefix = (colon > 0) ? qnameAttr.substring(0, colon) 
			    : newPrefix;
			_handler.namespaceAfterStartElement(prefix, uriAttr);
		        _handler.addAttribute((prefix + ":" + qnameAttr),
                            attr.getNodeValue());
		    } else {
                         _handler.addAttribute(qnameAttr, attr.getNodeValue());
                    }
                }
	    }
	    final String uri = node.getNamespaceURI();
            final String localName = node.getLocalName();
	    if (uri != null) {	
		colon = qname.lastIndexOf(':');
		prefix = (colon > 0) ? qname.substring(0, colon) : EMPTYSTRING;
		_handler.namespaceAfterStartElement(prefix, uri);
	    }else {
                  if (uri == null  && localName != null) {
 		     prefix = EMPTYSTRING;
		     _handler.namespaceAfterStartElement(prefix, EMPTYSTRING);
                 }
            }
	    next = node.getFirstChild();
	    while (next != null) {
		parse(next);
		next = next.getNextSibling();
	    }
	    _handler.endElement(qname);
	    break;
	case Node.PROCESSING_INSTRUCTION_NODE:
	    _handler.processingInstruction(node.getNodeName(),
					   node.getNodeValue());
	    break;
	case Node.TEXT_NODE:
	    _handler.characters(node.getNodeValue());
	    break;
	}
    }
    public DTDHandler getDTDHandler() { 
	return null;
    }
    public ErrorHandler getErrorHandler() {
	return null;
    }
    public boolean getFeature(String name) throws SAXNotRecognizedException,
	SAXNotSupportedException
    {
	return false;
    }
    public void setFeature(String name, boolean value) throws 
	SAXNotRecognizedException, SAXNotSupportedException 
    {
    }
    public void parse(String sysId) throws IOException, SAXException {
	throw new IOException("This method is not yet implemented.");
    }
    public void setDTDHandler(DTDHandler handler) throws NullPointerException {
    }
    public void setEntityResolver(EntityResolver resolver) throws 
	NullPointerException 
    {
    }
    public EntityResolver getEntityResolver() {
	return null;
    }
    public void setErrorHandler(ErrorHandler handler) throws 
	NullPointerException
    {
    }
    public void setProperty(String name, Object value) throws
	SAXNotRecognizedException, SAXNotSupportedException {
    }
    public Object getProperty(String name) throws SAXNotRecognizedException,
	SAXNotSupportedException
    {
	return null;
    }
    public int getColumnNumber() { 
	return 0; 
    }
    public int getLineNumber() { 
	return 0; 
    }
    public String getPublicId() { 
	return null; 
    }
    public String getSystemId() { 
	return null; 
    }
    private String getNodeTypeFromCode(short code) {
	String retval = null;
	switch (code) {
	case Node.ATTRIBUTE_NODE : 
	    retval = "ATTRIBUTE_NODE"; break; 
	case Node.CDATA_SECTION_NODE :
	    retval = "CDATA_SECTION_NODE"; break; 
	case Node.COMMENT_NODE :
	    retval = "COMMENT_NODE"; break; 
	case Node.DOCUMENT_FRAGMENT_NODE :
	    retval = "DOCUMENT_FRAGMENT_NODE"; break; 
	case Node.DOCUMENT_NODE :
	    retval = "DOCUMENT_NODE"; break; 
	case Node.DOCUMENT_TYPE_NODE :
	    retval = "DOCUMENT_TYPE_NODE"; break; 
	case Node.ELEMENT_NODE :
	    retval = "ELEMENT_NODE"; break; 
	case Node.ENTITY_NODE :
	    retval = "ENTITY_NODE"; break; 
	case Node.ENTITY_REFERENCE_NODE :
	    retval = "ENTITY_REFERENCE_NODE"; break; 
	case Node.NOTATION_NODE :
	    retval = "NOTATION_NODE"; break; 
	case Node.PROCESSING_INSTRUCTION_NODE :
	    retval = "PROCESSING_INSTRUCTION_NODE"; break; 
	case Node.TEXT_NODE:
	    retval = "TEXT_NODE"; break; 
        }
	return retval;
    }
}
