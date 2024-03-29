package org.apache.xerces.impl;
import java.io.EOFException;
import java.io.IOException;
import java.util.Locale;
import org.apache.xerces.impl.io.UCSReader;
import org.apache.xerces.impl.msg.XMLMessageFormatter;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLChar;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
public class XMLEntityScanner implements XMLLocator {
    private static final boolean DEBUG_ENCODINGS = false;
    private static final boolean DEBUG_BUFFER = false;
    private static final EOFException END_OF_DOCUMENT_ENTITY = new EOFException() {
        private static final long serialVersionUID = 980337771224675268L;
        public Throwable fillInStackTrace() {
            return this;
        }
    };
    private XMLEntityManager fEntityManager = null;
    protected XMLEntityManager.ScannedEntity fCurrentEntity = null;
    protected SymbolTable fSymbolTable = null;
    protected int fBufferSize = XMLEntityManager.DEFAULT_BUFFER_SIZE;
    protected XMLErrorReporter fErrorReporter;
    public XMLEntityScanner() {
    } 
    public final String getBaseSystemId() {
        return (fCurrentEntity != null && fCurrentEntity.entityLocation != null) ? fCurrentEntity.entityLocation.getExpandedSystemId() : null;
    } 
    public final void setEncoding(String encoding) throws IOException {
        if (DEBUG_ENCODINGS) {
            System.out.println("$$$ setEncoding: "+encoding);
        }
        if (fCurrentEntity.stream != null) {
            if (fCurrentEntity.encoding == null ||
                !fCurrentEntity.encoding.equals(encoding)) {
                if(fCurrentEntity.encoding != null && fCurrentEntity.encoding.startsWith("UTF-16")) {
                    String ENCODING = encoding.toUpperCase(Locale.ENGLISH);
                    if(ENCODING.equals("UTF-16")) return;
                    if(ENCODING.equals("ISO-10646-UCS-4")) {
                        if(fCurrentEntity.encoding.equals("UTF-16BE")) {
                            fCurrentEntity.reader = new UCSReader(fCurrentEntity.stream, UCSReader.UCS4BE);
                        } else {
                            fCurrentEntity.reader = new UCSReader(fCurrentEntity.stream, UCSReader.UCS4LE);
                        }
                        return;
                    }
                    if(ENCODING.equals("ISO-10646-UCS-2")) {
                        if(fCurrentEntity.encoding.equals("UTF-16BE")) {
                            fCurrentEntity.reader = new UCSReader(fCurrentEntity.stream, UCSReader.UCS2BE);
                        } else {
                            fCurrentEntity.reader = new UCSReader(fCurrentEntity.stream, UCSReader.UCS2LE);
                        }
                        return;
                    }
                }
                if (DEBUG_ENCODINGS) {
                    System.out.println("$$$ creating new reader from stream: "+
                                    fCurrentEntity.stream);
                }
                fCurrentEntity.setReader(fCurrentEntity.stream, encoding, null);
                fCurrentEntity.encoding = encoding;
            } else {
                if (DEBUG_ENCODINGS)
                    System.out.println("$$$ reusing old reader on stream");
            }
        }
    } 
    public final void setXMLVersion(String xmlVersion) {
        fCurrentEntity.xmlVersion = xmlVersion;
    } 
    public final boolean isExternal() {
        return fCurrentEntity.isExternal();
    } 
    public int peekChar() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(peekChar: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int c = fCurrentEntity.ch[fCurrentEntity.position];
        if (DEBUG_BUFFER) {
            System.out.print(")peekChar: ");
            XMLEntityManager.print(fCurrentEntity);
            if (fCurrentEntity.isExternal()) {
                System.out.println(" -> '"+(c!='\r'?(char)c:'\n')+"'");
            }
            else {
                System.out.println(" -> '"+(char)c+"'");
            }
        }
        if (fCurrentEntity.isExternal()) {
            return c != '\r' ? c : '\n';
        }
        else {
            return c;
        }
    } 
    public int scanChar() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanChar: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int c = fCurrentEntity.ch[fCurrentEntity.position++];
        boolean external = false;
        if (c == '\n' ||
            (c == '\r' && (external = fCurrentEntity.isExternal()))) {
            fCurrentEntity.lineNumber++;
            fCurrentEntity.columnNumber = 1;
            if (fCurrentEntity.position == fCurrentEntity.count) {
                fCurrentEntity.ch[0] = (char)c;
                load(1, false);
            }
            if (c == '\r' && external) {
                if (fCurrentEntity.ch[fCurrentEntity.position++] != '\n') {
                    fCurrentEntity.position--;
                }
                c = '\n';
            }
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanChar: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> '"+(char)c+"'");
        }
        fCurrentEntity.columnNumber++;
        return c;
    } 
    public String scanNmtoken() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanNmtoken: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int offset = fCurrentEntity.position;
        while (XMLChar.isName(fCurrentEntity.ch[fCurrentEntity.position])) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                int length = fCurrentEntity.position - offset;
                if (length == fCurrentEntity.ch.length) {
                    char[] tmp = new char[fCurrentEntity.ch.length << 1];
                    System.arraycopy(fCurrentEntity.ch, offset,
                                     tmp, 0, length);
                    fCurrentEntity.ch = tmp;
                }
                else {
                    System.arraycopy(fCurrentEntity.ch, offset,
                                     fCurrentEntity.ch, 0, length);
                }
                offset = 0;
                if (load(length, false)) {
                    break;
                }
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;
        String symbol = null;
        if (length > 0) {
            symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length);
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanNmtoken: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> "+String.valueOf(symbol));
        }
        return symbol;
    } 
    public String scanName() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanName: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int offset = fCurrentEntity.position;
        if (XMLChar.isNameStart(fCurrentEntity.ch[offset])) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                fCurrentEntity.ch[0] = fCurrentEntity.ch[offset];
                offset = 0;
                if (load(1, false)) {
                    fCurrentEntity.columnNumber++;
                    String symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1);
                    if (DEBUG_BUFFER) {
                        System.out.print(")scanName: ");
                        XMLEntityManager.print(fCurrentEntity);
                        System.out.println(" -> "+String.valueOf(symbol));
                    }
                    return symbol;
                }
            }
            while (XMLChar.isName(fCurrentEntity.ch[fCurrentEntity.position])) {
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    if (length == fCurrentEntity.ch.length) {
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;
        String symbol = null;
        if (length > 0) {
            symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length);
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanName: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> "+String.valueOf(symbol));
        }
        return symbol;
    } 
    public String scanNCName() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanNCName: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int offset = fCurrentEntity.position;
        if (XMLChar.isNCNameStart(fCurrentEntity.ch[offset])) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                fCurrentEntity.ch[0] = fCurrentEntity.ch[offset];
                offset = 0;
                if (load(1, false)) {
                    fCurrentEntity.columnNumber++;
                    String symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1);
                    if (DEBUG_BUFFER) {
                        System.out.print(")scanNCName: ");
                        XMLEntityManager.print(fCurrentEntity);
                        System.out.println(" -> "+String.valueOf(symbol));
                    }
                    return symbol;
                }
            }
            while (XMLChar.isNCName(fCurrentEntity.ch[fCurrentEntity.position])) {
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    if (length == fCurrentEntity.ch.length) {
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    offset = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length;
        String symbol = null;
        if (length > 0) {
            symbol = fSymbolTable.addSymbol(fCurrentEntity.ch, offset, length);
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanNCName: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> "+String.valueOf(symbol));
        }
        return symbol;
    } 
    public boolean scanQName(QName qname) throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanQName, "+qname+": ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int offset = fCurrentEntity.position;
        if (XMLChar.isNCNameStart(fCurrentEntity.ch[offset])) {
            if (++fCurrentEntity.position == fCurrentEntity.count) {
                fCurrentEntity.ch[0] = fCurrentEntity.ch[offset];
                offset = 0;
                if (load(1, false)) {
                    fCurrentEntity.columnNumber++;
                    String name =
                        fSymbolTable.addSymbol(fCurrentEntity.ch, 0, 1);
                    qname.setValues(null, name, name, null);
                    if (DEBUG_BUFFER) {
                        System.out.print(")scanQName, "+qname+": ");
                        XMLEntityManager.print(fCurrentEntity);
                        System.out.println(" -> true");
                    }
                    return true;
                }
            }
            int index = -1;
            while (XMLChar.isName(fCurrentEntity.ch[fCurrentEntity.position])) {
                char c = fCurrentEntity.ch[fCurrentEntity.position];
                if (c == ':') {
                    if (index != -1) {
                        break;
                    }
                    index = fCurrentEntity.position;
                }
                if (++fCurrentEntity.position == fCurrentEntity.count) {
                    int length = fCurrentEntity.position - offset;
                    if (length == fCurrentEntity.ch.length) {
                        char[] tmp = new char[fCurrentEntity.ch.length << 1];
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         tmp, 0, length);
                        fCurrentEntity.ch = tmp;
                    }
                    else {
                        System.arraycopy(fCurrentEntity.ch, offset,
                                         fCurrentEntity.ch, 0, length);
                    }
                    if (index != -1) {
                        index = index - offset;
                    }
                    offset = 0;
                    if (load(length, false)) {
                        break;
                    }
                }
            }
            int length = fCurrentEntity.position - offset;
            fCurrentEntity.columnNumber += length;
            if (length > 0) {
                String prefix = null;
                String localpart = null;
                String rawname = fSymbolTable.addSymbol(fCurrentEntity.ch,
                                                        offset, length);
                if (index != -1) {
                    int prefixLength = index - offset;
                    prefix = fSymbolTable.addSymbol(fCurrentEntity.ch,
                                                    offset, prefixLength);
                    int len = length - prefixLength - 1;
                    int startLocal = index +1;
                    if (!XMLChar.isNCNameStart(fCurrentEntity.ch[startLocal])){
                        fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                                                 "IllegalQName",
                                                  null,
                                                  XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                    localpart = fSymbolTable.addSymbol(fCurrentEntity.ch,
                                                       startLocal, len);
                }
                else {
                    localpart = rawname;
                }
                qname.setValues(prefix, localpart, rawname, null);
                if (DEBUG_BUFFER) {
                    System.out.print(")scanQName, "+qname+": ");
                    XMLEntityManager.print(fCurrentEntity);
                    System.out.println(" -> true");
                }
                return true;
            }
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanQName, "+qname+": ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> false");
        }
        return false;
    } 
    public int scanContent(XMLString content) throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanContent: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        else if (fCurrentEntity.position == fCurrentEntity.count - 1) {
            fCurrentEntity.ch[0] = fCurrentEntity.ch[fCurrentEntity.count - 1];
            load(1, false);
            fCurrentEntity.position = 0;
            fCurrentEntity.startPosition = 0;
        }
        int offset = fCurrentEntity.position;
        int c = fCurrentEntity.ch[offset];
        int newlines = 0;
        boolean external = fCurrentEntity.isExternal();
        if (c == '\n' || (c == '\r' && external)) {
            if (DEBUG_BUFFER) {
                System.out.print("[newline, "+offset+", "+fCurrentEntity.position+": ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println();
            }
            do {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if (c == '\r' && external) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    if (fCurrentEntity.ch[fCurrentEntity.position] == '\n') {
                        fCurrentEntity.position++;
                        offset++;
                    }
                    else {
                        newlines++;
                    }
                }
                else if (c == '\n') {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.position--;
                    break;
                }
            } while (fCurrentEntity.position < fCurrentEntity.count - 1);
            for (int i = offset; i < fCurrentEntity.position; i++) {
                fCurrentEntity.ch[i] = '\n';
            }
            int length = fCurrentEntity.position - offset;
            if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                content.setValues(fCurrentEntity.ch, offset, length);
                if (DEBUG_BUFFER) {
                    System.out.print("]newline, "+offset+", "+fCurrentEntity.position+": ");
                    XMLEntityManager.print(fCurrentEntity);
                    System.out.println();
                }
                return -1;
            }
            if (DEBUG_BUFFER) {
                System.out.print("]newline, "+offset+", "+fCurrentEntity.position+": ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println();
            }
        }
        while (fCurrentEntity.position < fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position++];
            if (!XMLChar.isContent(c)) {
                fCurrentEntity.position--;
                break;
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length - newlines;
        content.setValues(fCurrentEntity.ch, offset, length);
        if (fCurrentEntity.position != fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position];
            if (c == '\r' && external) {
                c = '\n';
            }
        }
        else {
            c = -1;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanContent: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> '"+(char)c+"'");
        }
        return c;
    } 
    public int scanLiteral(int quote, XMLString content)
        throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanLiteral, '"+(char)quote+"': ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        else if (fCurrentEntity.position == fCurrentEntity.count - 1) {
            fCurrentEntity.ch[0] = fCurrentEntity.ch[fCurrentEntity.count - 1];
            load(1, false);
            fCurrentEntity.position = 0;
            fCurrentEntity.startPosition = 0;
        }
        int offset = fCurrentEntity.position;
        int c = fCurrentEntity.ch[offset];
        int newlines = 0;
        boolean external = fCurrentEntity.isExternal();
        if (c == '\n' || (c == '\r' && external)) {
            if (DEBUG_BUFFER) {
                System.out.print("[newline, "+offset+", "+fCurrentEntity.position+": ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println();
            }
            do {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if (c == '\r' && external) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    if (fCurrentEntity.ch[fCurrentEntity.position] == '\n') {
                        fCurrentEntity.position++;
                        offset++;
                    }
                    else {
                        newlines++;
                    }
                }
                else if (c == '\n') {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.position--;
                    break;
                }
            } while (fCurrentEntity.position < fCurrentEntity.count - 1);
            for (int i = offset; i < fCurrentEntity.position; i++) {
                fCurrentEntity.ch[i] = '\n';
            }
            int length = fCurrentEntity.position - offset;
            if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                content.setValues(fCurrentEntity.ch, offset, length);
                if (DEBUG_BUFFER) {
                    System.out.print("]newline, "+offset+", "+fCurrentEntity.position+": ");
                    XMLEntityManager.print(fCurrentEntity);
                    System.out.println();
                }
                return -1;
            }
            if (DEBUG_BUFFER) {
                System.out.print("]newline, "+offset+", "+fCurrentEntity.position+": ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println();
            }
        }
        while (fCurrentEntity.position < fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position++];
            if ((c == quote &&
                 (!fCurrentEntity.literal || external))
                || c == '%' || !XMLChar.isContent(c)) {
                fCurrentEntity.position--;
                break;
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length - newlines;
        content.setValues(fCurrentEntity.ch, offset, length);
        if (fCurrentEntity.position != fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position];
            if (c == quote && fCurrentEntity.literal) {
                c = -1;
            }
        }
        else {
            c = -1;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")scanLiteral, '"+(char)quote+"': ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> '"+(char)c+"'");
        }
        return c;
    } 
    public boolean scanData(String delimiter, XMLStringBuffer buffer)
        throws IOException {
        boolean found = false;
        int delimLen = delimiter.length();
        char charAt0 = delimiter.charAt(0);
        boolean external = fCurrentEntity.isExternal();
        if (DEBUG_BUFFER) {
            System.out.print("(scanData: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        boolean bNextEntity = false;
        while ((fCurrentEntity.position > fCurrentEntity.count - delimLen)
            && (!bNextEntity))
        {
          System.arraycopy(fCurrentEntity.ch,
                           fCurrentEntity.position,
                           fCurrentEntity.ch,
                           0,
                           fCurrentEntity.count - fCurrentEntity.position);
          bNextEntity = load(fCurrentEntity.count - fCurrentEntity.position, false);
          fCurrentEntity.position = 0;
          fCurrentEntity.startPosition = 0;
        }
        if (fCurrentEntity.position > fCurrentEntity.count - delimLen) {
            int length = fCurrentEntity.count - fCurrentEntity.position;
            buffer.append (fCurrentEntity.ch, fCurrentEntity.position, length); 
            fCurrentEntity.columnNumber += fCurrentEntity.count;
            fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
            fCurrentEntity.position = fCurrentEntity.count;
            fCurrentEntity.startPosition = fCurrentEntity.count;
            load(0,true);
            return false;
        }
        int offset = fCurrentEntity.position;
        int c = fCurrentEntity.ch[offset];
        int newlines = 0;
        if (c == '\n' || (c == '\r' && external)) {
            if (DEBUG_BUFFER) {
                System.out.print("[newline, "+offset+", "+fCurrentEntity.position+": ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println();
            }
            do {
                c = fCurrentEntity.ch[fCurrentEntity.position++];
                if (c == '\r' && external) {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                    if (fCurrentEntity.ch[fCurrentEntity.position] == '\n') {
                        fCurrentEntity.position++;
                        offset++;
                    }
                    else {
                        newlines++;
                    }
                }
                else if (c == '\n') {
                    newlines++;
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        offset = 0;
                        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
                        fCurrentEntity.position = newlines;
                        fCurrentEntity.startPosition = newlines;
                        fCurrentEntity.count = newlines;
                        if (load(newlines, false)) {
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.position--;
                    break;
                }
            } while (fCurrentEntity.position < fCurrentEntity.count - 1);
            for (int i = offset; i < fCurrentEntity.position; i++) {
                fCurrentEntity.ch[i] = '\n';
            }
            int length = fCurrentEntity.position - offset;
            if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                buffer.append(fCurrentEntity.ch, offset, length);
                if (DEBUG_BUFFER) {
                    System.out.print("]newline, "+offset+", "+fCurrentEntity.position+": ");
                    XMLEntityManager.print(fCurrentEntity);
                    System.out.println();
                }
                return true;
            }
            if (DEBUG_BUFFER) {
                System.out.print("]newline, "+offset+", "+fCurrentEntity.position+": ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println();
            }
        }
        OUTER: while (fCurrentEntity.position < fCurrentEntity.count) {
            c = fCurrentEntity.ch[fCurrentEntity.position++];
            if (c == charAt0) {
                int delimOffset = fCurrentEntity.position - 1;
                for (int i = 1; i < delimLen; i++) {
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        fCurrentEntity.position -= i;
                        break OUTER;
                    }
                    c = fCurrentEntity.ch[fCurrentEntity.position++];
                    if (delimiter.charAt(i) != c) {
                        fCurrentEntity.position--;
                        break;
                    }
                }
                if (fCurrentEntity.position == delimOffset + delimLen) {
                    found = true;
                    break;
                }
            }
            else if (c == '\n' || (external && c == '\r')) {
                fCurrentEntity.position--;
                break;
            }
            else if (XMLChar.isInvalid(c)) {
                fCurrentEntity.position--;
                int length = fCurrentEntity.position - offset;
                fCurrentEntity.columnNumber += length - newlines;
                buffer.append(fCurrentEntity.ch, offset, length); 
                return true;
            }
        }
        int length = fCurrentEntity.position - offset;
        fCurrentEntity.columnNumber += length - newlines;
        if (found) {
            length -= delimLen;
        }
        buffer.append (fCurrentEntity.ch, offset, length);
        if (DEBUG_BUFFER) {
            System.out.print(")scanData: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> " + !found);
        }
        return !found;
    } 
    public boolean skipChar(int c) throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipChar, '"+(char)c+"': ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int cc = fCurrentEntity.ch[fCurrentEntity.position];
        if (cc == c) {
            fCurrentEntity.position++;
            if (c == '\n') {
                fCurrentEntity.lineNumber++;
                fCurrentEntity.columnNumber = 1;
            }
            else {
                fCurrentEntity.columnNumber++;
            }
            if (DEBUG_BUFFER) {
                System.out.print(")skipChar, '"+(char)c+"': ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println(" -> true");
            }
            return true;
        }
        else if (c == '\n' && cc == '\r' && fCurrentEntity.isExternal()) {
            if (fCurrentEntity.position == fCurrentEntity.count) {
                fCurrentEntity.ch[0] = (char)cc;
                load(1, false);
            }
            fCurrentEntity.position++;
            if (fCurrentEntity.ch[fCurrentEntity.position] == '\n') {
                fCurrentEntity.position++;
            }
            fCurrentEntity.lineNumber++;
            fCurrentEntity.columnNumber = 1;
            if (DEBUG_BUFFER) {
                System.out.print(")skipChar, '"+(char)c+"': ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println(" -> true");
            }
            return true;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipChar, '"+(char)c+"': ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> false");
        }
        return false;
    } 
    public boolean skipSpaces() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipSpaces: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int c = fCurrentEntity.ch[fCurrentEntity.position];
        if (XMLChar.isSpace(c)) {
            boolean external = fCurrentEntity.isExternal();
            do {
                boolean entityChanged = false;
                if (c == '\n' || (external && c == '\r')) {
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                        fCurrentEntity.ch[0] = (char)c;
                        entityChanged = load(1, true);
                        if (!entityChanged) {
                            fCurrentEntity.position = 0;
                            fCurrentEntity.startPosition = 0;
                        }
                    }
                    if (c == '\r' && external) {
                        if (fCurrentEntity.ch[++fCurrentEntity.position] != '\n') {
                            fCurrentEntity.position--;
                        }
                    }
                }
                else {
                    fCurrentEntity.columnNumber++;
                }
                if (!entityChanged)
                    fCurrentEntity.position++;
                if (fCurrentEntity.position == fCurrentEntity.count) {
                    load(0, true);
                }
            } while (XMLChar.isSpace(c = fCurrentEntity.ch[fCurrentEntity.position]));
            if (DEBUG_BUFFER) {
                System.out.print(")skipSpaces: ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println(" -> true");
            }
            return true;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipSpaces: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> false");
        }
        return false;
    } 
    public final boolean skipDeclSpaces() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipDeclSpaces: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        int c = fCurrentEntity.ch[fCurrentEntity.position];
        if (XMLChar.isSpace(c)) {
            boolean external = fCurrentEntity.isExternal();
            do {
                boolean entityChanged = false;
                if (c == '\n' || (external && c == '\r')) {
                    fCurrentEntity.lineNumber++;
                    fCurrentEntity.columnNumber = 1;
                    if (fCurrentEntity.position == fCurrentEntity.count - 1) {
                        fCurrentEntity.ch[0] = (char)c;
                        entityChanged = load(1, true);
                        if (!entityChanged) {
                            fCurrentEntity.position = 0;
                            fCurrentEntity.startPosition = 0;
                        }
                    }
                    if (c == '\r' && external) {
                        if (fCurrentEntity.ch[++fCurrentEntity.position] != '\n') {
                            fCurrentEntity.position--;
                        }
                    }
                }
                else {
                    fCurrentEntity.columnNumber++;
                }
                if (!entityChanged)
                    fCurrentEntity.position++;
                if (fCurrentEntity.position == fCurrentEntity.count) {
                    load(0, true);
                }
            } while (XMLChar.isSpace(c = fCurrentEntity.ch[fCurrentEntity.position]));
            if (DEBUG_BUFFER) {
                System.out.print(")skipDeclSpaces: ");
                XMLEntityManager.print(fCurrentEntity);
                System.out.println(" -> true");
            }
            return true;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipDeclSpaces: ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> false");
        }
        return false;
    } 
    public boolean skipString(String s) throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipString, \""+s+"\": ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        if (fCurrentEntity.position == fCurrentEntity.count) {
            load(0, true);
        }
        final int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = fCurrentEntity.ch[fCurrentEntity.position++];
            if (c != s.charAt(i)) {
                fCurrentEntity.position -= i + 1;
                if (DEBUG_BUFFER) {
                    System.out.print(")skipString, \""+s+"\": ");
                    XMLEntityManager.print(fCurrentEntity);
                    System.out.println(" -> false");
                }
                return false;
            }
            if (i < length - 1 && fCurrentEntity.position == fCurrentEntity.count) {
                System.arraycopy(fCurrentEntity.ch, fCurrentEntity.count - i - 1, fCurrentEntity.ch, 0, i + 1);
                if (load(i + 1, false)) {
                    fCurrentEntity.startPosition -= i + 1; 
                    fCurrentEntity.position -= i + 1;
                    if (DEBUG_BUFFER) {
                        System.out.print(")skipString, \""+s+"\": ");
                        XMLEntityManager.print(fCurrentEntity);
                        System.out.println(" -> false");
                    }
                    return false;
                }
            }
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipString, \""+s+"\": ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println(" -> true");
        }
        fCurrentEntity.columnNumber += length;
        return true;
    } 
    public final String getPublicId() {
        return (fCurrentEntity != null && fCurrentEntity.entityLocation != null) ? fCurrentEntity.entityLocation.getPublicId() : null;
    } 
    public final String getExpandedSystemId() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.entityLocation != null &&
                    fCurrentEntity.entityLocation.getExpandedSystemId() != null ) {
                return fCurrentEntity.entityLocation.getExpandedSystemId();
            }
            else {
                return fCurrentEntity.getExpandedSystemId();
            }
        }
        return null;
    } 
    public final String getLiteralSystemId() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.entityLocation != null &&
                    fCurrentEntity.entityLocation.getLiteralSystemId() != null ) {
                return fCurrentEntity.entityLocation.getLiteralSystemId();
            }
            else {
                return fCurrentEntity.getLiteralSystemId();
            }
        }
        return null;
    } 
    public final int getLineNumber() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.lineNumber;
            }
            else {
                return fCurrentEntity.getLineNumber();
            }
        }
        return -1;
    } 
    public final int getColumnNumber() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.columnNumber;
            }
            else {
                return fCurrentEntity.getColumnNumber();
            }
        }
        return -1;
    } 
    public final int getCharacterOffset() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.baseCharOffset + (fCurrentEntity.position - fCurrentEntity.startPosition);
            }
            else {
                return fCurrentEntity.getCharacterOffset();
            }
        }
        return -1;
    } 
    public final String getEncoding() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.encoding;
            }
            else {
                return fCurrentEntity.getEncoding();
            }
        }
        return null;
    } 
    public final String getXMLVersion() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.xmlVersion;
            }
            else {
                return fCurrentEntity.getXMLVersion();
            }
        }
        return null;
    } 
    public final void setCurrentEntity(XMLEntityManager.ScannedEntity ent) {
        fCurrentEntity = ent;
    }
    public final void setBufferSize(int size) {
        fBufferSize = size;
    }
    public final void reset(SymbolTable symbolTable, XMLEntityManager entityManager,
                        XMLErrorReporter reporter) {
        fCurrentEntity = null;
        fSymbolTable = symbolTable;
        fEntityManager = entityManager;
        fErrorReporter = reporter;
    }
    final boolean load(int offset, boolean changeEntity)
        throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(load, "+offset+": ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        fCurrentEntity.baseCharOffset += (fCurrentEntity.position - fCurrentEntity.startPosition);
        int length = fCurrentEntity.ch.length - offset;
        if (!fCurrentEntity.mayReadChunks && length > XMLEntityManager.DEFAULT_XMLDECL_BUFFER_SIZE) {
            length = XMLEntityManager.DEFAULT_XMLDECL_BUFFER_SIZE;
        }
        if (DEBUG_BUFFER) System.out.println("  length to try to read: "+length);
        int count = fCurrentEntity.reader.read(fCurrentEntity.ch, offset, length);
        if (DEBUG_BUFFER) System.out.println("  length actually read:  "+count);
        boolean entityChanged = false;
        if (count != -1) {
            if (count != 0) {
                fCurrentEntity.count = count + offset;
                fCurrentEntity.position = offset;
                fCurrentEntity.startPosition = offset;
            }
        }
        else {
            fCurrentEntity.count = offset;
            fCurrentEntity.position = offset;
            fCurrentEntity.startPosition = offset;
            entityChanged = true;
            if (changeEntity) {
                fEntityManager.endEntity();
                if (fCurrentEntity == null) {
                    throw END_OF_DOCUMENT_ENTITY;
                }
                if (fCurrentEntity.position == fCurrentEntity.count) {
                    load(0, true);
                }
            }
        }
        if (DEBUG_BUFFER) {
            System.out.print(")load, "+offset+": ");
            XMLEntityManager.print(fCurrentEntity);
            System.out.println();
        }
        return entityChanged;
    } 
} 
