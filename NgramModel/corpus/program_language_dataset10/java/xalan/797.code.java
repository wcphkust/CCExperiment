package org.apache.xpath;
import java.util.Vector;
import org.apache.xpath.functions.FuncExtFunction;
public interface ExtensionsProvider
{
  public boolean functionAvailable(String ns, String funcName)
          throws javax.xml.transform.TransformerException;
  public boolean elementAvailable(String ns, String elemName)
          throws javax.xml.transform.TransformerException;
  public Object extFunction(String ns, String funcName, 
                            Vector argVec, Object methodKey)
            throws javax.xml.transform.TransformerException;
  public Object extFunction(FuncExtFunction extFunction, 
                            Vector argVec)
            throws javax.xml.transform.TransformerException;
}
