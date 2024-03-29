package org.apache.xpath.compiler;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SAXSourceLocator;
import org.apache.xpath.Expression;
import org.apache.xpath.axes.UnionPathIterator;
import org.apache.xpath.axes.WalkerFactory;
import org.apache.xpath.functions.FuncExtFunction;
import org.apache.xpath.functions.FuncExtFunctionAvailable;
import org.apache.xpath.functions.Function;
import org.apache.xpath.functions.WrongNumberArgsException;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XString;
import org.apache.xpath.operations.And;
import org.apache.xpath.operations.Div;
import org.apache.xpath.operations.Equals;
import org.apache.xpath.operations.Gt;
import org.apache.xpath.operations.Gte;
import org.apache.xpath.operations.Lt;
import org.apache.xpath.operations.Lte;
import org.apache.xpath.operations.Minus;
import org.apache.xpath.operations.Mod;
import org.apache.xpath.operations.Mult;
import org.apache.xpath.operations.Neg;
import org.apache.xpath.operations.NotEquals;
import org.apache.xpath.operations.Operation;
import org.apache.xpath.operations.Or;
import org.apache.xpath.operations.Plus;
import org.apache.xpath.operations.UnaryOperation;
import org.apache.xpath.operations.Variable;
import org.apache.xpath.patterns.FunctionPattern;
import org.apache.xpath.patterns.NodeTest;
import org.apache.xpath.patterns.StepPattern;
import org.apache.xpath.patterns.UnionPattern;
import org.apache.xpath.res.XPATHErrorResources;
public class Compiler extends OpMap
{
  public Compiler(ErrorListener errorHandler, SourceLocator locator, 
            FunctionTable fTable)
  {
    m_errorHandler = errorHandler;
    m_locator = locator;
    m_functionTable = fTable;
  }
  public Compiler()
  {
    m_errorHandler = null;
    m_locator = null;
  }
  public Expression compile(int opPos) throws TransformerException
  {
    int op = getOp(opPos);
    Expression expr = null;
    switch (op)
    {
    case OpCodes.OP_XPATH :
      expr = compile(opPos + 2); break;
    case OpCodes.OP_OR :
      expr = or(opPos); break;
    case OpCodes.OP_AND :
      expr = and(opPos); break;
    case OpCodes.OP_NOTEQUALS :
      expr = notequals(opPos); break;
    case OpCodes.OP_EQUALS :
      expr = equals(opPos); break;
    case OpCodes.OP_LTE :
      expr = lte(opPos); break;
    case OpCodes.OP_LT :
      expr = lt(opPos); break;
    case OpCodes.OP_GTE :
      expr = gte(opPos); break;
    case OpCodes.OP_GT :
      expr = gt(opPos); break;
    case OpCodes.OP_PLUS :
      expr = plus(opPos); break;
    case OpCodes.OP_MINUS :
      expr = minus(opPos); break;
    case OpCodes.OP_MULT :
      expr = mult(opPos); break;
    case OpCodes.OP_DIV :
      expr = div(opPos); break;
    case OpCodes.OP_MOD :
      expr = mod(opPos); break;
    case OpCodes.OP_NEG :
      expr = neg(opPos); break;
    case OpCodes.OP_STRING :
      expr = string(opPos); break;
    case OpCodes.OP_BOOL :
      expr = bool(opPos); break;
    case OpCodes.OP_NUMBER :
      expr = number(opPos); break;
    case OpCodes.OP_UNION :
      expr = union(opPos); break;
    case OpCodes.OP_LITERAL :
      expr = literal(opPos); break;
    case OpCodes.OP_VARIABLE :
      expr = variable(opPos); break;
    case OpCodes.OP_GROUP :
      expr = group(opPos); break;
    case OpCodes.OP_NUMBERLIT :
      expr = numberlit(opPos); break;
    case OpCodes.OP_ARGUMENT :
      expr = arg(opPos); break;
    case OpCodes.OP_EXTFUNCTION :
      expr = compileExtension(opPos); break;
    case OpCodes.OP_FUNCTION :
      expr = compileFunction(opPos); break;
    case OpCodes.OP_LOCATIONPATH :
      expr = locationPath(opPos); break;
    case OpCodes.OP_PREDICATE :
      expr = null; break;  
    case OpCodes.OP_MATCHPATTERN :
      expr = matchPattern(opPos + 2); break;
    case OpCodes.OP_LOCATIONPATHPATTERN :
      expr = locationPathPattern(opPos); break;
    case OpCodes.OP_QUO:
      error(XPATHErrorResources.ER_UNKNOWN_OPCODE,
            new Object[]{ "quo" });  
      break;
    default :
      error(XPATHErrorResources.ER_UNKNOWN_OPCODE,
            new Object[]{ Integer.toString(getOp(opPos)) });  
    }
    return expr;
  }
  private Expression compileOperation(Operation operation, int opPos)
          throws TransformerException
  {
    int leftPos = getFirstChildPos(opPos);
    int rightPos = getNextOpPos(leftPos);
    operation.setLeftRight(compile(leftPos), compile(rightPos));
    return operation;
  }
  private Expression compileUnary(UnaryOperation unary, int opPos)
          throws TransformerException
  {
    int rightPos = getFirstChildPos(opPos);
    unary.setRight(compile(rightPos));
    return unary;
  }
  protected Expression or(int opPos) throws TransformerException
  {
    return compileOperation(new Or(), opPos);
  }
  protected Expression and(int opPos) throws TransformerException
  {
    return compileOperation(new And(), opPos);
  }
  protected Expression notequals(int opPos) throws TransformerException
  {
    return compileOperation(new NotEquals(), opPos);
  }
  protected Expression equals(int opPos) throws TransformerException
  {
    return compileOperation(new Equals(), opPos);
  }
  protected Expression lte(int opPos) throws TransformerException
  {
    return compileOperation(new Lte(), opPos);
  }
  protected Expression lt(int opPos) throws TransformerException
  {
    return compileOperation(new Lt(), opPos);
  }
  protected Expression gte(int opPos) throws TransformerException
  {
    return compileOperation(new Gte(), opPos);
  }
  protected Expression gt(int opPos) throws TransformerException
  {
    return compileOperation(new Gt(), opPos);
  }
  protected Expression plus(int opPos) throws TransformerException
  {
    return compileOperation(new Plus(), opPos);
  }
  protected Expression minus(int opPos) throws TransformerException
  {
    return compileOperation(new Minus(), opPos);
  }
  protected Expression mult(int opPos) throws TransformerException
  {
    return compileOperation(new Mult(), opPos);
  }
  protected Expression div(int opPos) throws TransformerException
  {
    return compileOperation(new Div(), opPos);
  }
  protected Expression mod(int opPos) throws TransformerException
  {
    return compileOperation(new Mod(), opPos);
  }
  protected Expression neg(int opPos) throws TransformerException
  {
    return compileUnary(new Neg(), opPos);
  }
  protected Expression string(int opPos) throws TransformerException
  {
    return compileUnary(new org.apache.xpath.operations.String(), opPos);
  }
  protected Expression bool(int opPos) throws TransformerException
  {
    return compileUnary(new org.apache.xpath.operations.Bool(), opPos);
  }
  protected Expression number(int opPos) throws TransformerException
  {
    return compileUnary(new org.apache.xpath.operations.Number(), opPos);
  }
  protected Expression literal(int opPos)
  {
    opPos = getFirstChildPos(opPos);
    return (XString) getTokenQueue().elementAt(getOp(opPos));
  }
  protected Expression numberlit(int opPos)
  {
    opPos = getFirstChildPos(opPos);
    return (XNumber) getTokenQueue().elementAt(getOp(opPos));
  }
  protected Expression variable(int opPos) throws TransformerException
  {
    Variable var = new Variable();
    opPos = getFirstChildPos(opPos);
    int nsPos = getOp(opPos);
    java.lang.String namespace 
      = (OpCodes.EMPTY == nsPos) ? null 
                                   : (java.lang.String) getTokenQueue().elementAt(nsPos);
    java.lang.String localname 
      = (java.lang.String) getTokenQueue().elementAt(getOp(opPos+1));
    QName qname = new QName(namespace, localname);
    var.setQName(qname);
    return var;
  }
  protected Expression group(int opPos) throws TransformerException
  {
    return compile(opPos + 2);
  }
  protected Expression arg(int opPos) throws TransformerException
  {
    return compile(opPos + 2);
  }
  protected Expression union(int opPos) throws TransformerException
  {
    locPathDepth++;
    try
    {
      return UnionPathIterator.createUnionIterator(this, opPos);
    }
    finally
    {
      locPathDepth--;
    }
  }
  private int locPathDepth = -1;
  public int getLocationPathDepth()
  {
    return locPathDepth;
  }
  FunctionTable getFunctionTable()
  {
    return m_functionTable;
  }
  public Expression locationPath(int opPos) throws TransformerException
  {
    locPathDepth++;
    try
    {
      DTMIterator iter = WalkerFactory.newDTMIterator(this, opPos, (locPathDepth == 0));
      return (Expression)iter; 
    }
    finally
    {
      locPathDepth--;
    }
  }
  public Expression predicate(int opPos) throws TransformerException
  {
    return compile(opPos + 2);
  }
  protected Expression matchPattern(int opPos) throws TransformerException
  {
    locPathDepth++;
    try
    {
      int nextOpPos = opPos;
      int i;
      for (i = 0; getOp(nextOpPos) == OpCodes.OP_LOCATIONPATHPATTERN; i++)
      {
        nextOpPos = getNextOpPos(nextOpPos);
      }
      if (i == 1)
        return compile(opPos);
      UnionPattern up = new UnionPattern();
      StepPattern[] patterns = new StepPattern[i];
      for (i = 0; getOp(opPos) == OpCodes.OP_LOCATIONPATHPATTERN; i++)
      {
        nextOpPos = getNextOpPos(opPos);
        patterns[i] = (StepPattern) compile(opPos);
        opPos = nextOpPos;
      }
      up.setPatterns(patterns);
      return up;
    }
    finally
    {
      locPathDepth--;
    }
  }
  public Expression locationPathPattern(int opPos)
          throws TransformerException
  {
    opPos = getFirstChildPos(opPos);
    return stepPattern(opPos, 0, null);
  }
  public int getWhatToShow(int opPos)
  {
    int axesType = getOp(opPos);
    int testType = getOp(opPos + 3);
    switch (testType)
    {
    case OpCodes.NODETYPE_COMMENT :
      return DTMFilter.SHOW_COMMENT;
    case OpCodes.NODETYPE_TEXT :
      return DTMFilter.SHOW_TEXT | DTMFilter.SHOW_CDATA_SECTION ;
    case OpCodes.NODETYPE_PI :
      return DTMFilter.SHOW_PROCESSING_INSTRUCTION;
    case OpCodes.NODETYPE_NODE :
      switch (axesType)
      {
      case OpCodes.FROM_NAMESPACE:
        return DTMFilter.SHOW_NAMESPACE;
      case OpCodes.FROM_ATTRIBUTES :
      case OpCodes.MATCH_ATTRIBUTE :
        return DTMFilter.SHOW_ATTRIBUTE;
      case OpCodes.FROM_SELF:
      case OpCodes.FROM_ANCESTORS_OR_SELF:
      case OpCodes.FROM_DESCENDANTS_OR_SELF:
        return DTMFilter.SHOW_ALL;
      default:
        if (getOp(0) == OpCodes.OP_MATCHPATTERN)
          return ~DTMFilter.SHOW_ATTRIBUTE
                  & ~DTMFilter.SHOW_DOCUMENT
                  & ~DTMFilter.SHOW_DOCUMENT_FRAGMENT;
        else
          return ~DTMFilter.SHOW_ATTRIBUTE;
      }
    case OpCodes.NODETYPE_ROOT :
      return DTMFilter.SHOW_DOCUMENT | DTMFilter.SHOW_DOCUMENT_FRAGMENT;
    case OpCodes.NODETYPE_FUNCTEST :
      return NodeTest.SHOW_BYFUNCTION;
    case OpCodes.NODENAME :
      switch (axesType)
      {
      case OpCodes.FROM_NAMESPACE :
        return DTMFilter.SHOW_NAMESPACE;
      case OpCodes.FROM_ATTRIBUTES :
      case OpCodes.MATCH_ATTRIBUTE :
        return DTMFilter.SHOW_ATTRIBUTE;
      case OpCodes.MATCH_ANY_ANCESTOR :
      case OpCodes.MATCH_IMMEDIATE_ANCESTOR :
        return DTMFilter.SHOW_ELEMENT;
      default :
        return DTMFilter.SHOW_ELEMENT;
      }
    default :
      return DTMFilter.SHOW_ALL;
    }
  }
private static final boolean DEBUG = false;
  protected StepPattern stepPattern(
          int opPos, int stepCount, StepPattern ancestorPattern)
            throws TransformerException
  {
    int startOpPos = opPos;
    int stepType = getOp(opPos);
    if (OpCodes.ENDOP == stepType)
    {
      return null;
    }
    boolean addMagicSelf = true;
    int endStep = getNextOpPos(opPos);
    StepPattern pattern;
    int argLen;
    switch (stepType)
    {
    case OpCodes.OP_FUNCTION :
      if(DEBUG)
        System.out.println("MATCH_FUNCTION: "+m_currentPattern); 
      addMagicSelf = false;
      argLen = getOp(opPos + OpMap.MAPINDEX_LENGTH);
      pattern = new FunctionPattern(compileFunction(opPos), Axis.PARENT, Axis.CHILD);
      break;
    case OpCodes.FROM_ROOT :
      if(DEBUG)
        System.out.println("FROM_ROOT, "+m_currentPattern);
      addMagicSelf = false;
      argLen = getArgLengthOfStep(opPos);
      opPos = getFirstChildPosOfStep(opPos);
      pattern = new StepPattern(DTMFilter.SHOW_DOCUMENT | 
                                DTMFilter.SHOW_DOCUMENT_FRAGMENT,
                                Axis.PARENT, Axis.CHILD);
      break;
    case OpCodes.MATCH_ATTRIBUTE :
     if(DEBUG)
        System.out.println("MATCH_ATTRIBUTE: "+getStepLocalName(startOpPos)+", "+m_currentPattern);
      argLen = getArgLengthOfStep(opPos);
      opPos = getFirstChildPosOfStep(opPos);
      pattern = new StepPattern(DTMFilter.SHOW_ATTRIBUTE,
                                getStepNS(startOpPos),
                                getStepLocalName(startOpPos),
                                Axis.PARENT, Axis.ATTRIBUTE);
      break;
    case OpCodes.MATCH_ANY_ANCESTOR :
      if(DEBUG)
        System.out.println("MATCH_ANY_ANCESTOR: "+getStepLocalName(startOpPos)+", "+m_currentPattern);
      argLen = getArgLengthOfStep(opPos);
      opPos = getFirstChildPosOfStep(opPos);
      int what = getWhatToShow(startOpPos);
      if(0x00000500 == what)
        addMagicSelf = false;
      pattern = new StepPattern(getWhatToShow(startOpPos),
                                        getStepNS(startOpPos),
                                        getStepLocalName(startOpPos),
                                        Axis.ANCESTOR, Axis.CHILD);
      break;
    case OpCodes.MATCH_IMMEDIATE_ANCESTOR :
      if(DEBUG)
        System.out.println("MATCH_IMMEDIATE_ANCESTOR: "+getStepLocalName(startOpPos)+", "+m_currentPattern);
      argLen = getArgLengthOfStep(opPos);
      opPos = getFirstChildPosOfStep(opPos);
      pattern = new StepPattern(getWhatToShow(startOpPos),
                                getStepNS(startOpPos),
                                getStepLocalName(startOpPos),
                                Axis.PARENT, Axis.CHILD);
      break;
    default :
      error(XPATHErrorResources.ER_UNKNOWN_MATCH_OPERATION, null);  
      return null;
    }
    pattern.setPredicates(getCompiledPredicates(opPos + argLen));
    if(null == ancestorPattern)
    {
    }
    else
    {
      pattern.setRelativePathPattern(ancestorPattern);
    }
    StepPattern relativePathPattern = stepPattern(endStep, stepCount + 1,
                                        pattern);
    return (null != relativePathPattern) ? relativePathPattern : pattern;
  }
  public Expression[] getCompiledPredicates(int opPos)
          throws TransformerException
  {
    int count = countPredicates(opPos);
    if (count > 0)
    {
      Expression[] predicates = new Expression[count];
      compilePredicates(opPos, predicates);
      return predicates;
    }
    return null;
  }
  public int countPredicates(int opPos) throws TransformerException
  {
    int count = 0;
    while (OpCodes.OP_PREDICATE == getOp(opPos))
    {
      count++;
      opPos = getNextOpPos(opPos);
    }
    return count;
  }
  private void compilePredicates(int opPos, Expression[] predicates)
          throws TransformerException
  {
    for (int i = 0; OpCodes.OP_PREDICATE == getOp(opPos); i++)
    {
      predicates[i] = predicate(opPos);
      opPos = getNextOpPos(opPos);
    }
  }
  Expression compileFunction(int opPos) throws TransformerException
  {
    int endFunc = opPos + getOp(opPos + 1) - 1;
    opPos = getFirstChildPos(opPos);
    int funcID = getOp(opPos);
    opPos++;
    if (-1 != funcID)
    {
      Function func = m_functionTable.getFunction(funcID);
      if (func instanceof FuncExtFunctionAvailable)
          ((FuncExtFunctionAvailable) func).setFunctionTable(m_functionTable);
      func.postCompileStep(this);
      try
      {
        int i = 0;
        for (int p = opPos; p < endFunc; p = getNextOpPos(p), i++)
        {
          func.setArg(compile(p), i);
        }
        func.checkNumberArgs(i);
      }
      catch (WrongNumberArgsException wnae)
      {
        java.lang.String name = m_functionTable.getFunctionName(funcID);
        m_errorHandler.fatalError( new TransformerException(
                  XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ONLY_ALLOWS, 
                      new Object[]{name, wnae.getMessage()}), m_locator)); 
      }
      return func;
    }
    else
    {
      error(XPATHErrorResources.ER_FUNCTION_TOKEN_NOT_FOUND, null);  
      return null;
    }
  }
  private static long s_nextMethodId = 0;
  synchronized private long getNextMethodId()
  {
    if (s_nextMethodId == Long.MAX_VALUE)
      s_nextMethodId = 0;
    return s_nextMethodId++;
  }
  private Expression compileExtension(int opPos)
          throws TransformerException
  {
    int endExtFunc = opPos + getOp(opPos + 1) - 1;
    opPos = getFirstChildPos(opPos);
    java.lang.String ns = (java.lang.String) getTokenQueue().elementAt(getOp(opPos));
    opPos++;
    java.lang.String funcName =
      (java.lang.String) getTokenQueue().elementAt(getOp(opPos));
    opPos++;
    Function extension = new FuncExtFunction(ns, funcName, String.valueOf(getNextMethodId()));
    try
    {
      int i = 0;
      while (opPos < endExtFunc)
      {
        int nextOpPos = getNextOpPos(opPos);
        extension.setArg(this.compile(opPos), i);
        opPos = nextOpPos;
        i++;
      }
    }
    catch (WrongNumberArgsException wnae)
    {
      ;  
    }
    return extension;
  }
  public void warn(String msg, Object[] args) throws TransformerException
  {
    java.lang.String fmsg = XSLMessages.createXPATHWarning(msg, args);
    if (null != m_errorHandler)
    {
      m_errorHandler.warning(new TransformerException(fmsg, m_locator));
    }
    else
    {
      System.out.println(fmsg
                          +"; file "+m_locator.getSystemId()
                          +"; line "+m_locator.getLineNumber()
                          +"; column "+m_locator.getColumnNumber());
    }
  }
  public void assertion(boolean b, java.lang.String msg)
  {
    if (!b)
    {
      java.lang.String fMsg = XSLMessages.createXPATHMessage(
        XPATHErrorResources.ER_INCORRECT_PROGRAMMER_ASSERTION,
        new Object[]{ msg });
      throw new RuntimeException(fMsg);
    }
  }
  public void error(String msg, Object[] args) throws TransformerException
  {
    java.lang.String fmsg = XSLMessages.createXPATHMessage(msg, args);
    if (null != m_errorHandler)
    {
      m_errorHandler.fatalError(new TransformerException(fmsg, m_locator));
    }
    else
    {
      throw new TransformerException(fmsg, (SAXSourceLocator)m_locator);
    }
  }
  private PrefixResolver m_currentPrefixResolver = null;
  public PrefixResolver getNamespaceContext()
  {
    return m_currentPrefixResolver;
  }
  public void setNamespaceContext(PrefixResolver pr)
  {
    m_currentPrefixResolver = pr;
  }
  ErrorListener m_errorHandler;
  SourceLocator m_locator;
  private FunctionTable m_functionTable;
}
