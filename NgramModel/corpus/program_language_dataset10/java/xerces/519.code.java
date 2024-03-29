package org.apache.xerces.jaxp;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;
class TeeXMLDocumentFilterImpl implements XMLDocumentFilter {
    private XMLDocumentHandler next;
    private XMLDocumentHandler side;
    private XMLDocumentSource source;
    public XMLDocumentHandler getSide() {
        return side;
    }
    public void setSide(XMLDocumentHandler side) {
        this.side = side;
    }
    public XMLDocumentSource getDocumentSource() {
        return source;
    }
    public void setDocumentSource(XMLDocumentSource source) {
        this.source = source;
    }
    public XMLDocumentHandler getDocumentHandler() {
        return next;
    }
    public void setDocumentHandler(XMLDocumentHandler handler) {
        next = handler;
    }
    public void characters(XMLString text, Augmentations augs) throws XNIException {
        side.characters(text, augs);
        next.characters(text, augs);
    }
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        side.comment(text, augs);
        next.comment(text, augs);
    }
    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs)
        throws XNIException {
        side.doctypeDecl(rootElement, publicId, systemId, augs);
        next.doctypeDecl(rootElement, publicId, systemId, augs);
    }
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        side.emptyElement(element, attributes, augs);
        next.emptyElement(element, attributes, augs);
    }
    public void endCDATA(Augmentations augs) throws XNIException {
        side.endCDATA(augs);
        next.endCDATA(augs);
    }
    public void endDocument(Augmentations augs) throws XNIException {
        side.endDocument(augs);
        next.endDocument(augs);
    }
    public void endElement(QName element, Augmentations augs) throws XNIException {
        side.endElement(element, augs);
        next.endElement(element, augs);
    }
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        side.endGeneralEntity(name, augs);
        next.endGeneralEntity(name, augs);
    }
    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        side.ignorableWhitespace(text, augs);
        next.ignorableWhitespace(text, augs);
    }
    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        side.processingInstruction(target, data, augs);
        next.processingInstruction(target, data, augs);
    }
    public void startCDATA(Augmentations augs) throws XNIException {
        side.startCDATA(augs);
        next.startCDATA(augs);
    }
    public void startDocument(
            XMLLocator locator,
            String encoding,
            NamespaceContext namespaceContext,
            Augmentations augs)
        throws XNIException {
        side.startDocument(locator, encoding, namespaceContext, augs);
        next.startDocument(locator, encoding, namespaceContext, augs);
    }
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        side.startElement(element, attributes, augs);
        next.startElement(element, attributes, augs);
    }
    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs)
        throws XNIException {
        side.startGeneralEntity(name, identifier, encoding, augs);
        next.startGeneralEntity(name, identifier, encoding, augs);
    }
    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        side.textDecl(version, encoding, augs);
        next.textDecl(version, encoding, augs);
    }
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        side.xmlDecl(version, encoding, standalone, augs);
        next.xmlDecl(version, encoding, standalone, augs);
    }
}
