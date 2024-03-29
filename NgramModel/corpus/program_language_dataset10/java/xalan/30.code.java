import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.SAXException;
public class JAXPTransletOneTransformation
{
  public static void main(String argv[])
          throws TransformerException, TransformerConfigurationException, IOException, SAXException,
                 ParserConfigurationException, FileNotFoundException
  { 
    String key = "javax.xml.transform.TransformerFactory";
    String value = "org.apache.xalan.xsltc.trax.TransformerFactoryImpl";
    Properties props = System.getProperties();
    props.put(key, value);
    System.setProperties(props);    
    String xslInURI = "todo.xsl";
    String xmlInURI = "todo.xml";
    String htmlOutURI = "todo.html";
    try
    {
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer(new StreamSource(xslInURI));
      transformer.transform(new StreamSource(xmlInURI),
                            new StreamResult(new FileOutputStream(htmlOutURI)));  
      System.out.println("Produced todo.html");  
    }
    catch (Exception e) 
    {
     System.out.println(e.toString());
     e.printStackTrace();
    }      
  }
}
