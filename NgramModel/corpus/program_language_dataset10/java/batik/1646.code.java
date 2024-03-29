package org.apache.batik.transcoder.image;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import org.apache.batik.transcoder.TranscoderInput;
public class ReaderTest extends AbstractImageTranscoderTest {
    protected String inputURI;
    protected String refImageURI;
    public ReaderTest(String inputURI, String refImageURI) {
        this.inputURI = inputURI;
        this.refImageURI = refImageURI;
    }
    protected TranscoderInput createTranscoderInput() {
        try {
            URL url = resolveURL(inputURI);
            Reader reader = new InputStreamReader(url.openStream());
            TranscoderInput input = new TranscoderInput(reader);
            input.setURI(url.toString()); 
            return input;
        } catch (IOException ex) {
            throw new IllegalArgumentException(inputURI);
        }
    }
    protected byte [] getReferenceImageData() {
        return createBufferedImageData(resolveURL(refImageURI));
    }
}
