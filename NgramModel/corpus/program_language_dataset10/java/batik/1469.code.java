package org.apache.batik.bridge;
import java.awt.AWTPermission;
import java.io.FilePermission;
import java.io.SerializablePermission;
import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.security.AllPermission;
import java.security.Permission;
import java.sql.SQLPermission;
import java.util.PropertyPermission;
import java.util.Vector;
import javax.sound.sampled.AudioPermission;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.script.ScriptHandler;
import org.apache.batik.script.Window;
import org.apache.batik.util.ParsedURL;
public class JarCheckPermissionsDenied implements ScriptHandler {
    public static final String svgNS = "http://www.w3.org/2000/svg";
    public static final String testNS = "http://xml.apache.org/batik/test";
    public static final String testedPath = "build.sh";
    public static final String testedHost = "nagoya.apache.org:8080";
    protected static Object[][] basePermissions = {
        {"AllPermission", new AllPermission()}, 
        {"FilePermission read", new FilePermission(testedPath, "read")}, 
        {"FilePermission write", new FilePermission(testedPath, "write")}, 
        {"FilePermission execute", new FilePermission(testedPath, "execute")}, 
        {"FilePermission delete", new FilePermission(testedPath, "delete")}, 
        {"SocketPermission accept", new SocketPermission(testedHost, "accept")}, 
        {"SocketPermission connect", new SocketPermission(testedHost, "connect")}, 
        {"SocketPermission listen", new SocketPermission(testedHost, "listen")}, 
        {"SocketPermission resolve", new SocketPermission(testedHost, "resolve")}, 
        {"AudioPermission play", new AudioPermission("play")}, 
        {"AudioPermission record", new AudioPermission("record")}, 
        {"AWTPermission accessClipboard", new AWTPermission("accessClipboard")}, 
        {"AWTPermission accessEventQueue", new AWTPermission("accessEventQueue")}, 
        {"AWTPermission listenToAllAWTEvents", new AWTPermission("listenToAllAWTEvents")}, 
        {"AWTPermission showWindowWithoutWarningBanner", new AWTPermission("showWindowWithoutWarningBanner")}, 
        {"AWTPermission readDisplayPixels", new AWTPermission("readDisplayPixels")}, 
        {"AWTPermission createRobot", new AWTPermission("createRobot")}, 
        {"AWTPermission fullScreenExclusive", new AWTPermission("fullScreenExclusive")}, 
        {"NetPermission setDefaultAuthenticator", new NetPermission("setDefaultAuthenticator")}, 
        {"NetPermission requestPasswordAuthentication", new NetPermission("requestPasswordAuthentication")}, 
        {"NetPermission specifyStreamHandler", new NetPermission("specifyStreamHandler")}, 
        {"PropertyPermission java.home read", new PropertyPermission("java.home", "read")}, 
        {"PropertyPermission java.home write", new PropertyPermission("java.home", "write")}, 
        {"ReflectPermission", new ReflectPermission("suppressAccessChecks")}, 
        {"RuntimePermission createClassLoader", new RuntimePermission("createClassLoader")}, 
        {"RuntimePermission getClassLoader", new RuntimePermission("getClassLoader")}, 
        {"RuntimePermission setContextClassLoader", new RuntimePermission("setContextClassLoader")}, 
        {"RuntimePermission setSecurityManager", new RuntimePermission("setSecurityManager")}, 
        {"RuntimePermission createSecurityManager", new RuntimePermission("createSecurityManager")}, 
        {"RuntimePermission exitVM", new RuntimePermission("exitVM")}, 
        {"RuntimePermission shutdownHooks", new RuntimePermission("shutdownHooks")}, 
        {"RuntimePermission setFactory", new RuntimePermission("setFactory")}, 
        {"RuntimePermission setIO", new RuntimePermission("setIO")}, 
        {"RuntimePermission modifyThread", new RuntimePermission("modifyThread")}, 
        {"RuntimePermission modifyThreadGroup", new RuntimePermission("modifyThreadGroup")}, 
        {"RuntimePermission getProtectionDomain", new RuntimePermission("getProtectionDomain")}, 
        {"RuntimePermission readFileDescriptor", new RuntimePermission("readFileDescriptor")}, 
        {"RuntimePermission writeFileDescriptor", new RuntimePermission("writeFileDescriptor")}, 
        {"RuntimePermission loadLibrary.{library name}", new RuntimePermission("loadLibrary.{library name}")}, 
        {"RuntimePermission accessClassInPackage.java.security", new RuntimePermission("accessClassInPackage.java.security")}, 
        {"RuntimePermission defineClassInPackage.java.lang", new RuntimePermission("defineClassInPackage.java.lang")}, 
        {"RuntimePermission accessDeclaredMembers", new RuntimePermission("accessDeclaredMembers")}, 
        {"RuntimePermission queuePrintJob", new RuntimePermission("queuePrintJob")}, 
        {"SecurityPermission createAccessControlContext", new SerializablePermission("createAccessControlContext")}, 
        {"SecurityPermission getDomainCombiner", new SerializablePermission("getDomainCombiner")}, 
        {"SecurityPermission getPolicy", new SerializablePermission("getPolicy")}, 
        {"SecurityPermission setPolicy", new SerializablePermission("setPolicy")}, 
        {"SecurityPermission setSystemScope", new SerializablePermission("setSystemScope")}, 
        {"SecurityPermission setIdentityPublicKey", new SerializablePermission("setIdentityPublicKey")}, 
        {"SecurityPermission setIdentityInfo", new SerializablePermission("setIdentityInfo")}, 
        {"SecurityPermission addIdentityCertificate", new SerializablePermission("addIdentityCertificate")}, 
        {"SecurityPermission removeIdentityCertificate", new SerializablePermission("removeIdentityCertificate")}, 
        {"SecurityPermission printIdentity", new SerializablePermission("printIdentity")}, 
        {"SecurityPermission getSignerPrivateKey", new SerializablePermission("getSignerPrivateKey")}, 
        {"SecurityPermission setSignerKeyPair", new SerializablePermission("setSignerKeyPair")}, 
        {"SerializablePermission enableSubclassImplementation", new SerializablePermission("enableSubclassImplementation")},
        {"SerializablePermission enableSubstitution", new SerializablePermission("enableSubstitution")},
        {"SQLPermission", new SQLPermission("setLog")}, 
    };
    private Object[][] permissions;
    public void run(final Document document, final Window win){
        int nGrantedTmp = 0;
        ParsedURL docURL = ((SVGOMDocument)document).getParsedURL();
        if ((docURL != null) && 
            (docURL.getHost() != null) && 
            (!"".equals(docURL.getHost()))) {
            permissions = new Object[basePermissions.length + 4][2];
            String docHost = docURL.getHost();
            if (docURL.getPort() != -1) {
                docHost += ":" + docURL.getPort();
            }
            int i=0;
            permissions[i][0] = "SocketPermission accept " + docHost;
            permissions[i][1] = new SocketPermission(docHost, "accept");
            i++;
            permissions[i][0] = "SocketPermission connect " + docHost;
            permissions[i][1] = new SocketPermission(docHost, "connect");
            i++;
            permissions[i][0] = "SocketPermission resolve " + docHost;
            permissions[i][1] = new SocketPermission(docHost, "resolve");
            i++;
            permissions[i][0] = "RuntimePermission stopThread";
            permissions[i][1] = new RuntimePermission("stopThread");
            i++;
            nGrantedTmp = i;
            System.arraycopy(basePermissions, 0, permissions, i, 
                             basePermissions.length);
        } else {
            permissions = basePermissions;
        }
        final int nGranted = nGrantedTmp;
        EventTarget root = (EventTarget)document.getDocumentElement();
        root.addEventListener("SVGLoad", new EventListener() {
                public void handleEvent(Event evt){
                    SecurityManager sm = System.getSecurityManager();
                    int successCnt = 0;
                    Vector unexpectedGrants = new Vector();
                    Vector unexpectedDenial = new Vector();
                    int unexpectedDenialCnt = 0;
                    int unexpectedGrantsCnt = 0;
                    if (sm == null){
                        for (int i=0; i<nGranted; i++) {
                            successCnt++;
                        }
                        for (int i=nGranted; i<permissions.length; i++) {
                            unexpectedGrants.add(permissions[i][0]);
                            unexpectedGrantsCnt++;
                        }
                    }
                    else {
                        for (int i=0; i<nGranted; i++) {
                            Permission p = (Permission)permissions[i][1];
                            try {
                                sm.checkPermission(p);
                                System.out.println(">>>> Permision : " + p + " was granted");
                                successCnt++;
                            } catch (SecurityException se){
                                unexpectedDenial.add(permissions[i][0]);
                                unexpectedDenialCnt++;
                            }
                        }
                        for (int i=nGranted; i<permissions.length; i++) {
                            Permission p = (Permission)permissions[i][1];
                            try {
                                sm.checkPermission(p);
                                System.out.println(">>>> Permision : " + p + " was granted");
                                unexpectedGrants.add(permissions[i][0]);
                                unexpectedGrantsCnt++;
                            } catch (SecurityException se){
                                successCnt++;
                            }
                        }
                    }
                    Element result = document.getElementById("testResult");
                    if ( successCnt == permissions.length ) {
                        result.setAttributeNS(null, "result", "passed");
                    } else {
                        System.out.println("test failed: " + unexpectedGrantsCnt + " / " + unexpectedDenialCnt);
                        result.setAttributeNS(null, "result", "failed");
                        result.setAttributeNS(null, "errorCode", "unexpected.grants.or.denials");
                        String unexpectedGrantsString = "";
                        String unexpectedDenialString = "";
                        for (int i=0; i<unexpectedGrantsCnt; i++) {
                            unexpectedGrantsString += unexpectedGrants.elementAt(i).toString();
                        }
                        for (int i=0; i<unexpectedDenialCnt; i++) {
                            unexpectedDenialString += unexpectedDenial.elementAt(i).toString();
                        }
                        System.out.println("unexpected.grants : " + unexpectedGrantsString);
                        Element entry = null;
                        entry = document.createElementNS(testNS, "errorDescriptiongEntry");
                        entry.setAttributeNS(null, "id", "unexpected.grants.count");
                        entry.setAttributeNS(null, "value", "" + unexpectedGrantsCnt);
                        result.appendChild(entry);
                        entry = document.createElementNS(testNS, "errorDescriptionEntry");
                        entry.setAttributeNS(null, "id", "unexpected.grants");
                        entry.setAttributeNS(null, "value", unexpectedGrantsString);
                        result.appendChild(entry);
                        entry = document.createElementNS(testNS, "errorDescriptiongEntry");
                        entry.setAttributeNS(null, "id", "unexpected.denials.count");
                        entry.setAttributeNS(null, "value", "" + unexpectedDenialCnt);
                        result.appendChild(entry);
                        System.out.println("unexpected.denials : " + unexpectedDenialString);
                        entry = document.createElementNS(testNS, "errorDescriptionEntry");
                        entry.setAttributeNS(null, "id", "unexpected.denials");
                        entry.setAttributeNS(null, "value", unexpectedDenialString);   
                        result.appendChild(entry); 
                    }
                } }, false);        
    }
}