package org.apache.xalan.xsltc.compiler;
import java.util.Vector;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.PUSH;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
final class ElementAvailableCall extends FunctionCall {
    public ElementAvailableCall(QName fname, Vector arguments) {
	super(fname, arguments);
    }
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	if (argument() instanceof LiteralExpr) {
	    return _type = Type.Boolean;
	}
	ErrorMsg err = new ErrorMsg(ErrorMsg.NEED_LITERAL_ERR,
				    "element-available", this);
	throw new TypeCheckError(err);
    }
    public Object evaluateAtCompileTime() {
	return getResult() ? Boolean.TRUE : Boolean.FALSE;
    }
    public boolean getResult() {
	try {
	    final LiteralExpr arg = (LiteralExpr) argument();
	    final String qname = arg.getValue();
	    final int index = qname.indexOf(':');
	    final String localName = (index > 0) ? 
		qname.substring(index + 1) : qname;
	    return getParser().elementSupported(arg.getNamespace(), 
					        localName);
	}
	catch (ClassCastException e) {
	    return false;
	}
    }
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final boolean result = getResult();
	methodGen.getInstructionList().append(new PUSH(cpg, result));
    }
}
