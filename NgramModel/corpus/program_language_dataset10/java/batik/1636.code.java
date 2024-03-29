package org.apache.batik.transcoder.image;
import java.util.HashMap;
import java.util.Map;
import org.apache.batik.transcoder.TranscoderInput;
public class DimensionTest extends AbstractImageTranscoderTest {
    protected String inputURI;
    protected String refImageURI;
    protected Float width;
    protected Float height;
    public DimensionTest(String inputURI, 
                         String refImageURI, 
                         Float width,
                         Float height) {
        this.inputURI = inputURI;
        this.refImageURI = refImageURI;
        this.width = width;
        this.height = height;
    }
    protected TranscoderInput createTranscoderInput() {
        return new TranscoderInput(resolveURL(inputURI).toString());
    }
    protected Map createTranscodingHints() {
        Map hints = new HashMap(7);
        if (width.floatValue() > 0) {
            hints.put(ImageTranscoder.KEY_WIDTH, width);
        }
        if (height.floatValue() > 0) {
            hints.put(ImageTranscoder.KEY_HEIGHT, height);
        }
        return hints;
    }
    protected byte [] getReferenceImageData() {
        return createBufferedImageData(resolveURL(refImageURI));
    }
}
