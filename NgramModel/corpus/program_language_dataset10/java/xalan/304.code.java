package org.apache.xalan.xsltc.compiler;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionList;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;
final class Fallback extends Instruction {
    private boolean _active = false;
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	if (_active) {
	    return(typeCheckContents(stable));
	}
	else {
	    return Type.Void;
	}
    }
    public void activate() {
	_active = true;
    }
    public String toString() {
	return("fallback");
    }
    public void parseContents(Parser parser) {
	if (_active) parseChildren(parser);
    }
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	if (_active) translateContents(classGen, methodGen);
    }
}
