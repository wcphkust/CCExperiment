package org.apache.batik.apps.tiledTranscoder;
import java.awt.image.renderable.*;
import java.awt.image.*;
import java.io.*;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.*;
import org.apache.batik.ext.awt.image.*;
import org.apache.batik.ext.awt.image.codec.*;
import org.apache.batik.ext.awt.image.codec.tiff.*;
import org.apache.batik.ext.awt.image.rendered.*;
import org.apache.batik.ext.awt.image.renderable.*;
import org.w3c.dom.Document;
public class TiledImageTranscoder extends SVGAbstractTranscoder {
    protected TiledImageTranscoder() {
    }
    protected void transcode(Document document,
                             String uri,
                             TranscoderOutput output)
            throws TranscoderException {
        super.transcode(document, uri, output);
        Filter f = this.root.getGraphicsNodeRable(true);
        RenderContext rc = new RenderContext(curTxf, null, null);
        RenderedImage img = f.createRendering(rc);
        int w = img.getWidth();
        int h = img.getHeight();
        try {
            int bands = img.getSampleModel().getNumBands();
            int [] off = new int[bands];
            for (int i=0; i<bands; i++)
                off[i] = i;
            SampleModel sm = new PixelInterleavedSampleModel
                (DataBuffer.TYPE_BYTE, 
                 w, (100000+w-1)/w, 
                 bands, w*bands, off);
            RenderedImage rimg = new FormatRed(GraphicsUtil.wrap(img), sm);
            TIFFImageEncoder enc = new TIFFImageEncoder
                (output.getOutputStream(), null);
            enc.encode(rimg);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    public static void main (String [] args) {
        try {
            FileOutputStream fos = new FileOutputStream(args[1]);
            TiledImageTranscoder tit = new TiledImageTranscoder();
            tit.addTranscodingHint(KEY_WIDTH, new Float(10240));
            tit.transcode(new TranscoderInput("file:" + args[0]), 
                          new TranscoderOutput(fos));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (TranscoderException te) {
            te.printStackTrace();
        }
    }
}
