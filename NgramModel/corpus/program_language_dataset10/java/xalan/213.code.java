package org.apache.xalan.trace;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.XPath;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Node;
public class SelectionEvent implements java.util.EventListener
{
  public final ElemTemplateElement m_styleNode;
  public final TransformerImpl m_processor;
  public final Node m_sourceNode;
  public final String m_attributeName;
  public final XPath m_xpath;
  public final XObject m_selection;
  public SelectionEvent(TransformerImpl processor, Node sourceNode,
                        ElemTemplateElement styleNode, String attributeName,
                        XPath xpath, XObject selection)
  {
    this.m_processor = processor;
    this.m_sourceNode = sourceNode;
    this.m_styleNode = styleNode;
    this.m_attributeName = attributeName;
    this.m_xpath = xpath;
    this.m_selection = selection;
  }
}
