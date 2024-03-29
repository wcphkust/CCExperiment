import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.xalan.extensions.XPathFunctionResolverImpl;
import org.apache.xalan.extensions.ExtensionNamespaceContext;
import org.xml.sax.InputSource;
public class ExtensionFunctionResolver
{
    public static final String EXPR1 = "math:max(/doc/num)";
    public static final String EXPR2 = "java:ExtensionTest.test('Bob')";
    public static void main(String[] args) throws Exception
    {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new ExtensionNamespaceContext());
        xpath.setXPathFunctionResolver(new XPathFunctionResolverImpl());
        Object result = null;
        InputSource context = new InputSource("numlist.xml");
        result = xpath.evaluate(EXPR1, context, XPathConstants.NUMBER);
        System.out.println(EXPR1 + " = " + result);
        result = xpath.evaluate(EXPR2, context, XPathConstants.STRING);
        System.out.println(EXPR2 + " = " + result);
    }
}
