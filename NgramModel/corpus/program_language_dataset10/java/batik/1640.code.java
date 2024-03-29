package org.apache.batik.transcoder.image;
import java.util.HashMap;
import java.util.Map;
import org.apache.batik.transcoder.TranscoderInput;
public class LanguageTest extends AbstractImageTranscoderTest {
    protected String inputURI;
    protected String refImageURI;
    protected String language;
    public LanguageTest(String inputURI, 
                        String refImageURI, 
                        String language) {
        this.inputURI = inputURI;
        this.refImageURI = refImageURI;
        this.language = language;
    }
    protected TranscoderInput createTranscoderInput() {
        return new TranscoderInput(resolveURL(inputURI).toString());
    }
    protected Map createTranscodingHints() {
        Map hints = new HashMap(7);
        hints.put(ImageTranscoder.KEY_LANGUAGE, language);
        return hints;
    }
    protected byte [] getReferenceImageData() {
        return createBufferedImageData(resolveURL(refImageURI));
    }
}
