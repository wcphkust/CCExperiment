package org.apache.xpath.functions;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;
public class FuncNumber extends FunctionDef1Arg
{
    static final long serialVersionUID = 7266745342264153076L;
  public XObject execute(XPathContext xctxt) throws javax.xml.transform.TransformerException
  {
    return new XNumber(getArg0AsNumber(xctxt));
  }
}