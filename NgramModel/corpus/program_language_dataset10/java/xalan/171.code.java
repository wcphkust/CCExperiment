package org.apache.xalan.templates;
import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPath;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XObject;
public class ElemIf extends ElemTemplateElement
{
    static final long serialVersionUID = 2158774632427453022L;
  private XPath m_test = null;
  public void setTest(XPath v)
  {
    m_test = v;
  }
  public XPath getTest()
  {
    return m_test;
  }
  public void compose(StylesheetRoot sroot) throws TransformerException
  {
    super.compose(sroot);
    java.util.Vector vnames = sroot.getComposeState().getVariableNames();
    if (null != m_test)
      m_test.fixupVariables(vnames, sroot.getComposeState().getGlobalsSize());
  }
  public int getXSLToken()
  {
    return Constants.ELEMNAME_IF;
  }
  public String getNodeName()
  {
    return Constants.ELEMNAME_IF_STRING;
  }
  public void execute(TransformerImpl transformer) throws TransformerException
  {
    XPathContext xctxt = transformer.getXPathContext();
    int sourceNode = xctxt.getCurrentNode();
    if (transformer.getDebug())
    {
      XObject test = m_test.execute(xctxt, sourceNode, this);
      if (transformer.getDebug())
        transformer.getTraceManager().fireSelectedEvent(sourceNode, this,
                "test", m_test, test);
      if (transformer.getDebug())
        transformer.getTraceManager().fireTraceEvent(this);
      if (test.bool())
      {
        transformer.executeChildTemplates(this, true);        
      }
      if (transformer.getDebug())
        transformer.getTraceManager().fireTraceEndEvent(this);
    }
    else if (m_test.bool(xctxt, sourceNode, this))
    {
      transformer.executeChildTemplates(this, true);
    }
  }
  protected void callChildVisitors(XSLTVisitor visitor, boolean callAttrs)
  {
  	if(callAttrs)
  		m_test.getExpression().callVisitors(m_test, visitor);
    super.callChildVisitors(visitor, callAttrs);
  }
}