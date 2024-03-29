package org.apache.batik.parser;
import java.io.IOException;
public class NumberListParser extends NumberParser {
    protected NumberListHandler numberListHandler;
    public NumberListParser() {
        numberListHandler = DefaultNumberListHandler.INSTANCE;
    }
    public void setNumberListHandler(NumberListHandler handler) {
        numberListHandler = handler;
    }
    public NumberListHandler getNumberListHandler() {
        return numberListHandler;
    }
    protected void doParse() throws ParseException, IOException {
        numberListHandler.startNumberList();
        current = reader.read();
        skipSpaces();
        try {
            for (;;) {
                numberListHandler.startNumber();
                float f = parseFloat();
                numberListHandler.numberValue(f);
                numberListHandler.endNumber();
                skipCommaSpaces();
                if (current == -1) {
                    break;
                }
            }
        } catch (NumberFormatException e) {
            reportUnexpectedCharacterError( current );
        }
        numberListHandler.endNumberList();
    }
}
