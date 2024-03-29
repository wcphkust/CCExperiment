package org.apache.batik.gvt.renderer;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderContext;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Iterator;
import org.apache.batik.ext.awt.geom.RectListManager;
import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.ext.awt.image.PadMode;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.rendered.CachableRed;
import org.apache.batik.ext.awt.image.rendered.PadRed;
import org.apache.batik.ext.awt.image.rendered.TileCacheRed;
import org.apache.batik.ext.awt.image.rendered.TranslateRed;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.HaltingThread;
public class StaticRenderer implements ImageRenderer {
    protected GraphicsNode      rootGN;
    protected Filter            rootFilter;
    protected CachableRed       rootCR;
    protected SoftReference     lastCR;
    protected SoftReference     lastCache;
    protected boolean isDoubleBuffered = false;
    protected WritableRaster currentBaseRaster;
    protected WritableRaster currentRaster;
    protected BufferedImage  currentOffScreen;
    protected WritableRaster workingBaseRaster;
    protected WritableRaster workingRaster;
    protected BufferedImage  workingOffScreen;
    protected int offScreenWidth;
    protected int offScreenHeight;
    protected RenderingHints renderingHints;
    protected AffineTransform usr2dev;
    protected static RenderingHints defaultRenderingHints;
    static {
        defaultRenderingHints = new RenderingHints(null);
        defaultRenderingHints.put(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);
        defaultRenderingHints.put(RenderingHints.KEY_INTERPOLATION,
                                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    public StaticRenderer(RenderingHints rh,
                          AffineTransform at){
        renderingHints = new RenderingHints(null);
        renderingHints.add(rh);
        usr2dev = new AffineTransform(at);
    }
    public StaticRenderer(){
        renderingHints = new RenderingHints(null);
        renderingHints.add(defaultRenderingHints);
        usr2dev = new AffineTransform();
    }
    public void dispose() {
        rootGN     = null;
        rootFilter = null;
        rootCR     = null;
        workingOffScreen = null;
        workingBaseRaster = null;
        workingRaster = null;
        currentOffScreen = null;
        currentBaseRaster = null;
        currentRaster = null;
        renderingHints = null;
        lastCache = null;
        lastCR = null;
    }
    public void setTree(GraphicsNode rootGN){
        this.rootGN = rootGN;
        rootFilter  = null;
        rootCR      = null;
        workingOffScreen = null;
        workingRaster = null;
        currentOffScreen = null;
        currentRaster = null;
    }
    public GraphicsNode getTree(){
        return rootGN;
    }
    public void setRenderingHints(RenderingHints rh) {
        renderingHints = new RenderingHints(null);
        renderingHints.add(rh);
        rootFilter = null;
        rootCR     = null;
        workingOffScreen = null;
        workingRaster = null;
        currentOffScreen = null;
        currentRaster = null;
    }
    public RenderingHints getRenderingHints() {
        return renderingHints;
    }
    public void setTransform(AffineTransform usr2dev){
        if (this.usr2dev.equals(usr2dev))
            return;
        if(usr2dev == null)
            this.usr2dev = new AffineTransform();
        else
            this.usr2dev = new AffineTransform(usr2dev);
        rootCR = null;
    }
    public AffineTransform getTransform(){
        return usr2dev;
    }
    public boolean isDoubleBuffered(){
        return isDoubleBuffered;
    }
    public void setDoubleBuffered(boolean isDoubleBuffered){
        if (this.isDoubleBuffered == isDoubleBuffered)
            return;
        this.isDoubleBuffered = isDoubleBuffered;
        if (isDoubleBuffered) {
            currentOffScreen  = null;
            currentBaseRaster = null;
            currentRaster     = null;
        } else {
            currentOffScreen  = workingOffScreen;
            currentBaseRaster = workingBaseRaster;
            currentRaster     = workingRaster;
        }
    }
    public void updateOffScreen(int width, int height) {
        offScreenWidth  = width;
        offScreenHeight = height;
    }
    public BufferedImage getOffScreen() {
        if (rootGN == null)
            return null;
        return currentOffScreen;
    }
    public void clearOffScreen() {
        if (isDoubleBuffered)
            return;
        updateWorkingBuffers();
        if ((rootCR == null)           ||
            (workingBaseRaster == null))
            return;
        ColorModel     cm         = rootCR.getColorModel();
        WritableRaster syncRaster = workingBaseRaster;
        synchronized (syncRaster) {
            BufferedImage bi = new BufferedImage
                (cm, workingBaseRaster, cm.isAlphaPremultiplied(), null);
            Graphics2D g2d = bi.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
            g2d.dispose();
        }
    }
    public void repaint(Shape area) {
        if (area == null) return;
        RectListManager rlm = new RectListManager();
        rlm.add(usr2dev.createTransformedShape(area).getBounds());
        repaint(rlm);
    }
    public void repaint(RectListManager areas) {
        if (areas == null)
            return;
        CachableRed cr;
        WritableRaster syncRaster;
        WritableRaster copyRaster;
        updateWorkingBuffers();
        if ((rootCR == null)           ||
            (workingBaseRaster == null))
            return;
        cr = rootCR;
        syncRaster = workingBaseRaster;
        copyRaster = workingRaster;
        Rectangle srcR = rootCR.getBounds();
        Rectangle dstR = workingRaster.getBounds();
        if ((dstR.x < srcR.x) ||
            (dstR.y < srcR.y) ||
            (dstR.x+dstR.width  > srcR.x+srcR.width) ||
            (dstR.y+dstR.height > srcR.y+srcR.height))
            cr = new PadRed(cr, dstR, PadMode.ZERO_PAD, null);
        synchronized (syncRaster) {
            cr.copyData(copyRaster);
        }
        if (!HaltingThread.hasBeenHalted()) {
            BufferedImage tmpBI = workingOffScreen;
            workingBaseRaster = currentBaseRaster;
            workingRaster     = currentRaster;
            workingOffScreen  = currentOffScreen;
            currentRaster     = copyRaster;
            currentBaseRaster = syncRaster;
            currentOffScreen  = tmpBI;
        }
    }
    public void flush() {
        if (lastCache == null) return;
        Object o = lastCache.get();
        if (o == null) return;
        TileCacheRed tcr = (TileCacheRed)o;
        tcr.flushCache(tcr.getBounds());
    }
    public void flush(Collection areas) {
        AffineTransform at = getTransform();
        Iterator i = areas.iterator();
        while (i.hasNext()) {
            Shape s = (Shape)i.next();
            Rectangle r = at.createTransformedShape(s).getBounds();
            flush(r);
        }
    }
    public void flush(Rectangle r) {
        if (lastCache == null) return;
        Object o = lastCache.get();
        if (o == null) return;
        TileCacheRed tcr = (TileCacheRed)o;
        r = (Rectangle)r.clone();
        r.x -= Math.round((float)usr2dev.getTranslateX());
        r.y -= Math.round((float)usr2dev.getTranslateY());
        tcr.flushCache(r);
    }
    protected CachableRed setupCache(CachableRed img) {
        if ((lastCR == null) ||
            (img != lastCR.get())) {
            lastCR    = new SoftReference(img);
            lastCache = null;
        }
        Object o = null;
        if (lastCache != null)
            o = lastCache.get();
        if (o != null)
            return (CachableRed)o;
        img       = new TileCacheRed(img);
        lastCache = new SoftReference(img);
        return img;
    }
    protected CachableRed renderGNR() {
        AffineTransform at, rcAT;
        at = usr2dev;
        rcAT = new AffineTransform(at.getScaleX(), at.getShearY(),
                                   at.getShearX(), at.getScaleY(),
                                   0, 0);
        RenderContext rc = new RenderContext(rcAT, null, renderingHints);
        RenderedImage ri = rootFilter.createRendering(rc);
        if (ri == null)
            return null;
        CachableRed ret;
        ret = GraphicsUtil.wrap(ri);
        ret = setupCache(ret);
        int dx = Math.round((float)at.getTranslateX());
        int dy = Math.round((float)at.getTranslateY());
        ret = new TranslateRed(ret, ret.getMinX()+dx, ret.getMinY()+dy);
        ret = GraphicsUtil.convertTosRGB(ret);
        return ret;
    }
    protected void updateWorkingBuffers() {
        if (rootFilter == null) {
            rootFilter = rootGN.getGraphicsNodeRable(true);
            rootCR = null;
        }
        rootCR = renderGNR();
        if (rootCR == null) {
            workingRaster = null;
            workingOffScreen = null;
            workingBaseRaster = null;
            currentOffScreen = null;
            currentBaseRaster = null;
            currentRaster = null;
            return;
        }
        SampleModel sm = rootCR.getSampleModel();
        int         w  = offScreenWidth;
        int         h  = offScreenHeight;
        int tw = sm.getWidth();
        int th = sm.getHeight();
        w = (((w+tw-1)/tw)+1)*tw;
        h = (((h+th-1)/th)+1)*th;
        if ((workingBaseRaster == null) ||
            (workingBaseRaster.getWidth()  < w) ||
            (workingBaseRaster.getHeight() < h)) {
            sm = sm.createCompatibleSampleModel(w, h);
            workingBaseRaster
                = Raster.createWritableRaster(sm, new Point(0,0));
        }
        int tgx = -rootCR.getTileGridXOffset();
        int tgy = -rootCR.getTileGridYOffset();
        int xt, yt;
        if (tgx>=0) xt = tgx/tw;
        else        xt = (tgx-tw+1)/tw;
        if (tgy>=0) yt = tgy/th;
        else        yt = (tgy-th+1)/th;
        int xloc = xt*tw - tgx;
        int yloc = yt*th - tgy;
        workingRaster = workingBaseRaster.createWritableChild
          (0, 0, w, h, xloc, yloc, null);
        workingOffScreen =  new BufferedImage
          (rootCR.getColorModel(),
           workingRaster.createWritableChild (0, 0, offScreenWidth,
                                           offScreenHeight, 0, 0, null),
           rootCR.getColorModel().isAlphaPremultiplied(), null);
        if (!isDoubleBuffered) {
            currentOffScreen  = workingOffScreen;
            currentBaseRaster = workingBaseRaster;
            currentRaster     = workingRaster;
        }
    }
}
