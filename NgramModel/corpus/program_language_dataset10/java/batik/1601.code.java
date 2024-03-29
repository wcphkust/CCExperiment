package org.apache.batik.test.svg;
import java.awt.Graphics2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Iterator;
import org.apache.batik.test.DefaultTestReport;
import org.apache.batik.test.TestReport;
import org.apache.batik.swing.JSVGCanvasHandler;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.Overlay;
import java.awt.image.BufferedImage;
public class JSVGRenderingAccuracyTest extends SamplesRenderingTest 
       implements JSVGCanvasHandler.Delegate {
    public static final String ERROR_SAVE_FAILED = 
        "JSVGRenderingAccuracyTest.message.error.save.failed";
    public static String fmt(String key, Object []args) {
        return Messages.formatMessage(key, args);
    }
    public JSVGRenderingAccuracyTest(){
    }
    protected URL srcURL;
    protected FileOutputStream fos;
    protected TestReport failReport = null;
    protected boolean done;
    protected JSVGCanvasHandler handler = null;
    public JSVGCanvasHandler createCanvasHandler() {
        return new JSVGCanvasHandler(this, this);
    }
    public TestReport encode(URL srcURL, FileOutputStream fos) {
        this.srcURL = srcURL;
        this.fos    = fos;
        handler = createCanvasHandler();
        done = false;
        handler.runCanvas(srcURL.toString());
        handler = null;
        if (failReport != null) return failReport;
        DefaultTestReport report = new DefaultTestReport(this);
        report.setPassed(true);
        return report;
    }
    public void scriptDone() {
        synchronized (this) {
            done = true;
            handler.scriptDone();
        }
    }
    public boolean canvasInit(JSVGCanvas canvas) {
        canvas.setURI(srcURL.toString());
        return true;
    }
    public void canvasLoaded(JSVGCanvas canvas) {
    }
    public void canvasRendered(JSVGCanvas canvas) {
    }
    public boolean canvasUpdated(JSVGCanvas canvas) {
        synchronized (this) {
            return done;
        }
    }
    public void canvasDone(JSVGCanvas canvas) {
        synchronized (this) {
            done = true;
            if (failReport != null)
                return;
            try {
                BufferedImage theImage = copyImage(canvas.getOffScreen());
                List overlays = canvas.getOverlays();
                Graphics2D g = theImage.createGraphics();
                Iterator it = overlays.iterator();
                while (it.hasNext()) {
                    ((Overlay)it.next()).paint(g);
                }
                saveImage(theImage, fos);
            } catch (IOException ioe) {
                StringWriter trace = new StringWriter();
                ioe.printStackTrace(new PrintWriter(trace));
                DefaultTestReport report = new DefaultTestReport(this);
                report.setErrorCode(ERROR_SAVE_FAILED);
                report.setDescription(new TestReport.Entry[] { 
                    new TestReport.Entry
                    (fmt(ENTRY_KEY_ERROR_DESCRIPTION, null),
                     fmt(ERROR_SAVE_FAILED, 
                         new Object[]{ srcURL.toString(),
                                       trace.toString()}))
                });
                report.setPassed(false);
                failReport = report;
            }
        }
    }
    public void failure(TestReport report) {
        synchronized (this) {
            done = true;
            failReport = report;
        }
    }
    public static BufferedImage copyImage(BufferedImage bi) {
        BufferedImage ret;
        ret = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
        bi.copyData(ret.getRaster());
        return ret;
    }
}
