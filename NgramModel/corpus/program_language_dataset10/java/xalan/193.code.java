package org.apache.xalan.templates;
import org.apache.xml.utils.QName;
import org.apache.xpath.XPath;
public class KeyDeclaration extends ElemTemplateElement
{
    static final long serialVersionUID = 7724030248631137918L;
  public KeyDeclaration(Stylesheet parentNode, int docOrderNumber)
  {
    m_parentNode = parentNode;
    setUid(docOrderNumber);
  }
  private QName m_name;
  public void setName(QName name)
  {
    m_name = name;
  }
  public QName getName()
  {
    return m_name;
  }
  public String getNodeName()
  {
    return Constants.ELEMNAME_KEY_STRING;
  }
  private XPath m_matchPattern = null;
  public void setMatch(XPath v)
  {
    m_matchPattern = v;
  }
  public XPath getMatch()
  {
    return m_matchPattern;
  }
  private XPath m_use;
  public void setUse(XPath v)
  {
    m_use = v;
  }
  public XPath getUse()
  {
    return m_use;
  }
  public int getXSLToken()
  {
    return Constants.ELEMNAME_KEY;
  }
  public void compose(StylesheetRoot sroot) 
    throws javax.xml.transform.TransformerException
  {
    super.compose(sroot);
    java.util.Vector vnames = sroot.getComposeState().getVariableNames();
    if(null != m_matchPattern)
      m_matchPattern.fixupVariables(vnames, sroot.getComposeState().getGlobalsSize());
    if(null != m_use)
      m_use.fixupVariables(vnames, sroot.getComposeState().getGlobalsSize());
  }
  public void recompose(StylesheetRoot root)
  {
    root.recomposeKeys(this);
  }
}
