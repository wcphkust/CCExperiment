package org.apache.xalan.transformer;
import java.util.Vector;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.XMLString;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNodeSet;
public class KeyManager
{
  private transient Vector m_key_tables = null;
  public XNodeSet getNodeSetDTMByKey(
          XPathContext xctxt, int doc, QName name, XMLString ref, PrefixResolver nscontext)
            throws javax.xml.transform.TransformerException
  {
    XNodeSet nl = null;
    ElemTemplateElement template = (ElemTemplateElement) nscontext;  
    if ((null != template)
            && null != template.getStylesheetRoot().getKeysComposed())
    {
      boolean foundDoc = false;
      if (null == m_key_tables)
      {
        m_key_tables = new Vector(4);
      }
      else
      {
        int nKeyTables = m_key_tables.size();
        for (int i = 0; i < nKeyTables; i++)
        {
          KeyTable kt = (KeyTable) m_key_tables.elementAt(i);
          if (kt.getKeyTableName().equals(name) && doc == kt.getDocKey())
          {
            nl = kt.getNodeSetDTMByKey(name, ref);
            if (nl != null)
            {
              foundDoc = true;
              break;
            }
          }
        }
      }
      if ((null == nl) &&!foundDoc )
      {
        KeyTable kt =
          new KeyTable(doc, nscontext, name,
                       template.getStylesheetRoot().getKeysComposed(),
                       xctxt);
        m_key_tables.addElement(kt);
        if (doc == kt.getDocKey())
        {
          foundDoc = true;
          nl = kt.getNodeSetDTMByKey(name, ref);
        }
      }
    }
    return nl;
  }
}