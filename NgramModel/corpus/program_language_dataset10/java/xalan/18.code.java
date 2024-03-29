package servlet;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import javax.xml.transform.OutputKeys;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.StylesheetRoot;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.Locator;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.apache.xalan.processor.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.XMLFilterImpl;
public class ApplyXSLT extends HttpServlet
{
  protected ApplyXSLTProperties ourDefaultParameters = null;
  public final static String EOL = System.getProperty("line.separator");
  public final static String FS = System.getProperty("file.separator");
  public final static String ROOT = System.getProperty("server.root");
  public static String CURRENTDIR;
  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);
    if (ROOT != null){
      CURRENTDIR= getServletContext().getRealPath("/WEB-INF/classes/servlet/") + FS;
	  System.out.println ( CURRENTDIR);}
    else
      CURRENTDIR = System.getProperty("user.dir")+ FS;
	setDefaultParameters(config);
    setMediaProps(config.getInitParameter("mediaURL"));	
  }
  protected void setDefaultParameters(ServletConfig config)
  {
    ourDefaultParameters = new DefaultApplyXSLTProperties(config);
  }
  protected void setMediaProps(String mediaURLstring)
  {
    if (mediaURLstring != null)
    {
      URL url = null;
      try
      {
        url = new URL(mediaURLstring);
      }
      catch (MalformedURLException mue1)
      {
        try
        {
          url = new URL("file", "", CURRENTDIR + mediaURLstring);
        }
        catch (MalformedURLException mue2)
        {
          writeLog("Unable to find the media properties file based on parameter 'mediaURL' = "
                   + mediaURLstring, HttpServletResponse.SC_ACCEPTED, mue2);
          url = null;
        }
      }
      if (url != null)
      {
        try
        {
          ourMediaProps = new OrderedProps(url.openStream());
        }
        catch (IOException ioe1)
        {
          writeLog("Exception occurred while opening media properties file: " + mediaURLstring +
                   ".  Media table may be invalid.", HttpServletResponse.SC_ACCEPTED, ioe1);
        }
      }
    }
    else
    {
      String defaultProp = CURRENTDIR + "media.properties";
      try
      {
        ourMediaProps = new OrderedProps(new FileInputStream(defaultProp));
      }
      catch (IOException ioe2)
      {
        writeLog("Default media properties file " + defaultProp + " not found.",
                 HttpServletResponse.SC_ACCEPTED, ioe2);
      }
    }
  }
  public String getMedia(HttpServletRequest request)
  {
    return ourMediaProps.getValue(request.getHeader(HEADER_NAME));
  }
  public void doGet (HttpServletRequest request,
                     HttpServletResponse response)
    throws ServletException, IOException
  {
    try
    {	
      TransformerFactory tFactory = TransformerFactory.newInstance();
      process(tFactory, request, response);
    }
    catch (Exception e)
    {
    }
  }
  public void process(TransformerFactory tFactory, 
					  HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException, SAXException
  {
    boolean debug = ourDefaultParameters.isDebug(request);
    long time = 0;
    if (debug)
      time = System.currentTimeMillis();
    ApplyXSLTListener listener = new ApplyXSLTListener();
	listener.out.println("debug is " + debug);
    StreamSource xmlSource = null;
	StreamSource xslSource = null;
    try
    {
      if ((xmlSource = getDocument(request, listener)) == null)
        throw new ApplyXSLTException("getDocument() returned null",
                                     new NullPointerException(),
                                     response.SC_NOT_FOUND);
    }
    catch (ApplyXSLTException axe)
    {
      axe.appendMessage(EOL + "getDocument() resulted in ApplyXSLTException" + EOL
                        + listener.getMessage());
      if (debug) writeLog(axe);
      displayException(response, axe, debug);
      xmlSource = null;
    }
    if (xmlSource != null)
	{
      try
      {
	    if ((xslSource = getStylesheet(tFactory, request, xmlSource, listener)) == null)
		{
          throw new ApplyXSLTException("getStylesheet() returned null",
                                       new NullPointerException(),
                                       response.SC_NOT_FOUND);
        }
		xmlSource = getDocument(request, listener); 
      }
      catch (ApplyXSLTException axe)
      {
        axe.appendMessage(EOL + "getStylesheet() resulted in ApplyXSLTException" + EOL
                          + listener.getMessage());
        if (debug) writeLog(axe);
        displayException(response, axe, debug);
        xslSource = null;
      }
    if ((xmlSource != null) && (xslSource != null))
    {
	  try
	  {
        listener.out.println("Performing transformation...");
        Templates templates = tFactory.newTemplates(xslSource);
        Transformer transformer = templates.newTransformer();
        {
          try
          {
            String contentType = null;
			      contentType = getContentType(templates);
            if (contentType != null);
              response.setContentType(contentType);
			      if (transformer instanceof TransformerImpl)
			      {
			        TransformerImpl transformerImpl = (TransformerImpl)transformer;
              transformerImpl.setQuietConflictWarnings(ourDefaultParameters.isNoCW(request));
			      }
			      setStylesheetParams(transformer, request);			
	          transformer.transform(xmlSource, new StreamResult(response.getOutputStream()));
			      if (debug)              
              writeLog(listener.getMessage(), response.SC_OK);
          }
          catch (Exception exc)
          {
            ApplyXSLTException axe = new ApplyXSLTException
				                     ("Exception occurred during Transformation:"
                                          + EOL + listener.getMessage() + EOL
                                          + exc.getMessage(), 
									              exc,
                                response.SC_INTERNAL_SERVER_ERROR);
            if (debug) writeLog(axe);
            displayException(response, axe, debug);
          }
          finally
          {
          } 
		}
	  }
      catch (Exception saxExc)
      {
        ApplyXSLTException axe = new ApplyXSLTException
			                     ("Exception occurred during ctor/Transformation:"
                                             + EOL + listener.getMessage() + EOL
                                             + saxExc.getMessage(), 
			                					  saxExc,
                                  response.SC_INTERNAL_SERVER_ERROR);
        if (debug) writeLog(axe);
        displayException(response, axe, debug);
      } 
    } 
    if (debug)
    {
      time = System.currentTimeMillis() - time;
      writeLog("  No Conflict Warnings = " + ourDefaultParameters.isNoCW(request) +
               "  Transformation time: " + time + " ms", response.SC_OK);
    }
  }
  }  
  protected StreamSource getDocument(HttpServletRequest request,
                                     ApplyXSLTListener listener)
    throws ApplyXSLTException
  {
    try
    {
      String xmlURL = null;
      if ((xmlURL = request.getPathInfo()) != null)
      {
        listener.out.println("Parsing XML Document from PathInfo: " + xmlURL);
        return new StreamSource(new URL("http", ((DefaultApplyXSLTProperties)
                                         ourDefaultParameters).getLocalHost(),
                                         request.getServerPort(),
                                         xmlURL.replace('\\', '/')).openStream());                          
      }
      if ((xmlURL = ourDefaultParameters.getXMLurl(request)) != null)
      {
        listener.out.println("Parsing XML Document from request parameter: " + xmlURL);
        return new StreamSource(new URL(xmlURL).openStream());
      }
      String contentType = request.getContentType();
      if ((contentType != null) && contentType.startsWith("text/xml"))
      {
        listener.out.println("Parsing XML Document from request chain");
        return new StreamSource(request.getInputStream());
      }
    }
    catch (IOException ioe)
    {
      throw new ApplyXSLTException(ioe, HttpServletResponse.SC_NOT_FOUND);
    }
    catch (Exception e)
    {
      throw new ApplyXSLTException(e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    return null;
  }
  protected StreamSource getStylesheet(TransformerFactory tFactory,
				   		  			   HttpServletRequest request,
                                       StreamSource xmlSource,
                                       ApplyXSLTListener listener)
    throws ApplyXSLTException
  {
    try
    {
      String xslURL = ((DefaultApplyXSLTProperties) ourDefaultParameters).getXSLRequestURL(request);
      if (xslURL != null)
        listener.out.println("Parsing XSL Stylesheet Document from request parameter: "
                             + xslURL);
      else
      {
        if (xmlSource != null){
          listener.out.println("calling getXSLURLfromDoc and getMedia " + getMedia(request) );
          xslURL = getXSLURLfromDoc(xmlSource, STYLESHEET_ATTRIBUTE, getMedia(request), tFactory);
        }
        if (xslURL != null)
          listener.out.println("Parsing XSL Stylesheet Document from XML Document tag: " + xslURL);
        else
          if ((xslURL = ourDefaultParameters.getXSLurl(null)) != null)
            listener.out.println("Parsing XSL Stylesheet Document from configuration: " + xslURL);
      }
      return new StreamSource(xslURL);
    }
    catch (IOException ioe)
    {
      throw new ApplyXSLTException(ioe, HttpServletResponse.SC_NOT_FOUND);
    }
    catch (Exception e)
    {
      throw new ApplyXSLTException(e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
  public String getContentType(Templates templates)
  {
    Properties oprops = templates.getOutputProperties();
    String encoding = oprops.getProperty(OutputKeys.ENCODING);  
          String media = oprops.getProperty(OutputKeys.MEDIA_TYPE);
          if (media != null)
          {
      if (encoding != null)
        return media + "; charset=" + encoding;
      return media;
          }
          else
          {
            String method = oprops.getProperty(OutputKeys.METHOD);
            if (method.equals("html"))
                    return "text/html";
            else if (method.equals("text"))
                    return "text/plain";
            else 
                    return "text/xml";
          }
  }  
  public void setStylesheetParams(Transformer transformer, HttpServletRequest request)
  {
    Enumeration paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements())
    {
      String paramName = (String) paramNames.nextElement();
      try
      {
        String[] paramVals = request.getParameterValues(paramName);
        if (paramVals != null)
            transformer.setParameter(paramName, new XString(paramVals[0]));
      }
      catch (Exception e)
      {
      }
    }
    try
    {
      transformer.setParameter("servlet-RemoteAddr", new XString(request.getRemoteAddr()));
    }
    catch (Exception e)
    {
    }
    try
    {
      transformer.setParameter("servlet-RemoteHost", new XString(request.getRemoteHost()));
    }
    catch (Exception e)
    {
    }
    try
    {
      transformer.setParameter("servlet-RemoteUser", new XString(request.getRemoteUser()));
    }
    catch (Exception e)
    {
    }
  }
  protected void writeLog(ApplyXSLTException axe)
  {
    writeLog(axe.getMessage(), axe.getStatusCode(), axe.getException());
  }
  protected void writeLog(String msg, int statusCode, Throwable t)
  {
    if (t == null)
      writeLog(msg, statusCode);
    else
    {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      PrintWriter writer = new PrintWriter(bytes, true);
      System.out.println("Exception is " + t.getClass().getName());
      t.printStackTrace(writer);
      log("HTTP Status Code: " + statusCode + " - " + msg + EOL + bytes.toString());
    }
  }
  protected void writeLog(String msg, int statusCode)
  {
    log("HTTP Status Code: " + statusCode + " - " + msg);
  }
  protected void displayException(HttpServletResponse response, ApplyXSLTException xse, boolean debug)
  {
    String mesg = xse.getMessage();
    if (mesg == null)
      mesg = "";
    else mesg = "<B>" + mesg + "</B>";
    StringTokenizer tokens = new StringTokenizer(mesg, EOL);
    StringBuffer strBuf = new StringBuffer();
    while (tokens.hasMoreTokens())
      strBuf.append(tokens.nextToken() + EOL + "<BR>");
    mesg = strBuf.toString();
    if (debug)
    {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      PrintWriter writer = new PrintWriter(bytes, true);
      xse.getException().printStackTrace(writer);
      mesg += " <PRE> " + bytes.toString() + " </PRE> ";
    }
    response.setContentType("text/html");
    try
    {
      response.sendError(xse.getStatusCode(), mesg);
    }
    catch (IOException ioe)
    {
      System.err.println("IOException is occurring when sendError is called");
    }
  }
  protected OrderedProps ourMediaProps = null;
  protected URLConnection toAcceptLanguageConnection(URL url, HttpServletRequest request)
    throws Exception
  {
    URLConnection tempConnection = url.openConnection();
    tempConnection.setRequestProperty("Accept-Language", request.getHeader("Accept-Language"));
    return tempConnection;
  }
  public static String getXSLURLfromDoc(StreamSource xmlSource,
                                        String attributeName,
                                        String attributeValue,
                                        TransformerFactory tFactory)
  {
    String tempURL = null, returnURL = null;
    try
    {
	  DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
      Node sourceTree = docBuilder.parse(xmlSource.getInputStream());
      for(Node child=sourceTree.getFirstChild(); null != child; child=child.getNextSibling())
      {
        if(Node.PROCESSING_INSTRUCTION_NODE == child.getNodeType())
        {
          ProcessingInstruction pi = (ProcessingInstruction)child;
          if(pi.getNodeName().equals("xml-stylesheet"))
          {
            PIA pia = new PIA(pi);
            if("text/xsl".equals(pia.getAttribute("type")))
            {
              tempURL = pia.getAttribute("href");
              String attribute = pia.getAttribute(attributeName);
              if ((attribute != null) && (attribute.indexOf(attributeValue) > -1))
                return tempURL;
              if (!"yes".equals(pia.getAttribute("alternate")))
                returnURL = tempURL;
            }
          }
        }
      }
    }
    catch(Exception saxExc)
    {
    }
    return returnURL;
  }  
  protected static final String STYLESHEET_ATTRIBUTE = "media";
  protected static final String HEADER_NAME = "user-Agent";
}
class OrderedProps
{
  private Vector attVec = new Vector(15);
  OrderedProps(InputStream inputStream)
    throws IOException
  {
    BufferedReader input  = new BufferedReader(new InputStreamReader(inputStream));
    String currentLine, Key = null;
    StringTokenizer currentTokens;
    while ((currentLine = input.readLine()) != null)
    {
      currentTokens = new StringTokenizer(currentLine, "=\t\r\n");
      if (currentTokens.hasMoreTokens()) Key = currentTokens.nextToken().trim();
      if ((Key != null) && !Key.startsWith("#") && currentTokens.hasMoreTokens())
      {
        String temp[] = new String[2];
        temp[0] = Key; temp[1] = currentTokens.nextToken().trim();
        attVec.addElement(temp);
      }
    }
  }
  String getValue(String s)
  {
    int i, j = attVec.size();
    for (i = 0; i < j; i++)
    {
      String temp[] = (String[]) attVec.elementAt(i);
      if (s.indexOf(temp[0]) > -1)
        return temp[1];
    }
    return "unknown";
  }
}
class PIA
{
  private Hashtable piAttributes = null;
  PIA(ProcessingInstruction pi)
  {
    piAttributes = new Hashtable();
    StringTokenizer tokenizer = new StringTokenizer(pi.getNodeValue(), "=\"");
    while(tokenizer.hasMoreTokens())
    {
      piAttributes.put(tokenizer.nextToken().trim(), tokenizer.nextToken().trim());
    }
  }
  String getAttribute(String name)
  {
    return (String) piAttributes.get(name);
  }  
}
