package org.apache.batik.swing;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.ref.WeakReference;
import javax.swing.JFrame;
import org.apache.batik.test.DefaultTestReport;
import org.apache.batik.test.Test;
import org.apache.batik.test.TestReport;
import org.apache.batik.bridge.ScriptingEnvironment;
import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.bridge.UpdateManagerEvent;
import org.apache.batik.bridge.UpdateManagerListener;
import org.apache.batik.script.Interpreter;
import org.apache.batik.script.InterpreterException;
import org.apache.batik.util.RunnableQueue;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.swing.svg.SVGLoadEventDispatcherAdapter;
import org.apache.batik.swing.svg.SVGLoadEventDispatcherEvent;
import org.apache.batik.swing.svg.SVGLoadEventDispatcher;
public class JSVGCanvasHandler {
    public interface Delegate {
        String getName();
        boolean canvasInit(JSVGCanvas canvas);
        void canvasLoaded(JSVGCanvas canvas);
        void canvasRendered(JSVGCanvas canvas);
        boolean canvasUpdated(JSVGCanvas canvas);
        void canvasDone(JSVGCanvas canvas);
        void failure(TestReport report);
    }
    public static final String REGARD_TEST_INSTANCE = "regardTestInstance";
    public static final String REGARD_START_SCRIPT =
        "try { regardStart(); } catch (er) {}";
    public static final String ERROR_CANNOT_LOAD_SVG =
        "JSVGCanvasHandler.message.error.could.not.load.svg";
    public static final String ERROR_SVG_RENDER_FAILED =
        "JSVGCanvasHandler.message.error.svg.render.failed";
    public static final String ERROR_SVG_UPDATE_FAILED =
        "JSVGCanvasHandler.message.error.svg.update.failed";
    public static final String ENTRY_KEY_ERROR_DESCRIPTION
        = "JSVGCanvasHandler.entry.key.error.description";
    public static String fmt(String key, Object []args) {
        return TestMessages.formatMessage(key, args);
    }
    JFrame     frame = null;
    JSVGCanvas canvas = null;
    WeakReference updateManager = null;
    WindowListener wl = null;
    InitialRenderListener irl = null;
    LoadListener ll = null;
    SVGLoadEventListener sll = null;
    UpdateRenderListener url = null;
    boolean failed;
    boolean abort = false;
    boolean done  = false;
    final Object loadMonitor = new Object();
    final Object renderMonitor = new Object();
    Delegate delegate;
    Test host;
    String desc;
    public JSVGCanvasHandler(Test host, Delegate delegate) {
        this.host     = host;
        this.delegate = delegate;
    }
    public JFrame getFrame()     { return frame; }
    public JSVGCanvas getCanvas() { return canvas; }
    public JSVGCanvas createCanvas() { return new JSVGCanvas(); }
    public void runCanvas(String desc) {
        this.desc = desc;
        setupCanvas();
        if ( abort) return;
        try {
            synchronized (renderMonitor) {
                synchronized (loadMonitor) {
                    if (delegate.canvasInit(canvas)) {
                        checkLoad();
                    }
                }
                if ( abort) return;
                checkRender();
                if ( abort) return;
                if (updateManager == null || updateManager.get() == null)
                    return;
                while (!done) {
                    checkUpdate();
                    if ( abort) return;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            delegate.canvasDone(canvas);
            dispose();
        }
    }
    public void setupCanvas() {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        frame = new JFrame(delegate.getName());
                        canvas = createCanvas();
                        canvas.setPreferredSize(new Dimension(450, 500));
                        frame.getContentPane().add(canvas);
                        frame.pack();
                        wl = new WindowAdapter() {
                                public void windowClosing(WindowEvent e) {
                                    synchronized (loadMonitor) {
                                        abort = true;
                                        loadMonitor.notifyAll();
                                    }
                                    synchronized (renderMonitor) {
                                        abort = true;
                                        renderMonitor.notifyAll();
                                    }
                                }
                            };
                        frame.addWindowListener(wl);
                        frame.setVisible(true);
                        irl = new InitialRenderListener();
                        canvas.addGVTTreeRendererListener(irl);
                        ll = new LoadListener();
                        canvas.addSVGDocumentLoaderListener(ll);
                        sll = new SVGLoadEventListener();
                        canvas.addSVGLoadEventDispatcherListener(sll);
                    }});
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public void scriptDone() {
        Runnable r = new Runnable() {
                public void run() {
                    UpdateManager um = getUpdateManager();
                    if (um != null)
                        um.forceRepaint();
                    synchronized(renderMonitor) {
                        done = true;
                        failed = false;
                        renderMonitor.notifyAll();
                    }
                }
            };
        UpdateManager um = getUpdateManager();
        if ((um == null) ||
            (!um.isRunning())){
            Thread t = new Thread(r);
            t.start();
        } else {
            um.getUpdateRunnableQueue().invokeLater(r);
        }
    }
    public void dispose() {
        if (frame != null) {
            frame.removeWindowListener(wl);
            frame.setVisible(false);
        }
        wl = null;
        if (canvas != null) {
            canvas.removeGVTTreeRendererListener(irl);  irl=null;
            canvas.removeSVGDocumentLoaderListener(ll); ll=null;
            canvas.removeUpdateManagerListener(url);    url=null;
        }
        updateManager = null;
        canvas = null;
        frame = null;
    }
    public void checkSomething(Object monitor, String errorCode) {
        synchronized (monitor) {
            failed = true;
            try { monitor.wait(); }
            catch(InterruptedException ie) {  }
            if (abort || failed) {
                DefaultTestReport report = new DefaultTestReport(host);
                report.setErrorCode(errorCode);
                report.setDescription(new TestReport.Entry[] {
                    new TestReport.Entry
                    (fmt(ENTRY_KEY_ERROR_DESCRIPTION, null),
                     fmt(errorCode, new Object[]{desc}))
                });
                report.setPassed(false);
                delegate.failure(report);
                done = true;
                return;
            }
        }
    }
    public void checkLoad() {
        checkSomething(loadMonitor, ERROR_CANNOT_LOAD_SVG);
        delegate.canvasLoaded(canvas);
    }
    public void checkRender() {
        checkSomething(renderMonitor, ERROR_SVG_RENDER_FAILED);
        delegate.canvasRendered(canvas);
    }
    public void checkUpdate() {
        checkSomething(renderMonitor, ERROR_SVG_UPDATE_FAILED);
        if (!done)
            done = delegate.canvasUpdated(canvas);
    }
    public void bindHost() {
        UpdateManager um = getUpdateManager();
        RunnableQueue rq;
        rq = um.getUpdateRunnableQueue();
        rq.invokeLater(new Runnable() {
                UpdateManager um = getUpdateManager();
                public void run() {
                    ScriptingEnvironment scriptEnv;
                    scriptEnv = um.getScriptingEnvironment();
                    Interpreter interp;
                    interp    = scriptEnv.getInterpreter();
                    interp.bindObject(REGARD_TEST_INSTANCE,
                                      host);
                    try {
                        interp.evaluate(REGARD_START_SCRIPT);
                    } catch (InterpreterException ie) {
                    }
                }
            });
    }
    protected UpdateManager getUpdateManager() {
        if (updateManager != null) {
            return (UpdateManager) updateManager.get();
        }
        return null;
    }
    class UpdateRenderListener implements UpdateManagerListener {
        public void updateCompleted(UpdateManagerEvent e) {
            synchronized(renderMonitor){
                failed = false;
                renderMonitor.notifyAll();
            }
        }
        public void updateFailed(UpdateManagerEvent e) {
            synchronized(renderMonitor){
                renderMonitor.notifyAll();
            }
        }
        public void managerStarted(UpdateManagerEvent e) {
          bindHost();
        }
        public void managerSuspended(UpdateManagerEvent e) { }
        public void managerResumed(UpdateManagerEvent e) { }
        public void managerStopped(UpdateManagerEvent e) { }
        public void updateStarted(UpdateManagerEvent e) { }
    }
    class InitialRenderListener extends GVTTreeRendererAdapter {
        public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
            synchronized(renderMonitor){
                failed = false;
                renderMonitor.notifyAll();
            }
        }
        public void gvtRenderingCancelled(GVTTreeRendererEvent e) {
            synchronized(renderMonitor){
                renderMonitor.notifyAll();
            }
        }
        public void gvtRenderingFailed(GVTTreeRendererEvent e) {
            synchronized(renderMonitor){
                renderMonitor.notifyAll();
            }
        }
    }
    class LoadListener extends SVGDocumentLoaderAdapter {
        public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
            synchronized(loadMonitor){
                failed = false;
                loadMonitor.notifyAll();
            }
        }
        public void documentLoadingFailed(SVGDocumentLoaderEvent e) {
            synchronized(loadMonitor){
                loadMonitor.notifyAll();
            }
        }
        public void documentLoadingCancelled(SVGDocumentLoaderEvent e) {
            synchronized(loadMonitor){
                loadMonitor.notifyAll();
            }
        }
    }
    class SVGLoadEventListener extends SVGLoadEventDispatcherAdapter {
        public void svgLoadEventDispatchStarted(SVGLoadEventDispatcherEvent e){
            SVGLoadEventDispatcher dispatcher;
            dispatcher = (SVGLoadEventDispatcher)e.getSource();
            UpdateManager um = dispatcher.getUpdateManager();
            updateManager = new WeakReference(um);
            url = new UpdateRenderListener();
            um.addUpdateManagerListener(url);
        }
    }
}
