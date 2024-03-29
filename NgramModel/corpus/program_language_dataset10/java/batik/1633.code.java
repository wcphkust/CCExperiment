package org.apache.batik.transcoder.image;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.HashMap;
import org.apache.batik.transcoder.TranscoderInput;
public class AOITest extends AbstractImageTranscoderTest {
    protected String inputURI;
    protected String refImageURI;
    protected Rectangle2D aoi;
    protected Float imgWidth;
    protected Float imgHeight;
    public AOITest(String inputURI, 
                   String refImageURI, 
                   Float x,
                   Float y,
                   Float width,
                   Float height) {
        this(inputURI, 
             refImageURI, 
             x, 
             y, 
             width, 
             height, 
             new Float(-1), 
             new Float(-1));
    }
    public AOITest(String inputURI, 
                   String refImageURI, 
                   Float x,
                   Float y,
                   Float width,
                   Float height,
                   Float imgWidth,
                   Float imgHeight) {
        this.inputURI = inputURI;
        this.refImageURI = refImageURI;
        this.aoi = new Rectangle2D.Float(x.floatValue(),
                                         y.floatValue(),
                                         width.floatValue(),
                                         height.floatValue());
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
    }
    protected TranscoderInput createTranscoderInput() {
        return new TranscoderInput(resolveURL(inputURI).toString());
    }
    protected Map createTranscodingHints() {
        Map hints = new HashMap(11);
        hints.put(ImageTranscoder.KEY_AOI, aoi);
        if (imgWidth.floatValue() > 0) {
            hints.put(ImageTranscoder.KEY_WIDTH, imgWidth);
        }
        if (imgHeight.floatValue() > 0) {
            hints.put(ImageTranscoder.KEY_HEIGHT, imgHeight);
        }
        return hints;
    }
    protected byte [] getReferenceImageData() {
        return createBufferedImageData(resolveURL(refImageURI));
    }
}
