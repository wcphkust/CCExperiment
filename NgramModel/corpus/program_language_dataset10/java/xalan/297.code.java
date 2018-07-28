package org.apache.xalan.xsltc.compiler;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionList;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.NodeSetType;
import org.apache.xalan.xsltc.compiler.util.NodeType;
import org.apache.xalan.xsltc.compiler.util.ReferenceType;
import org.apache.xalan.xsltc.compiler.util.ResultTreeType;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
import org.apache.xalan.xsltc.compiler.util.Util;
final class CopyOf extends Instruction {
    private Expression _select;
    public void display(int indent) {
	indent(indent);
	Util.println("CopyOf");
	indent(indent + IndentIncrement);
	Util.println("select " + _select.toString());
    }
    public void parseContents(Parser parser) {
	_select = parser.parseExpression(this, "select", null);
        if (_select.isDummy()) {
	    reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "select");
	    return;
        }
    }
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	final Type tselect = _select.typeCheck(stable);
	if (tselect instanceof NodeType ||
	    tselect instanceof NodeSetType ||
	    tselect instanceof ReferenceType ||
	    tselect instanceof ResultTreeType) {
	}
	else {
	    _select = new CastExpr(_select, Type.String);
	}
	return Type.Void;
    }
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	final Type tselect = _select.getType();
	final String CPY1_SIG = "("+NODE_ITERATOR_SIG+TRANSLET_OUTPUT_SIG+")V";
	final int cpy1 = cpg.addInterfaceMethodref(DOM_INTF, "copy", CPY1_SIG);
	final String CPY2_SIG = "("+NODE_SIG+TRANSLET_OUTPUT_SIG+")V";
	final int cpy2 = cpg.addInterfaceMethodref(DOM_INTF, "copy", CPY2_SIG);
	final String getDoc_SIG = "()"+NODE_SIG;
	final int getDoc = cpg.addInterfaceMethodref(DOM_INTF, "getDocument", getDoc_SIG);
	if (tselect instanceof NodeSetType) {
	    il.append(methodGen.loadDOM());
	    _select.translate(classGen, methodGen);	
	    _select.startIterator(classGen, methodGen);
	    il.append(methodGen.loadHandler());
	    il.append(new INVOKEINTERFACE(cpy1, 3));
	}
	else if (tselect instanceof NodeType) {
	    il.append(methodGen.loadDOM());
	    _select.translate(classGen, methodGen);	
	    il.append(methodGen.loadHandler());
	    il.append(new INVOKEINTERFACE(cpy2, 3));
	}
	else if (tselect instanceof ResultTreeType) {
	    _select.translate(classGen, methodGen);	
	    il.append(DUP); 
	    il.append(new INVOKEINTERFACE(getDoc,1)); 
	    il.append(methodGen.loadHandler());
	    il.append(new INVOKEINTERFACE(cpy2, 3));
	}
	else if (tselect instanceof ReferenceType) {
	    _select.translate(classGen, methodGen);
	    il.append(methodGen.loadHandler());
	    il.append(methodGen.loadCurrentNode());
	    il.append(methodGen.loadDOM());
	    final int copy = cpg.addMethodref(BASIS_LIBRARY_CLASS, "copy",
					      "(" 
					      + OBJECT_SIG  
					      + TRANSLET_OUTPUT_SIG 
					      + NODE_SIG
					      + DOM_INTF_SIG
					      + ")V");
	    il.append(new INVOKESTATIC(copy));
	}
	else {
	    il.append(classGen.loadTranslet());
	    _select.translate(classGen, methodGen);
	    il.append(methodGen.loadHandler());
	    il.append(new INVOKEVIRTUAL(cpg.addMethodref(TRANSLET_CLASS,
							 CHARACTERSW,
							 CHARACTERSW_SIG)));
	}
    }
}