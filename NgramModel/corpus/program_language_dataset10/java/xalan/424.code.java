package org.apache.xalan.xsltc.compiler.util;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IFGE;
import org.apache.bcel.generic.IFGT;
import org.apache.bcel.generic.IFLE;
import org.apache.bcel.generic.IFLT;
import org.apache.bcel.generic.IF_ICMPGE;
import org.apache.bcel.generic.IF_ICMPGT;
import org.apache.bcel.generic.IF_ICMPLE;
import org.apache.bcel.generic.IF_ICMPLT;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionConstants;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.NEW;
import org.apache.xalan.xsltc.compiler.Constants;
import org.apache.xalan.xsltc.compiler.FlowList;
public final class IntType extends NumberType {
    protected IntType() {}
    public String toString() {
	return "int";
    }
    public boolean identicalTo(Type other) {
	return this == other;
    }
    public String toSignature() {
	return "I";
    }
    public org.apache.bcel.generic.Type toJCType() {
	return org.apache.bcel.generic.Type.INT;
    }
    public int distanceTo(Type type) {
	if (type == this) {
	    return 0;
	}
	else if (type == Type.Real) {
	    return 1;
	}
	else
	    return Integer.MAX_VALUE;
    }
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen, 
			    final Type type) {
	if (type == Type.Real) {
	    translateTo(classGen, methodGen, (RealType) type);
	}
	else if (type == Type.String) {
	    translateTo(classGen, methodGen, (StringType) type);
	}
	else if (type == Type.Boolean) {
	    translateTo(classGen, methodGen, (BooleanType) type);
	}
	else if (type == Type.Reference) {
	    translateTo(classGen, methodGen, (ReferenceType) type);
	}
	else {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
					toString(), type.toString());
	    classGen.getParser().reportError(Constants.FATAL, err);
	}
    }
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen, 
			    RealType type) {
	methodGen.getInstructionList().append(I2D);
    }
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen, 
			    StringType type) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	il.append(new INVOKESTATIC(cpg.addMethodref(INTEGER_CLASS,
						    "toString",
						    "(I)" + STRING_SIG)));
    }
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen, 
			    BooleanType type) {
	final InstructionList il = methodGen.getInstructionList();
	final BranchHandle falsec = il.append(new IFEQ(null));
	il.append(ICONST_1);
	final BranchHandle truec = il.append(new GOTO(null));
	falsec.setTarget(il.append(ICONST_0));
	truec.setTarget(il.append(NOP));
    }
    public FlowList translateToDesynthesized(ClassGenerator classGen, 
					     MethodGenerator methodGen, 
					     BooleanType type) {
	final InstructionList il = methodGen.getInstructionList();
	return new FlowList(il.append(new IFEQ(null)));
    }
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen, 
			    ReferenceType type) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	il.append(new NEW(cpg.addClass(INTEGER_CLASS)));
	il.append(DUP_X1);
	il.append(SWAP);
	il.append(new INVOKESPECIAL(cpg.addMethodref(INTEGER_CLASS,
						     "<init>", "(I)V")));
    }
    public void translateTo(ClassGenerator classGen, MethodGenerator methodGen, 
			    Class clazz) {
	final InstructionList il = methodGen.getInstructionList();
	if (clazz == Character.TYPE) {
	    il.append(I2C);
	}
	else if (clazz == Byte.TYPE) {
	    il.append(I2B);
	}
	else if (clazz == Short.TYPE) {
	    il.append(I2S);
	}
	else if (clazz == Integer.TYPE) {
	    il.append(NOP);
	}
	else if (clazz == Long.TYPE) {
	    il.append(I2L);
	}
	else if (clazz == Float.TYPE) {
	    il.append(I2F);
	}
	else if (clazz == Double.TYPE) {
	    il.append(I2D);
	}
       else if (clazz.isAssignableFrom(java.lang.Double.class)) {
           il.append(I2D);
           Type.Real.translateTo(classGen, methodGen, Type.Reference);
        }
	else {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.DATA_CONVERSION_ERR,
					toString(), clazz.getName());
	    classGen.getParser().reportError(Constants.FATAL, err);
	}
    }
    public void translateBox(ClassGenerator classGen,
			     MethodGenerator methodGen) {
	translateTo(classGen, methodGen, Type.Reference);
    }
    public void translateUnBox(ClassGenerator classGen,
			       MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	il.append(new CHECKCAST(cpg.addClass(INTEGER_CLASS)));
	final int index = cpg.addMethodref(INTEGER_CLASS,
					   INT_VALUE, 
					   INT_VALUE_SIG);
	il.append(new INVOKEVIRTUAL(index));
    }
    public Instruction ADD() {
	return InstructionConstants.IADD;
    }
    public Instruction SUB() {
	return InstructionConstants.ISUB;
    }
    public Instruction MUL() {
	return InstructionConstants.IMUL;
    }
    public Instruction DIV() {
	return InstructionConstants.IDIV;
    }
    public Instruction REM() {
	return InstructionConstants.IREM;
    }
    public Instruction NEG() {
	return InstructionConstants.INEG;
    }
    public Instruction LOAD(int slot) {
	return new ILOAD(slot);
    }
    public Instruction STORE(int slot) {
	return new ISTORE(slot);
    }
    public BranchInstruction GT(boolean tozero) {
	return tozero ? (BranchInstruction) new IFGT(null) : 
	    (BranchInstruction) new IF_ICMPGT(null);
    }
    public BranchInstruction GE(boolean tozero) {
	return tozero ? (BranchInstruction) new IFGE(null) : 
	    (BranchInstruction) new IF_ICMPGE(null);
    }
    public BranchInstruction LT(boolean tozero) {
	return tozero ? (BranchInstruction) new IFLT(null) : 
	    (BranchInstruction) new IF_ICMPLT(null);
    }
    public BranchInstruction LE(boolean tozero) {
	return tozero ? (BranchInstruction) new IFLE(null) : 
	    (BranchInstruction) new IF_ICMPLE(null);
    }
}
