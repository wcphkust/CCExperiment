package thread;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.apache.xerces.dom.CoreDocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.parsers.SAXParser;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
public class Test {
class InFileInfo
{
    public String fileName;
    public String fileContent; 
    int     checkSum;        
}
final int MAXINFILES = 25;
class RunInfo
{
    boolean     quiet;
    boolean     verbose;
    int         numThreads;
    boolean     validating;
    boolean     dom;
    boolean     reuseParser;
    boolean     inMemory;
    boolean     dumpOnErr;
    int         totalTime;
    int         numInputFiles;
    InFileInfo  files[] = new InFileInfo[MAXINFILES];
}
class ThreadInfo
{
    boolean    fHeartBeat;      
    int        fParses;         
    int        fThreadNum;      
    ThreadInfo() {
        fHeartBeat = false;
        fParses = 0;
        fThreadNum = -1;
    }
}
RunInfo         gRunInfo = new RunInfo();
ThreadInfo      gThreadInfo[];
class ThreadParser extends HandlerBase
{
    private int           fCheckSum;
    private SAXParser     fSAXParser;
    private DOMParser     fDOMParser;
    public void characters(char chars[], int start, int length) {
        addToCheckSum(chars, start, length);}
    public void ignorableWhitespace(char chars[], int start, int length) {
        addToCheckSum(chars, start, length);}
    public void warning(SAXParseException ex)     {
        System.err.print("*** Warning "+
                         ex.getMessage());}
    public void error(SAXParseException ex)       {
        System.err.print("*** Error "+
                         ex.getMessage());}
    public void fatalError(SAXParseException ex)  {
        System.err.print("***** Fatal error "+
                         ex.getMessage());}
ThreadParser()
{
    if (gRunInfo.dom) {
        fDOMParser = new org.apache.xerces.parsers.DOMParser();
        try {
            fDOMParser.setFeature( "http://xml.org/sax/features/validation", 
                                   gRunInfo.validating);
        }
        catch (Exception e) {}
        fDOMParser.setErrorHandler(this);
    }
    else
    {
        fSAXParser = new org.apache.xerces.parsers.SAXParser();
        try {
            fSAXParser.setFeature( "http://xml.org/sax/features/validation", 
                                   gRunInfo.validating);
        }
        catch (Exception e) {}
        fSAXParser.setDocumentHandler(this);
        fSAXParser.setErrorHandler(this);
    }
}
int parse(int fileNum)
{
    InputSource mbis = null;
    InFileInfo  fInfo = gRunInfo.files[fileNum];
    fCheckSum = 0;
    if (gRunInfo.inMemory) {
        mbis = new InputSource(new StringReader(fInfo.fileContent));
    }
    try
    {
        if (gRunInfo.dom) {
            if (gRunInfo.inMemory)
                fDOMParser.parse(mbis);
            else
                fDOMParser.parse(fInfo.fileName);
            Document doc = fDOMParser.getDocument();
            domCheckSum(doc);
            CoreDocumentImpl core = (CoreDocumentImpl) doc;
            DOMConfiguration config = core.getDomConfig();
            config.setParameter("validate", Boolean.TRUE);
            config.setParameter("schema-type", "http://www.w3.org/2001/XMLSchema");
            core.normalizeDocument();
        }
        else
        {
            if (gRunInfo.inMemory)
                fSAXParser.parse(mbis);
            else
                fSAXParser.parse(fInfo.fileName);
        }
    }
    catch (SAXException e)
    {
        String exceptionMessage = e.getMessage();
        System.err.println(" during parsing: " + fInfo.fileName +
                           " Exception message is: " + exceptionMessage);
    }
    catch (IOException e)
    {
        String exceptionMessage = e.getMessage();
        System.err.println(" during parsing: " + fInfo.fileName +
                           " Exception message is: " + exceptionMessage);
    }
    return fCheckSum;
}
private void addToCheckSum(char chars[], int start, int len)
{
    int i;
    for (i=start; i<len; i++)
        fCheckSum = fCheckSum*5 + chars[i];
}
private void addToCheckSum(String chars)
{
    int i;
    int len = chars.length();
    for (i=0; i<len; i++)
        fCheckSum = fCheckSum*5 + chars.charAt(i);
}
public void startElement(String name, AttributeList attributes)
{
    addToCheckSum(name);
    int n = attributes.getLength();
    int i;
    for (i=0; i<n; i++)
    {
        String attNam = attributes.getName(i);
        addToCheckSum(attNam);
        String attVal = attributes.getValue(i);
        addToCheckSum(attVal);
    }
}
public void domCheckSum(Node node)
{
    String        s;
    Node          child;
    NamedNodeMap  attributes;
    switch (node.getNodeType() )
    {
    case Node.ELEMENT_NODE:
        {
            s = node.getNodeName();   
            attributes = node.getAttributes();  
            int numAttributes = attributes.getLength();
            int i;
            for (i=0; i<numAttributes; i++)
                domCheckSum(attributes.item(i));
            addToCheckSum(s);  
            for (child=node.getFirstChild(); child!=null; child=child.getNextSibling())
                domCheckSum(child);
            break;
        }
    case Node.ATTRIBUTE_NODE:
        {
            s = node.getNodeName();  
            addToCheckSum(s);
            s = node.getNodeValue();  
            if (s != null)
                addToCheckSum(s);
            break;
        }
    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
        {
            s = node.getNodeValue();
            addToCheckSum(s);
            break;
        }
    case Node.ENTITY_REFERENCE_NODE:
    case Node.DOCUMENT_NODE:
        {
            for (child=node.getFirstChild(); child!=null; child=child.getNextSibling())
                domCheckSum(child);
            break;
        }
    }
}
public int reCheck()
{
    if (gRunInfo.dom) {
        fCheckSum = 0;
        Document doc = fDOMParser.getDocument();
        domCheckSum(doc);
    }
    return fCheckSum;
}
public void domPrint()
{
    System.out.println("Begin DOMPrint ...");
    if (gRunInfo.dom)
        domPrint(fDOMParser.getDocument());
    System.out.println("End DOMPrint");
}
void domPrint(Node node)
{
    String        s;
    Node          child;
    NamedNodeMap  attributes;
    switch (node.getNodeType() )
    {
    case Node.ELEMENT_NODE:
        {
            System.out.print("<");
            System.out.print(node.getNodeName());   
            attributes = node.getAttributes();  
            int numAttributes = attributes.getLength();
            int i;
            for (i=0; i<numAttributes; i++) {
                domPrint(attributes.item(i));
            }
            System.out.print(">");
            for (child=node.getFirstChild(); child!=null; child=child.getNextSibling())
                domPrint(child);
            System.out.print("</");
            System.out.print(node.getNodeName());
            System.out.print(">");
            break;
        }
    case Node.ATTRIBUTE_NODE:
        {
            System.out.print(" ");
            System.out.print(node.getNodeName());   
            System.out.print("= \"");
            System.out.print(node.getNodeValue());  
            System.out.print("\"");
            break;
        }
    case Node.TEXT_NODE:
    case Node.CDATA_SECTION_NODE:
        {
            System.out.print(node.getNodeValue());
            break;
        }
    case Node.ENTITY_REFERENCE_NODE:
    case Node.DOCUMENT_NODE:
        {
            for (child=node.getFirstChild(); child!=null; child=child.getNextSibling())
                domPrint(child);
            break;
        }
    }
}
} 
void parseCommandLine(String argv[])
{
    gRunInfo.quiet = false;               
    gRunInfo.verbose = false;
    gRunInfo.numThreads = 2;
    gRunInfo.validating = false;
    gRunInfo.dom = false;
    gRunInfo.reuseParser = false;
    gRunInfo.inMemory = false;
    gRunInfo.dumpOnErr = false;
    gRunInfo.totalTime = 0;
    gRunInfo.numInputFiles = 0;
    try             
    {
        int argnum = 0;
        int argc = argv.length;
        while (argnum < argc)
        {
            if (argv[argnum].equals("-quiet"))
                gRunInfo.quiet = true;
            else if (argv[argnum].equals("-verbose"))
                gRunInfo.verbose = true;
            else if (argv[argnum].equals("-v"))
                gRunInfo.validating = true;
            else if (argv[argnum].equals("-dom"))
                gRunInfo.dom = true;
            else if (argv[argnum].equals("-reuse"))
                gRunInfo.reuseParser = true;
            else if (argv[argnum].equals("-dump"))
                gRunInfo.dumpOnErr = true;
            else if (argv[argnum].equals("-mem"))
                gRunInfo.inMemory = true;
            else if (argv[argnum].equals("-threads"))
            {
                ++argnum;
                if (argnum >= argc)
                    throw new Exception();
                try {
                    gRunInfo.numThreads = Integer.parseInt(argv[argnum]);
                }
                catch (NumberFormatException e) {
                    throw new Exception();
                }
                if (gRunInfo.numThreads < 0)
                    throw new Exception();
            }
            else if (argv[argnum].equals("-time"))
            {
                ++argnum;
                if (argnum >= argc)
                    throw new Exception();
                try {
                    gRunInfo.totalTime = Integer.parseInt(argv[argnum]);
                }
                catch (NumberFormatException e) {
                    throw new Exception();
                }
                if (gRunInfo.totalTime < 1)
                    throw new Exception();
            }
            else  if (argv[argnum].charAt(0) == '-')
            {
                System.err.println("Unrecognized command line option. Scanning"
                                   + " \"" + argv[argnum] + "\"");
                throw new Exception();
            }
            else
            {
                gRunInfo.numInputFiles++;
                if (gRunInfo.numInputFiles >= MAXINFILES)
                {
                    System.err.println("Too many input files. Limit is "
                                       + MAXINFILES);
                    throw new Exception();
                }
                gRunInfo.files[gRunInfo.numInputFiles-1] = new InFileInfo();
                gRunInfo.files[gRunInfo.numInputFiles-1].fileName = argv[argnum];
            }
            argnum++;
        }
        if (gRunInfo.numInputFiles == 0)
        {
            System.err.println("No input XML file specified on command line.");
            throw new Exception();
        };
    }
    catch (Exception e)
    {
        System.err.print("usage: java thread.Test [-v] [-threads nnn] [-time nnn] [-quiet] [-verbose] xmlfile...\n" +
            "     -v             Use validating parser.  Non-validating is default. \n" +
            "     -dom           Use a DOM parser.  Default is SAX. \n" +
            "     -quiet         Suppress periodic status display. \n" +
            "     -verbose       Display extra messages. \n" +
            "     -reuse         Retain and reuse parser.  Default creates new for each parse.\n" +
            "     -threads nnn   Number of threads.  Default is 2. \n" +
            "     -time nnn      Total time to run, in seconds.  Default is forever.\n" +
            "     -dump          Dump DOM tree on error.\n" +
            "     -mem           Read files into memory once only, and parse them from there.\n"
            );
        System.exit(1);
    }
}
void ReadFilesIntoMemory()
{
    int     fileNum;
    InputStreamReader fileF;
    char chars[] = new char[1024];
    StringBuffer buf = new StringBuffer();
    if (gRunInfo.inMemory)
    {
        for (fileNum = 0; fileNum <gRunInfo.numInputFiles; fileNum++)
        {
            InFileInfo fInfo = gRunInfo.files[fileNum];
            buf.setLength(0);
            try {
                FileInputStream in = new FileInputStream( fInfo.fileName );
                fileF = new InputStreamReader(in);
                int len = 0;
                while ((len = fileF.read(chars, 0, chars.length)) > 0) {
                    buf.append(chars, 0, len);
                }
                fInfo.fileContent = buf.toString();
                fileF.close();
            }
            catch (FileNotFoundException e) {
                System.err.print("File not found: \"" +
                                 fInfo.fileName + "\".");
                System.exit(-1);
            }
            catch (IOException e) {
                System.err.println("Error reading file \"" +
                                   fInfo.fileName + "\".");
                System.exit(-1);
            }
        }
    }
}
class thread extends Thread {
    ThreadInfo thInfo;
    thread (ThreadInfo param) {
        thInfo = param;
    }
    public void run()
{
    ThreadParser thParser = null;
    if (gRunInfo.verbose)
        System.out.println("Thread " + thInfo.fThreadNum + ": starting");
    int docNum = gRunInfo.numInputFiles;
    while (true)
    {
        if (thParser == null)
            thParser = new ThreadParser();
        docNum++;
        if (docNum >= gRunInfo.numInputFiles)
            docNum = 0;
        InFileInfo fInfo = gRunInfo.files[docNum];
        if (gRunInfo.verbose )
            System.out.println("Thread " + thInfo.fThreadNum +
                               ": starting file " + fInfo.fileName);
        int checkSum = 0;
        checkSum = thParser.parse(docNum);
        if (checkSum != gRunInfo.files[docNum].checkSum)
        {
            System.err.println("\nThread " + thInfo.fThreadNum +
                               ": Parse Check sum error on file  \"" +
                               fInfo.fileName + "\".  Expected " +
                               fInfo.checkSum + ",  got " + checkSum);
            int secondTryCheckSum = thParser.reCheck();
            System.err.println("   Retry checksum is " + secondTryCheckSum);
            if (gRunInfo.dumpOnErr)
                thParser.domPrint();
            System.out.flush();
            System.exit(-1);
        }
        if (gRunInfo.reuseParser == false)
        {
            thParser = null;
        }
        thInfo.fHeartBeat = true;
        thInfo.fParses++;
    }
} 
} 
void run(String argv[])
{
    parseCommandLine(argv);
    ReadFilesIntoMemory();
    ThreadParser mainParser = new ThreadParser();
    int     n;
    boolean errors = false;
    int     cksum;
    for (n = 0; n < gRunInfo.numInputFiles; n++)
    {
        String fileName = gRunInfo.files[n].fileName;
        if (gRunInfo.verbose)
            System.out.print(fileName + " checksum is ");
        cksum = mainParser.parse(n);
        if (cksum == 0)
        {
            System.err.println("An error occured while initially parsing" +
                               fileName);
            errors = true;
        }
        gRunInfo.files[n].checkSum = cksum;
        if (gRunInfo.verbose )
            System.out.println(cksum);
        if (gRunInfo.dumpOnErr && errors)
            mainParser.domPrint();
    }
    if (errors)
        System.exit(1);
    if (gRunInfo.numThreads == 0)
        return;
    gThreadInfo = new ThreadInfo[gRunInfo.numThreads];
    int threadNum;
    for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
    {
        gThreadInfo[threadNum] = new ThreadInfo();
        gThreadInfo[threadNum].fThreadNum = threadNum;
        thread t = new thread(gThreadInfo[threadNum]);
        t.start();
    }
    long startTime = System.currentTimeMillis();
    long elapsedSeconds = 0;
    while (gRunInfo.totalTime == 0 || gRunInfo.totalTime > elapsedSeconds)
    {
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
        }
        if (gRunInfo.quiet == false && gRunInfo.verbose == false)
        {
            char c = '+';
            for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
            {
                if (gThreadInfo[threadNum].fHeartBeat == false)
                {
                    c = '.';
                    break;
                }
            }
            System.out.print(c);
            System.out.flush();
            if (c == '+')
                for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
                    gThreadInfo[threadNum].fHeartBeat = false;
        }
        elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
    };
    double totalParsesCompleted = 0;
    for (threadNum=0; threadNum < gRunInfo.numThreads; threadNum++)
    {
        totalParsesCompleted += gThreadInfo[threadNum].fParses;
    }
    double parsesPerMinute =
        totalParsesCompleted / (((double)gRunInfo.totalTime) / ((double)60));
    System.out.println("\n" + parsesPerMinute + " parses per minute.");
    System.exit(0);
}
static public void main(String argv[]) {
    Test test = new Test();
    test.run(argv);
}
} 
