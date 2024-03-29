package org.apache.batik.ext.awt.image.codec.png;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.ext.awt.image.renderable.DeferRable;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.RedRable;
import org.apache.batik.ext.awt.image.rendered.Any2sRGBRed;
import org.apache.batik.ext.awt.image.rendered.CachableRed;
import org.apache.batik.ext.awt.image.rendered.FormatRed;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.ext.awt.image.spi.MagicNumberRegistryEntry;
import org.apache.batik.util.ParsedURL;
public class PNGRegistryEntry
    extends MagicNumberRegistryEntry {
    static final byte [] signature = {(byte)0x89, 80, 78, 71, 13, 10, 26, 10};
    public PNGRegistryEntry() {
        super("PNG", "png", "image/png", 0, signature);
    }
    public Filter handleStream(InputStream inIS,
                               ParsedURL   origURL,
                               boolean needRawData) {
        final DeferRable  dr  = new DeferRable();
        final InputStream is  = inIS;
        final boolean     raw = needRawData;
        final String      errCode;
        final Object []   errParam;
        if (origURL != null) {
            errCode  = ERR_URL_FORMAT_UNREADABLE;
            errParam = new Object[] {"PNG", origURL};
        } else {
            errCode  = ERR_STREAM_FORMAT_UNREADABLE;
            errParam = new Object[] {"PNG"};
        }
        Thread t = new Thread() {
                public void run() {
                    Filter filt;
                    try {
                        PNGDecodeParam param = new PNGDecodeParam();
                        param.setExpandPalette(true);
                        if (raw)
                            param.setPerformGammaCorrection(false);
                        else {
                            param.setPerformGammaCorrection(true);
                            param.setDisplayExponent(2.2f); 
                        }
                        CachableRed cr = new PNGRed(is, param);
                        dr.setBounds(new Rectangle2D.Double
                                     (0, 0, cr.getWidth(), cr.getHeight()));
                        cr = new Any2sRGBRed(cr);
                        cr = new FormatRed(cr, GraphicsUtil.sRGB_Unpre);
                        WritableRaster wr = (WritableRaster)cr.getData();
                        ColorModel cm = cr.getColorModel();
                        BufferedImage image;
                        image = new BufferedImage
                            (cm, wr, cm.isAlphaPremultiplied(), null);
                        cr = GraphicsUtil.wrap(image);
                        filt = new RedRable(cr);
                    } catch (IOException ioe) {
                        filt = ImageTagRegistry.getBrokenLinkImage
                            (PNGRegistryEntry.this, errCode, errParam);
                    } catch (ThreadDeath td) {
                        filt = ImageTagRegistry.getBrokenLinkImage
                            (PNGRegistryEntry.this, errCode, errParam);
                        dr.setSource(filt);
                        throw td;
                    } catch (Throwable t) {
                        filt = ImageTagRegistry.getBrokenLinkImage
                            (PNGRegistryEntry.this, errCode, errParam);
                    }
                    dr.setSource(filt);
                }
            };
        t.start();
        return dr;
    }
}
