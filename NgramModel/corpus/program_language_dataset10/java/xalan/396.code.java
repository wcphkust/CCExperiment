package org.apache.xalan.xsltc.compiler;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.PUSH;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
import org.apache.xalan.xsltc.compiler.util.Util;
import org.apache.xml.utils.XML11Char;
final class XslElement extends Instruction {
    private String  _prefix;
    private boolean _ignore = false;
    private boolean _isLiteralName = true;
    private AttributeValueTemplate _name; 
    private AttributeValueTemplate _namespace;
    public void display(int indent) {
	indent(indent);
	Util.println("Element " + _name);
	displayContents(indent + IndentIncrement);
    }
    public boolean declaresDefaultNS() {
	return false;
    }
    public void parseContents(Parser parser) {
	final SymbolTable stable = parser.getSymbolTable();
	String name = getAttribute("name");
	if (name == EMPTYSTRING) {
	    ErrorMsg msg = new ErrorMsg(ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
					name, this);
	    parser.reportError(WARNING, msg);
	    parseChildren(parser);
	    _ignore = true; 	
	    return;
	}
	String namespace = getAttribute("namespace");
        _isLiteralName = Util.isLiteral(name);
	if (_isLiteralName) {
            if (!XML11Char.isXML11ValidQName(name)) {
		ErrorMsg msg = new ErrorMsg(ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
					    name, this);
		parser.reportError(WARNING, msg);
		parseChildren(parser);
		_ignore = true; 	
		return;
	    }
	    final QName qname = parser.getQNameSafe(name);
	    String prefix = qname.getPrefix();
	    String local = qname.getLocalPart();
	    if (prefix == null) {
		prefix = EMPTYSTRING;
	    }
	    if (!hasAttribute("namespace")) {
		namespace = lookupNamespace(prefix); 
		if (namespace == null) {
		    ErrorMsg err = new ErrorMsg(ErrorMsg.NAMESPACE_UNDEF_ERR,
						prefix, this);
		    parser.reportError(WARNING, err);
		    parseChildren(parser);
		    _ignore = true; 	
		    return;
		}
		_prefix = prefix;
		_namespace = new AttributeValueTemplate(namespace, parser, this);
	    }
	    else {
		if (prefix == EMPTYSTRING) {
        	    if (Util.isLiteral(namespace)) {
			prefix = lookupPrefix(namespace);
			if (prefix == null) {
			    prefix = stable.generateNamespacePrefix();
			}
		    }
		    final StringBuffer newName = new StringBuffer(prefix);
		    if (prefix != EMPTYSTRING) {
			newName.append(':');
		    }
		    name = newName.append(local).toString();
		}
		_prefix = prefix;
		_namespace = new AttributeValueTemplate(namespace, parser, this);
	    }
	}
	else {
            _namespace = (namespace == EMPTYSTRING) ? null :
			 new AttributeValueTemplate(namespace, parser, this);
	}
	_name = new AttributeValueTemplate(name, parser, this);
	final String useSets = getAttribute("use-attribute-sets");
	if (useSets.length() > 0) {
            if (!Util.isValidQNames(useSets)) {
                ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_QNAME_ERR, useSets, this);
                parser.reportError(Constants.ERROR, err);	
            }
	    setFirstElement(new UseAttributeSets(useSets, parser));
	}
	parseChildren(parser);
    }
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	if (!_ignore) {
	    _name.typeCheck(stable);
	    if (_namespace != null) {
		_namespace.typeCheck(stable);
	    }
	}
	typeCheckContents(stable);
	return Type.Void;
    }
    public void translateLiteral(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	if (!_ignore) {
	    il.append(methodGen.loadHandler());
	    _name.translate(classGen, methodGen);
	    il.append(DUP2);
	    il.append(methodGen.startElement());
	    if (_namespace != null) {
		il.append(methodGen.loadHandler());
		il.append(new PUSH(cpg, _prefix));
		_namespace.translate(classGen,methodGen);
		il.append(methodGen.namespace());
	    }
	}
	translateContents(classGen, methodGen);
	if (!_ignore) {
	    il.append(methodGen.endElement());
	}
    }
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	LocalVariableGen local = null;
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	if (_isLiteralName) {
	    translateLiteral(classGen, methodGen);
	    return;
	}
	if (!_ignore) {
            LocalVariableGen nameValue =
                    methodGen.addLocalVariable2("nameValue",
                                                Util.getJCRefType(STRING_SIG),
                                                null);
            _name.translate(classGen, methodGen);
            nameValue.setStart(il.append(new ASTORE(nameValue.getIndex())));
            il.append(new ALOAD(nameValue.getIndex()));
            final int check = cpg.addMethodref(BASIS_LIBRARY_CLASS, "checkQName",
                            "("
                            +STRING_SIG
                            +")V");                 
            il.append(new INVOKESTATIC(check));
            il.append(methodGen.loadHandler());         
            nameValue.setEnd(il.append(new ALOAD(nameValue.getIndex())));
	    if (_namespace != null) {
		_namespace.translate(classGen, methodGen);
	    }
	    else {
                String transletClassName = getXSLTC().getClassName();
                il.append(DUP);
                il.append(new PUSH(cpg, getNodeIDForStylesheetNSLookup()));
                il.append(new GETSTATIC(cpg.addFieldref(
                                             transletClassName,
                                             STATIC_NS_ANCESTORS_ARRAY_FIELD,
                                             NS_ANCESTORS_INDEX_SIG)));
                il.append(new GETSTATIC(cpg.addFieldref(
                                             transletClassName,
                                             STATIC_PREFIX_URIS_IDX_ARRAY_FIELD,
                                             PREFIX_URIS_IDX_SIG)));
                il.append(new GETSTATIC(cpg.addFieldref(
                                             transletClassName,
                                             STATIC_PREFIX_URIS_ARRAY_FIELD,
                                             PREFIX_URIS_ARRAY_SIG)));
                il.append(ICONST_0);
                il.append(
                    new INVOKESTATIC(
                        cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                           LOOKUP_STYLESHEET_QNAME_NS_REF,
                                           LOOKUP_STYLESHEET_QNAME_NS_SIG)));
	    }
	    il.append(methodGen.loadHandler());
	    il.append(methodGen.loadDOM());
	    il.append(methodGen.loadCurrentNode());
            il.append(new INVOKESTATIC(
            cpg.addMethodref(BASIS_LIBRARY_CLASS, "startXslElement",
                    "(" + STRING_SIG 
                    + STRING_SIG 
                    + TRANSLET_OUTPUT_SIG 
                    + DOM_INTF_SIG + "I)" + STRING_SIG)));                
	}
	translateContents(classGen, methodGen);
	if (!_ignore) {
	    il.append(methodGen.endElement());
	}
    }
    public void translateContents(ClassGenerator classGen,
				  MethodGenerator methodGen) {
	final int n = elementCount();
	for (int i = 0; i < n; i++) {
	    final SyntaxTreeNode item =
		(SyntaxTreeNode)getContents().elementAt(i);
	    if (_ignore && item instanceof XslAttribute) continue;
	    item.translate(classGen, methodGen);
	}
    }
}
