package org.apache.batik.parser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Locale;
import java.util.MissingResourceException;
import org.apache.batik.i18n.LocalizableSupport;
import org.apache.batik.util.io.NormalizingReader;
import org.apache.batik.util.io.StreamNormalizingReader;
import org.apache.batik.util.io.StringNormalizingReader;
public abstract class AbstractParser implements Parser {
    public static final String BUNDLE_CLASSNAME =
        "org.apache.batik.parser.resources.Messages";
    protected ErrorHandler errorHandler = new DefaultErrorHandler();
    protected LocalizableSupport localizableSupport =
        new LocalizableSupport(BUNDLE_CLASSNAME,
                               AbstractParser.class.getClassLoader());
    protected NormalizingReader reader;
    protected int current;
    public int getCurrent() {
        return current;
    }
    public void setLocale(Locale l) {
        localizableSupport.setLocale(l);
    }
    public Locale getLocale() {
        return localizableSupport.getLocale();
    }
    public String formatMessage(String key, Object[] args)
        throws MissingResourceException {
        return localizableSupport.formatMessage(key, args);
    }
    public void setErrorHandler(ErrorHandler handler) {
        errorHandler = handler;
    }
    public void parse(Reader r) throws ParseException {
        try {
            reader = new StreamNormalizingReader(r);
            doParse();
        } catch (IOException e) {
            errorHandler.error
                (new ParseException
                 (createErrorMessage("io.exception", null), e));
        }
    }
    public void parse(InputStream is, String enc) throws ParseException {
        try {
            reader = new StreamNormalizingReader(is, enc);
            doParse();
        } catch (IOException e) {
            errorHandler.error
                (new ParseException
                 (createErrorMessage("io.exception", null), e));
        }
    }
    public void parse(String s) throws ParseException {
        try {
            reader = new StringNormalizingReader(s);
            doParse();
        } catch (IOException e) {
            errorHandler.error
                (new ParseException
                 (createErrorMessage("io.exception", null), e));
        }
    }
    protected abstract void doParse()
        throws ParseException, IOException;
    protected void reportError(String key, Object[] args)
        throws ParseException {
        errorHandler.error(new ParseException(createErrorMessage(key, args),
                                              reader.getLine(),
                                              reader.getColumn()));
    }
    protected void reportCharacterExpectedError( char expectedChar, int currentChar ){
        reportError("character.expected",
                    new Object[] { new Character( expectedChar ),
                                   new Integer( currentChar ) });
    }
    protected void reportUnexpectedCharacterError( int currentChar ){
        reportError("character.unexpected",
                    new Object[] { new Integer( currentChar ) });
    }
    protected String createErrorMessage(String key, Object[] args) {
        try {
            return formatMessage(key, args);
        } catch (MissingResourceException e) {
            return key;
        }
    }
    protected String getBundleClassName() {
        return BUNDLE_CLASSNAME;
    }
    protected void skipSpaces() throws IOException {
        for (;;) {
            switch (current) {
            default:
                return;
            case 0x20:
            case 0x09:
            case 0x0D:
            case 0x0A:
            }
            current = reader.read();
        }
    }
    protected void skipCommaSpaces() throws IOException {
        wsp1: for (;;) {
            switch (current) {
            default:
                break wsp1;
            case 0x20:
            case 0x9:
            case 0xD:
            case 0xA:
            }
            current = reader.read();
        }
        if (current == ',') {
            wsp2: for (;;) {
                switch (current = reader.read()) {
                default:
                    break wsp2;
                case 0x20:
                case 0x9:
                case 0xD:
                case 0xA:
                }
            }
        }
    }
}
