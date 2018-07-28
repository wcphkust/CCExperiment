package org.apache.xml.utils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
public class ListingErrorHandler implements ErrorHandler, ErrorListener
{
    protected PrintWriter m_pw = null;
    public ListingErrorHandler(PrintWriter pw)
    {
        if (null == pw)
            throw new NullPointerException(XMLMessages.createXMLMessage(XMLErrorResources.ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER, null));
        m_pw = pw;
    }
    public ListingErrorHandler()
    {
        m_pw = new PrintWriter(System.err, true);
    }
    public void warning (SAXParseException exception)
    	throws SAXException
    {
    	logExceptionLocation(m_pw, exception);
        m_pw.println("warning: " + exception.getMessage());
        m_pw.flush();
        if (getThrowOnWarning())
            throw exception;
    }
    public void error (SAXParseException exception)
    	throws SAXException
    {
    	logExceptionLocation(m_pw, exception);
        m_pw.println("error: " + exception.getMessage());
        m_pw.flush();
        if (getThrowOnError())
            throw exception;
    }
    public void fatalError (SAXParseException exception)
    	throws SAXException
    {
    	logExceptionLocation(m_pw, exception);
        m_pw.println("fatalError: " + exception.getMessage());
        m_pw.flush();
        if (getThrowOnFatalError())
            throw exception;
    }
    public void warning(TransformerException exception)
        throws TransformerException
    {
    	logExceptionLocation(m_pw, exception);
        m_pw.println("warning: " + exception.getMessage());
        m_pw.flush();
        if (getThrowOnWarning())
            throw exception;
    }
    public void error(TransformerException exception)
        throws TransformerException
    {
    	logExceptionLocation(m_pw, exception);
        m_pw.println("error: " + exception.getMessage());
        m_pw.flush();
        if (getThrowOnError())
            throw exception;
    }
    public void fatalError(TransformerException exception)
        throws TransformerException
    {
    	logExceptionLocation(m_pw, exception);
        m_pw.println("error: " + exception.getMessage());
        m_pw.flush();
        if (getThrowOnError())
            throw exception;
    }
    public static void logExceptionLocation(PrintWriter pw, Throwable exception)
    {
        if (null == pw)
            pw = new PrintWriter(System.err, true);
        SourceLocator locator = null;
        Throwable cause = exception;
        do
        {
            if(cause instanceof SAXParseException)
            {
                locator = new SAXSourceLocator((SAXParseException)cause);
            }
            else if (cause instanceof TransformerException)
            {
                SourceLocator causeLocator = ((TransformerException)cause).getLocator();
                if(null != causeLocator)
                {
                    locator = causeLocator;
                }
            }
            if(cause instanceof TransformerException)
                cause = ((TransformerException)cause).getCause();
            else if(cause instanceof WrappedRuntimeException)
                cause = ((WrappedRuntimeException)cause).getException();
            else if(cause instanceof SAXException)
                cause = ((SAXException)cause).getException();
            else
                cause = null;
        }
        while(null != cause);
        if(null != locator)
        {
            String id = (locator.getPublicId() != locator.getPublicId())
                      ? locator.getPublicId()
                        : (null != locator.getSystemId())
                          ? locator.getSystemId() : "SystemId-Unknown";
            pw.print(id + ":Line=" + locator.getLineNumber()
                             + ";Column=" + locator.getColumnNumber()+": ");
            pw.println("exception:" + exception.getMessage());
            pw.println("root-cause:" 
                       + ((null != cause) ? cause.getMessage() : "null"));
            logSourceLine(pw, locator); 
        }
        else
        {
            pw.print("SystemId-Unknown:locator-unavailable: ");
            pw.println("exception:" + exception.getMessage());
            pw.println("root-cause:" 
                       + ((null != cause) ? cause.getMessage() : "null"));
        }
    }
    public static void logSourceLine(PrintWriter pw, SourceLocator locator)
    {
        if (null == locator)
            return;
        if (null == pw)
            pw = new PrintWriter(System.err, true);
        String url = locator.getSystemId();
        if (null == url)
        {
            pw.println("line: (No systemId; cannot read file)");
            pw.println();
            return;
        }
        try
        {
            int line = locator.getLineNumber();
            int column = locator.getColumnNumber();
            pw.println("line: " + getSourceLine(url, line));
            StringBuffer buf = new StringBuffer("line: ");
            for (int i = 1; i < column; i++)
            {
                buf.append(' ');
            }
            buf.append('^');
            pw.println(buf.toString());
        }
        catch (Exception e)
        {
            pw.println("line: logSourceLine unavailable due to: " + e.getMessage());
            pw.println();
        }
    }
    protected static String getSourceLine(String sourceUrl, int lineNum)
            throws Exception
    {
        URL url = null;
        try
        {
            url = new URL(sourceUrl);
        }
        catch (java.net.MalformedURLException mue)
        {
            int indexOfColon = sourceUrl.indexOf(':');
            int indexOfSlash = sourceUrl.indexOf('/');
            if ((indexOfColon != -1)
                && (indexOfSlash != -1)
                && (indexOfColon < indexOfSlash))
            {
                throw mue;
            }
            else
            {
                url = new URL(SystemIDResolver.getAbsoluteURI(sourceUrl));
            }
        }
        String line = null;
        InputStream is = null;
        BufferedReader br = null;
        try
        {
            URLConnection uc = url.openConnection();
            is = uc.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            for (int i = 1; i <= lineNum; i++)
            {
                line = br.readLine();
            }
        } 
        finally
        {
            br.close();
            is.close();
        }
        return line;
    }    
    public void setThrowOnWarning(boolean b)
    {
        throwOnWarning = b;
    }
    public boolean getThrowOnWarning()
    {
        return throwOnWarning;
    }
    protected boolean throwOnWarning = false;
    public void setThrowOnError(boolean b)
    {
        throwOnError = b;
    }
    public boolean getThrowOnError()
    {
        return throwOnError;
    }
    protected boolean throwOnError = true;
    public void setThrowOnFatalError(boolean b)
    {
        throwOnFatalError = b;
    }
    public boolean getThrowOnFatalError()
    {
        return throwOnFatalError;
    }
    protected boolean throwOnFatalError = true;
}