package org.apache.xalan.xsltc.compiler;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
import org.apache.xalan.xsltc.compiler.util.Util;
import org.apache.xml.utils.XML11Char;
import java.util.Vector;
final class CallTemplate extends Instruction {
    private QName _name;
    private Object[] _parameters = null;
    private Template _calleeTemplate = null;
    public void display(int indent) {
	indent(indent);
	System.out.print("CallTemplate");
	Util.println(" name " + _name);
	displayContents(indent + IndentIncrement);
    }
    public boolean hasWithParams() {
	return elementCount() > 0;
    }
    public void parseContents(Parser parser) {
        final String name = getAttribute("name");
        if (name.length() > 0) {
            if (!XML11Char.isXML11ValidQName(name)) {
                ErrorMsg err = new ErrorMsg(ErrorMsg.INVALID_QNAME_ERR, name, this);
                parser.reportError(Constants.ERROR, err);           
            }                
            _name = parser.getQNameIgnoreDefaultNs(name);
        }
        else {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "name");		
        }
	parseChildren(parser);
    }
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	final Template template = stable.lookupTemplate(_name);
	if (template != null) {
	    typeCheckContents(stable);
	}
	else {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.TEMPLATE_UNDEF_ERR,_name,this);
	    throw new TypeCheckError(err);
	}
	return Type.Void;
    }
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final Stylesheet stylesheet = classGen.getStylesheet();
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	if (stylesheet.hasLocalParams() || hasContents()) {
	    _calleeTemplate = getCalleeTemplate();
	    if (_calleeTemplate != null) {
	    	buildParameterList();
	    }
	    else {
	        final int push = cpg.addMethodref(TRANSLET_CLASS, 
					          PUSH_PARAM_FRAME,
					          PUSH_PARAM_FRAME_SIG);
	        il.append(classGen.loadTranslet());
	        il.append(new INVOKEVIRTUAL(push));
	        translateContents(classGen, methodGen);
	    }
	}
	final String className = stylesheet.getClassName();
        String methodName = Util.escape(_name.toString());
	il.append(classGen.loadTranslet());
	il.append(methodGen.loadDOM());
	il.append(methodGen.loadIterator());
	il.append(methodGen.loadHandler());
	il.append(methodGen.loadCurrentNode());
	StringBuffer methodSig = new StringBuffer("(" + DOM_INTF_SIG 
            + NODE_ITERATOR_SIG + TRANSLET_OUTPUT_SIG + NODE_SIG);
	if (_calleeTemplate != null) {
	    Vector calleeParams = _calleeTemplate.getParameters();
	    int numParams = _parameters.length;
	    for (int i = 0; i < numParams; i++) {
	        SyntaxTreeNode node = (SyntaxTreeNode)_parameters[i];
                methodSig.append(OBJECT_SIG);   
                if (node instanceof Param) {
                    il.append(ACONST_NULL);
                }
                else {  
                    node.translate(classGen, methodGen);
                }
            }
        }
	methodSig.append(")V");
	il.append(new INVOKEVIRTUAL(cpg.addMethodref(className,
						     methodName,
						     methodSig.toString())));
	if (_calleeTemplate == null && (stylesheet.hasLocalParams() || hasContents())) {
	    final int pop = cpg.addMethodref(TRANSLET_CLASS,
					     POP_PARAM_FRAME,
					     POP_PARAM_FRAME_SIG);
	    il.append(classGen.loadTranslet());
	    il.append(new INVOKEVIRTUAL(pop));
	}
    }
    public Template getCalleeTemplate() {
    	Template foundTemplate
            = getXSLTC().getParser().getSymbolTable().lookupTemplate(_name);
        return foundTemplate.isSimpleNamedTemplate() ? foundTemplate : null;
    }
    private void buildParameterList() {   	
    	Vector defaultParams = _calleeTemplate.getParameters();
    	int numParams = defaultParams.size();
    	_parameters = new Object[numParams];
    	for (int i = 0; i < numParams; i++) {
    	    _parameters[i] = defaultParams.elementAt(i);
    	}
    	int count = elementCount();
    	for (int i = 0; i < count; i++) {
    	    Object node = elementAt(i);
    	    if (node instanceof WithParam) {
    	    	WithParam withParam = (WithParam)node;
    	    	QName name = withParam.getName();
    	    	for (int k = 0; k < numParams; k++) {
    	    	    Object object = _parameters[k];
    	    	    if (object instanceof Param 
    	    	        && ((Param)object).getName().equals(name)) {
    	    	        withParam.setDoParameterOptimization(true);
    	    	        _parameters[k] = withParam;
    	    	        break;
    	    	    }
    	    	    else if (object instanceof WithParam 
    	    	        && ((WithParam)object).getName().equals(name)) {
    	    	        withParam.setDoParameterOptimization(true);
    	    	        _parameters[k] = withParam;    	    	        
    	    	        break;
    	    	    }
    	    	}    	    	
    	    }
    	}
     }
}
