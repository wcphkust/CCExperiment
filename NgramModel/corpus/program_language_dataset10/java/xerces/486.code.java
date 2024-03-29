package org.apache.xerces.impl.xs.traversers;
import java.util.Locale;
import java.util.Vector;
import org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import org.apache.xerces.impl.dv.XSFacets;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.validation.ValidationState;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.impl.xs.SchemaSymbols;
import org.apache.xerces.impl.xs.XSAnnotationImpl;
import org.apache.xerces.impl.xs.XSAttributeGroupDecl;
import org.apache.xerces.impl.xs.XSAttributeUseImpl;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.impl.xs.XSParticleDecl;
import org.apache.xerces.impl.xs.XSWildcardDecl;
import org.apache.xerces.impl.xs.util.XInt;
import org.apache.xerces.impl.xs.util.XSObjectListImpl;
import org.apache.xerces.util.DOMUtil;
import org.apache.xerces.util.NamespaceSupport;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSTypeDefinition;
import org.w3c.dom.Element;
abstract class XSDAbstractTraverser {
    protected static final String NO_NAME      = "(no name)";
    protected static final int NOT_ALL_CONTEXT    = 0;
    protected static final int PROCESSING_ALL_EL  = 1;
    protected static final int GROUP_REF_WITH_ALL = 2;
    protected static final int CHILD_OF_GROUP     = 4;
    protected static final int PROCESSING_ALL_GP  = 8;
    protected XSDHandler            fSchemaHandler = null;
    protected SymbolTable           fSymbolTable = null;
    protected XSAttributeChecker    fAttrChecker = null;
    protected boolean               fValidateAnnotations = false;
    ValidationState fValidationState = new ValidationState();
    XSDAbstractTraverser (XSDHandler handler,
            XSAttributeChecker attrChecker) {
        fSchemaHandler = handler;
        fAttrChecker = attrChecker;
    }
    void reset(SymbolTable symbolTable, boolean validateAnnotations, Locale locale) {
        fSymbolTable = symbolTable;
        fValidateAnnotations = validateAnnotations;
        fValidationState.setExtraChecking(false);
        fValidationState.setSymbolTable(symbolTable);
        fValidationState.setLocale(locale);
    }
    XSAnnotationImpl traverseAnnotationDecl(Element annotationDecl, Object[] parentAttrs,
            boolean isGlobal, XSDocumentInfo schemaDoc) {
        Object[] attrValues = fAttrChecker.checkAttributes(annotationDecl, isGlobal, schemaDoc);
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        String contents = DOMUtil.getAnnotation(annotationDecl);
        Element child = DOMUtil.getFirstChildElement(annotationDecl);
        if (child != null) {
            do {
                String name = DOMUtil.getLocalName(child);
                if (!((name.equals(SchemaSymbols.ELT_APPINFO)) ||
                        (name.equals(SchemaSymbols.ELT_DOCUMENTATION)))) {
                    reportSchemaError("src-annotation", new Object[]{name}, child);
                }
                else {
                    attrValues = fAttrChecker.checkAttributes(child, true, schemaDoc);
                    fAttrChecker.returnAttrArray(attrValues, schemaDoc);
                }
                child = DOMUtil.getNextSiblingElement(child);
            }
            while (child != null);
        }
        if (contents == null) return null;
        SchemaGrammar grammar = fSchemaHandler.getGrammar(schemaDoc.fTargetNamespace);
        Vector annotationLocalAttrs = (Vector)parentAttrs[XSAttributeChecker.ATTIDX_NONSCHEMA];
        if(annotationLocalAttrs != null && !annotationLocalAttrs.isEmpty()) {
            StringBuffer localStrBuffer = new StringBuffer(64);
            localStrBuffer.append(" ");
            int i = 0;
            while (i < annotationLocalAttrs.size()) {
                String rawname = (String)annotationLocalAttrs.elementAt(i++);
                int colonIndex = rawname.indexOf(':');
                String prefix, localpart;
                if (colonIndex == -1) {
                    prefix = "";
                    localpart = rawname;
                }
                else {
                    prefix = rawname.substring(0,colonIndex);
                    localpart = rawname.substring(colonIndex+1);
                }
                String uri = schemaDoc.fNamespaceSupport.getURI(fSymbolTable.addSymbol(prefix));
                if (annotationDecl.getAttributeNS(uri, localpart).length() != 0) {
                    i++; 
                    continue;
                }
                localStrBuffer.append(rawname)
                .append("=\"");
                String value = (String)annotationLocalAttrs.elementAt(i++);
                value = processAttValue(value);
                localStrBuffer.append(value)
                .append("\" ");
            }
            StringBuffer contentBuffer = new StringBuffer(contents.length() + localStrBuffer.length());
            int annotationTokenEnd = contents.indexOf(SchemaSymbols.ELT_ANNOTATION);
            if(annotationTokenEnd == -1) return null;
            annotationTokenEnd += SchemaSymbols.ELT_ANNOTATION.length();
            contentBuffer.append(contents.substring(0,annotationTokenEnd));
            contentBuffer.append(localStrBuffer.toString());
            contentBuffer.append(contents.substring(annotationTokenEnd, contents.length()));
            final String annotation = contentBuffer.toString();
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(annotation, annotationDecl));
            }
            return new XSAnnotationImpl(annotation, grammar);
        } else {
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(contents, annotationDecl));
            }
            return new XSAnnotationImpl(contents, grammar);
        }
    }
    XSAnnotationImpl traverseSyntheticAnnotation(Element annotationParent, String initialContent,
            Object[] parentAttrs, boolean isGlobal, XSDocumentInfo schemaDoc) {
        String contents = initialContent;
        SchemaGrammar grammar = fSchemaHandler.getGrammar(schemaDoc.fTargetNamespace);
        Vector annotationLocalAttrs = (Vector)parentAttrs[XSAttributeChecker.ATTIDX_NONSCHEMA];
        if (annotationLocalAttrs != null && !annotationLocalAttrs.isEmpty()) {
            StringBuffer localStrBuffer = new StringBuffer(64);
            localStrBuffer.append(" ");
            int i = 0;
            while (i < annotationLocalAttrs.size()) {
                String rawname = (String)annotationLocalAttrs.elementAt(i++);
                int colonIndex = rawname.indexOf(':');
                String prefix, localpart;
                if (colonIndex == -1) {
                    prefix = "";
                    localpart = rawname;
                }
                else {
                    prefix = rawname.substring(0,colonIndex);
                    localpart = rawname.substring(colonIndex+1);
                }
                String uri = schemaDoc.fNamespaceSupport.getURI(fSymbolTable.addSymbol(prefix));
                localStrBuffer.append(rawname)
                .append("=\"");
                String value = (String)annotationLocalAttrs.elementAt(i++);
                value = processAttValue(value);
                localStrBuffer.append(value)
                .append("\" ");
            }
            StringBuffer contentBuffer = new StringBuffer(contents.length() + localStrBuffer.length());
            int annotationTokenEnd = contents.indexOf(SchemaSymbols.ELT_ANNOTATION);
            if(annotationTokenEnd == -1) return null;
            annotationTokenEnd += SchemaSymbols.ELT_ANNOTATION.length();
            contentBuffer.append(contents.substring(0,annotationTokenEnd));
            contentBuffer.append(localStrBuffer.toString());
            contentBuffer.append(contents.substring(annotationTokenEnd, contents.length()));
            final String annotation = contentBuffer.toString();
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(annotation, annotationParent));
            }
            return new XSAnnotationImpl(annotation, grammar);
        } else {
            if (fValidateAnnotations) {
                schemaDoc.addAnnotation(new XSAnnotationInfo(contents, annotationParent));
            }
            return new XSAnnotationImpl(contents, grammar);
        }
    }
    private static final XSSimpleType fQNameDV = (XSSimpleType)SchemaGrammar.SG_SchemaNS.getGlobalTypeDecl(SchemaSymbols.ATTVAL_QNAME);
    private StringBuffer fPattern = new StringBuffer();
    private final XSFacets xsFacets = new XSFacets();
    static final class FacetInfo {
        final XSFacets facetdata;
        final Element nodeAfterFacets;
        final short fPresentFacets;
        final short fFixedFacets;
        FacetInfo(XSFacets facets, Element nodeAfterFacets, short presentFacets, short fixedFacets) {
            facetdata = facets;
            this.nodeAfterFacets = nodeAfterFacets;
            fPresentFacets = presentFacets;
            fFixedFacets = fixedFacets;
        }
    }
    FacetInfo traverseFacets(Element content,
            XSSimpleType baseValidator,
            XSDocumentInfo schemaDoc) {
        short facetsPresent = 0 ;
        short facetsFixed = 0; 
        String facet;
        boolean hasQName = containsQName(baseValidator);
        Vector enumData = null;
        XSObjectListImpl enumAnnotations = null;
        XSObjectListImpl patternAnnotations = null;
        Vector enumNSDecls = hasQName ? new Vector() : null;       
        int currentFacet = 0;
        xsFacets.reset();
        while (content != null) {           
            Object[] attrs = null;
            facet = DOMUtil.getLocalName(content);
            if (facet.equals(SchemaSymbols.ELT_ENUMERATION)) {
                attrs = fAttrChecker.checkAttributes(content, false, schemaDoc, hasQName);
                String enumVal = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                if (enumVal == null) {
                    reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_ENUMERATION, SchemaSymbols.ATT_VALUE}, content);
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }
                NamespaceSupport nsDecls = (NamespaceSupport)attrs[XSAttributeChecker.ATTIDX_ENUMNSDECLS];
                if (baseValidator.getVariety() == XSSimpleType.VARIETY_ATOMIC &&
                        baseValidator.getPrimitiveKind() == XSSimpleType.PRIMITIVE_NOTATION) {
                    schemaDoc.fValidationContext.setNamespaceSupport(nsDecls);
                    Object notation = null;
                    try{
                        QName temp = (QName)fQNameDV.validate(enumVal, schemaDoc.fValidationContext, null);
                        notation = fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.NOTATION_TYPE, temp, content);
                    }catch(InvalidDatatypeValueException ex){
                        reportSchemaError(ex.getKey(), ex.getArgs(), content);
                    }
                    if (notation == null) {
                        fAttrChecker.returnAttrArray (attrs, schemaDoc);
                        content = DOMUtil.getNextSiblingElement(content);
                        continue;
                    }
                    schemaDoc.fValidationContext.setNamespaceSupport(schemaDoc.fNamespaceSupport);
                }
                if (enumData == null){
                    enumData = new Vector();
                    enumAnnotations = new XSObjectListImpl();
                }
                enumData.addElement(enumVal);
                enumAnnotations.addXSObject(null);
                if (hasQName)
                    enumNSDecls.addElement(nsDecls);
                Element child = DOMUtil.getFirstChildElement( content );
                if (child != null &&
                    DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    enumAnnotations.addXSObject(enumAnnotations.getLength()-1,traverseAnnotationDecl(child, attrs, false, schemaDoc));
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(content);
                    if (text != null) {
                        enumAnnotations.addXSObject(enumAnnotations.getLength()-1, traverseSyntheticAnnotation(content, text, attrs, false, schemaDoc));
                    }
                }
                if (child !=null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{"enumeration", "(annotation?)", DOMUtil.getLocalName(child)}, child);
                }
            }
            else if (facet.equals(SchemaSymbols.ELT_PATTERN)) {
                attrs = fAttrChecker.checkAttributes(content, false, schemaDoc);
                String patternVal = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                if (patternVal == null) {
                    reportSchemaError("s4s-att-must-appear", new Object[]{SchemaSymbols.ELT_PATTERN, SchemaSymbols.ATT_VALUE}, content);
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }
                if (fPattern.length() == 0) {
                    fPattern.append(patternVal);
                } else {
                    fPattern.append("|");
                    fPattern.append(patternVal);
                }
                Element child = DOMUtil.getFirstChildElement( content );
                if (child != null &&
                        DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    if (patternAnnotations == null){
                        patternAnnotations = new XSObjectListImpl();
                    }
                    patternAnnotations.addXSObject(traverseAnnotationDecl(child, attrs, false, schemaDoc));
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(content);
                    if (text != null) {
                        if (patternAnnotations == null){
                            patternAnnotations = new XSObjectListImpl();
                        }
                        patternAnnotations.addXSObject(traverseSyntheticAnnotation(content, text, attrs, false, schemaDoc));
                    }
                }
                if (child !=null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{"pattern", "(annotation?)", DOMUtil.getLocalName(child)}, child);
                }
            }
            else {
                if (facet.equals(SchemaSymbols.ELT_MINLENGTH)) {
                    currentFacet = XSSimpleType.FACET_MINLENGTH;
                }
                else if (facet.equals(SchemaSymbols.ELT_MAXLENGTH)) {
                    currentFacet = XSSimpleType.FACET_MAXLENGTH;
                }
                else if (facet.equals(SchemaSymbols.ELT_MAXEXCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MAXEXCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_MAXINCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MAXINCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_MINEXCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MINEXCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_MININCLUSIVE)) {
                    currentFacet = XSSimpleType.FACET_MININCLUSIVE;
                }
                else if (facet.equals(SchemaSymbols.ELT_TOTALDIGITS)) {
                    currentFacet = XSSimpleType.FACET_TOTALDIGITS;
                }
                else if (facet.equals(SchemaSymbols.ELT_FRACTIONDIGITS)) {
                    currentFacet = XSSimpleType.FACET_FRACTIONDIGITS;
                }
                else if (facet.equals(SchemaSymbols.ELT_WHITESPACE)) {
                    currentFacet = XSSimpleType.FACET_WHITESPACE;
                }
                else if (facet.equals(SchemaSymbols.ELT_LENGTH)) {
                    currentFacet = XSSimpleType.FACET_LENGTH;
                }
                else {
                    break;   
                }
                attrs = fAttrChecker.checkAttributes(content, false, schemaDoc);
                if ((facetsPresent & currentFacet) != 0) {
                    reportSchemaError("src-single-facet-value", new Object[]{facet}, content);
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }
                if (attrs[XSAttributeChecker.ATTIDX_VALUE] == null) {
                    if (content.getAttributeNodeNS(null, "value") == null) {
                        reportSchemaError("s4s-att-must-appear", new Object[]{content.getLocalName(), SchemaSymbols.ATT_VALUE}, content);
                    }
                    fAttrChecker.returnAttrArray (attrs, schemaDoc);
                    content = DOMUtil.getNextSiblingElement(content);
                    continue;
                }
                facetsPresent |= currentFacet;
                if (((Boolean)attrs[XSAttributeChecker.ATTIDX_FIXED]).booleanValue()) {
                    facetsFixed |= currentFacet;
                }
                switch (currentFacet) {
                case XSSimpleType.FACET_MINLENGTH:
                    xsFacets.minLength = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                case XSSimpleType.FACET_MAXLENGTH:
                    xsFacets.maxLength = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                case XSSimpleType.FACET_MAXEXCLUSIVE:
                    xsFacets.maxExclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_MAXINCLUSIVE:
                    xsFacets.maxInclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_MINEXCLUSIVE:
                    xsFacets.minExclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_MININCLUSIVE:
                    xsFacets.minInclusive = (String)attrs[XSAttributeChecker.ATTIDX_VALUE];
                    break;
                case XSSimpleType.FACET_TOTALDIGITS:
                    xsFacets.totalDigits = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                case XSSimpleType.FACET_FRACTIONDIGITS:
                    xsFacets.fractionDigits = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                case XSSimpleType.FACET_WHITESPACE:
                    xsFacets.whiteSpace = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).shortValue();
                    break;
                case XSSimpleType.FACET_LENGTH:
                    xsFacets.length = ((XInt)attrs[XSAttributeChecker.ATTIDX_VALUE]).intValue();
                    break;
                }
                Element child = DOMUtil.getFirstChildElement( content );
                XSAnnotationImpl annotation = null;
                if (child != null &&
                    DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    annotation = traverseAnnotationDecl(child, attrs, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                }
                else {
                    String text = DOMUtil.getSyntheticAnnotation(content);
                    if (text != null) {
                        annotation = traverseSyntheticAnnotation(content, text, attrs, false, schemaDoc);
                    }
               }
                switch (currentFacet) {
                case XSSimpleType.FACET_MINLENGTH:
                    xsFacets.minLengthAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MAXLENGTH:
                    xsFacets.maxLengthAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MAXEXCLUSIVE:
                    xsFacets.maxExclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MAXINCLUSIVE:
                    xsFacets.maxInclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MINEXCLUSIVE:
                    xsFacets.minExclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_MININCLUSIVE:
                    xsFacets.minInclusiveAnnotation = annotation;
                break;
                case XSSimpleType.FACET_TOTALDIGITS:
                    xsFacets.totalDigitsAnnotation = annotation;
                break;
                case XSSimpleType.FACET_FRACTIONDIGITS:
                    xsFacets.fractionDigitsAnnotation = annotation;
                break;
                case XSSimpleType.FACET_WHITESPACE:
                    xsFacets.whiteSpaceAnnotation = annotation;
                break;
                case XSSimpleType.FACET_LENGTH:
                    xsFacets.lengthAnnotation = annotation;
                break;
                }
                if (child != null) {
                    reportSchemaError("s4s-elt-must-match.1", new Object[]{facet, "(annotation?)", DOMUtil.getLocalName(child)}, child);
                }
            }
            fAttrChecker.returnAttrArray (attrs, schemaDoc);
            content = DOMUtil.getNextSiblingElement(content);
        }
        if (enumData !=null) {
            facetsPresent |= XSSimpleType.FACET_ENUMERATION;
            xsFacets.enumeration = enumData;
            xsFacets.enumNSDecls = enumNSDecls;
            xsFacets.enumAnnotations = enumAnnotations;
        }
        if (fPattern.length() != 0) {
            facetsPresent |= XSSimpleType.FACET_PATTERN;
            xsFacets.pattern = fPattern.toString();
            xsFacets.patternAnnotations = patternAnnotations;
        }
        fPattern.setLength(0);
        return new FacetInfo(xsFacets, content, facetsPresent, facetsFixed);
    }
    private boolean containsQName(XSSimpleType type) {
        if (type.getVariety() == XSSimpleType.VARIETY_ATOMIC) {
            short primitive = type.getPrimitiveKind();
            return (primitive == XSSimpleType.PRIMITIVE_QNAME ||
                    primitive == XSSimpleType.PRIMITIVE_NOTATION);
        }
        else if (type.getVariety() == XSSimpleType.VARIETY_LIST) {
            return containsQName((XSSimpleType)type.getItemType());
        }
        else if (type.getVariety() == XSSimpleType.VARIETY_UNION) {
            XSObjectList members = type.getMemberTypes();
            for (int i = 0; i < members.getLength(); i++) {
                if (containsQName((XSSimpleType)members.item(i)))
                    return true;
            }
        }
        return false;
    }
    Element traverseAttrsAndAttrGrps(Element firstAttr, XSAttributeGroupDecl attrGrp,
            XSDocumentInfo schemaDoc, SchemaGrammar grammar,
            XSComplexTypeDecl enclosingCT) {
        Element child=null;
        XSAttributeGroupDecl tempAttrGrp = null;
        XSAttributeUseImpl tempAttrUse = null;
        XSAttributeUse otherUse = null;
        String childName;
        for (child=firstAttr; child!=null; child=DOMUtil.getNextSiblingElement(child)) {
            childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ATTRIBUTE)) {
                tempAttrUse = fSchemaHandler.fAttributeTraverser.traverseLocal(child,
                        schemaDoc,
                        grammar,
                        enclosingCT);
                if (tempAttrUse == null) continue;
                if (tempAttrUse.fUse == SchemaSymbols.USE_PROHIBITED) {
                    attrGrp.addAttributeUse(tempAttrUse);
                    continue;
                }
                otherUse = attrGrp.getAttributeUseNoProhibited(
                        tempAttrUse.fAttrDecl.getNamespace(),
                        tempAttrUse.fAttrDecl.getName());
                if (otherUse==null) {
                    String idName = attrGrp.addAttributeUse(tempAttrUse);
                    if (idName != null) {
                        String code = (enclosingCT == null) ? "ag-props-correct.3" : "ct-props-correct.5";
                        String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                        reportSchemaError(code, new Object[]{name, tempAttrUse.fAttrDecl.getName(), idName}, child);
                    }
                }
                else if (otherUse != tempAttrUse) {
                    String code = (enclosingCT == null) ? "ag-props-correct.2" : "ct-props-correct.4";
                    String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                    reportSchemaError(code, new Object[]{name, tempAttrUse.fAttrDecl.getName()}, child);
                }
            }
            else if (childName.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                tempAttrGrp = fSchemaHandler.fAttributeGroupTraverser.traverseLocal(
                        child, schemaDoc, grammar);
                if(tempAttrGrp == null ) continue;
                XSObjectList attrUseS = tempAttrGrp.getAttributeUses();
                XSAttributeUseImpl oneAttrUse;
                int attrCount = attrUseS.getLength();
                for (int i=0; i<attrCount; i++) {
                    oneAttrUse = (XSAttributeUseImpl)attrUseS.item(i);
                    if (oneAttrUse.fUse == SchemaSymbols.USE_PROHIBITED) {
                        attrGrp.addAttributeUse(oneAttrUse);
                        continue;
                    }
                    otherUse = attrGrp.getAttributeUseNoProhibited(
                            oneAttrUse.fAttrDecl.getNamespace(),
                            oneAttrUse.fAttrDecl.getName());
                    if (otherUse==null) {
                        String idName = attrGrp.addAttributeUse(oneAttrUse);
                        if (idName != null) {
                            String code = (enclosingCT == null) ? "ag-props-correct.3" : "ct-props-correct.5";
                            String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                            reportSchemaError(code, new Object[]{name, oneAttrUse.fAttrDecl.getName(), idName}, child);
                        }
                    }
                    else if (oneAttrUse != otherUse) {
                        String code = (enclosingCT == null) ? "ag-props-correct.2" : "ct-props-correct.4";
                        String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                        reportSchemaError(code, new Object[]{name, oneAttrUse.fAttrDecl.getName()}, child);
                    }
                }
                if (tempAttrGrp.fAttributeWC != null) {
                    if (attrGrp.fAttributeWC == null) {
                        attrGrp.fAttributeWC = tempAttrGrp.fAttributeWC;
                    }
                    else {
                        attrGrp.fAttributeWC = attrGrp.fAttributeWC.
                        performIntersectionWith(tempAttrGrp.fAttributeWC, attrGrp.fAttributeWC.fProcessContents);
                        if (attrGrp.fAttributeWC == null) {
                            String code = (enclosingCT == null) ? "src-attribute_group.2" : "src-ct.4";
                            String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                            reportSchemaError(code, new Object[]{name}, child);
                        }
                    }
                }
            }
            else
                break;
        } 
        if (child != null) {
            childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ANYATTRIBUTE)) {
                XSWildcardDecl tempAttrWC = fSchemaHandler.fWildCardTraverser.
                traverseAnyAttribute(child, schemaDoc, grammar);
                if (attrGrp.fAttributeWC == null) {
                    attrGrp.fAttributeWC = tempAttrWC;
                }
                else {
                    attrGrp.fAttributeWC = tempAttrWC.
                    performIntersectionWith(attrGrp.fAttributeWC, tempAttrWC.fProcessContents);
                    if (attrGrp.fAttributeWC == null) {
                        String code = (enclosingCT == null) ? "src-attribute_group.2" : "src-ct.4";
                        String name = (enclosingCT == null) ? attrGrp.fName : enclosingCT.getName();
                        reportSchemaError(code, new Object[]{name}, child);
                    }
                }
                child = DOMUtil.getNextSiblingElement(child);
            }
        }
        return child;
    }
    void reportSchemaError (String key, Object[] args, Element ele) {
        fSchemaHandler.reportSchemaError(key, args, ele);
    }
    void checkNotationType(String refName, XSTypeDefinition typeDecl, Element elem) {
        if (typeDecl.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE &&
                ((XSSimpleType)typeDecl).getVariety() == XSSimpleType.VARIETY_ATOMIC &&
                ((XSSimpleType)typeDecl).getPrimitiveKind() == XSSimpleType.PRIMITIVE_NOTATION) {
            if ((((XSSimpleType)typeDecl).getDefinedFacets() & XSSimpleType.FACET_ENUMERATION) == 0) {
                reportSchemaError("enumeration-required-notation", new Object[]{typeDecl.getName(), refName, DOMUtil.getLocalName(elem)}, elem);
            }
        }
    }
    protected XSParticleDecl checkOccurrences(XSParticleDecl particle,
            String particleName, Element parent,
            int allContextFlags,
            long defaultVals) {
        int min = particle.fMinOccurs;
        int max = particle.fMaxOccurs;
        boolean defaultMin = (defaultVals & (1 << XSAttributeChecker.ATTIDX_MINOCCURS)) != 0;
        boolean defaultMax = (defaultVals & (1 << XSAttributeChecker.ATTIDX_MAXOCCURS)) != 0;
        boolean processingAllEl = ((allContextFlags & PROCESSING_ALL_EL) != 0);
        boolean processingAllGP = ((allContextFlags & PROCESSING_ALL_GP) != 0);
        boolean groupRefWithAll = ((allContextFlags & GROUP_REF_WITH_ALL) != 0);
        boolean isGroupChild    = ((allContextFlags & CHILD_OF_GROUP) != 0);
        if (isGroupChild) {
            if (!defaultMin) {
                Object[] args = new Object[]{particleName, "minOccurs"};
                reportSchemaError("s4s-att-not-allowed", args, parent);
                min = 1;
            }
            if (!defaultMax) {
                Object[] args = new Object[]{particleName, "maxOccurs"};
                reportSchemaError("s4s-att-not-allowed", args, parent);
                max = 1;
            }
        }
        if (min == 0 && max== 0) {
            particle.fType = XSParticleDecl.PARTICLE_EMPTY;
            return null;
        }
        if (processingAllEl) {
            if (max != 1) {
                reportSchemaError("cos-all-limited.2", new Object[]{
                        (max == SchemaSymbols.OCCURRENCE_UNBOUNDED) ? SchemaSymbols.ATTVAL_UNBOUNDED : Integer.toString(max),
                        ((XSElementDecl)particle.fValue).getName()}, parent);
                max = 1;
                if (min > 1)
                    min = 1;
            }
        }
        else if (processingAllGP || groupRefWithAll) {
            if (max != 1) {
                reportSchemaError("cos-all-limited.1.2", null, parent);
                if (min > 1)
                    min = 1;
                max = 1;
            }
        }
        particle.fMinOccurs = min;
        particle.fMaxOccurs = max;
        return particle;
    }
    private static String processAttValue(String original) {
        final int length = original.length();
        for (int i = 0; i < length; ++i) {
            char currChar = original.charAt(i);
            if (currChar == '"' || currChar == '<' || currChar == '&' ||
                    currChar == 0x09 || currChar == 0x0A || currChar == 0x0D) {
                return escapeAttValue(original, i);
            }
        }
        return original;
    }
    private static String escapeAttValue(String original, int from) {
        int i;
        final int length = original.length();
        StringBuffer newVal = new StringBuffer(length);
        newVal.append(original.substring(0, from));
        for (i = from; i < length; ++i) {
            char currChar = original.charAt(i);
            if (currChar == '"') {
                newVal.append("&quot;");
            } 
            else if (currChar == '<') {
                newVal.append("&lt;");
            }
            else if (currChar == '&') {
                newVal.append("&amp;");
            }
            else if (currChar == 0x09) {
                newVal.append("&#x9;");
            }
            else if (currChar == 0x0A) {
                newVal.append("&#xA;");
            }
            else if (currChar == 0x0D) {
                newVal.append("&#xD;");
            }
            else {
                newVal.append(currChar);
            }
        }
        return newVal.toString();
    }
}
