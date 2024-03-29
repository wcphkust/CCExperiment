package org.apache.xerces.util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.taskdefs.Javac;
import java.lang.StringBuffer;
import java.util.Properties;
import java.util.Locale;
public class XJavac extends Javac {
    public void execute() throws BuildException {
        if(isJDK14OrHigher()) {
            Properties props = null;
            try {
                props = System.getProperties();
            } catch (Exception e) {
                throw new BuildException("unable to determine java vendor because could not access system properties!");
            }
            String vendor = ((String)props.get("java.vendor")).toUpperCase(Locale.ENGLISH);
            if (vendor.indexOf("IBM") >= 0) {
                setBootclasspath(createIBMJDKBootclasspath());
            }
            else if( (vendor.indexOf("SUN") >= 0) || 
                     (vendor.indexOf("BLACKDOWN") >= 0) || 
                     (vendor.indexOf("APPLE") >= 0) ||
                     (vendor.indexOf("HEWLETT-PACKARD") >= 0) ||
                     (vendor.indexOf("KAFFE") >= 0) ||
                     (vendor.indexOf("SABLE") >= 0) ||
                     (vendor.indexOf("FREEBSD") >= 0)) {
                Path bcp = createBootclasspath();
                Path clPath = getClasspath();
                bcp.append(clPath);
                String currBCP = (String)props.get("sun.boot.class.path");
                Path currBCPath = new Path(null); 
                currBCPath.createPathElement().setPath(currBCP);
                bcp.append(currBCPath);
                setBootclasspath(bcp);
            }
        }
        super.execute();
    }
    private Path createIBMJDKBootclasspath() {
        Path bcp = createBootclasspath();
        String javaHome = System.getProperty("java.home");
        StringBuffer bcpMember = new StringBuffer();
        bcpMember.append(javaHome).append("/lib/charsets.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(), "/lib/core.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(), "/lib/vm.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(), "/lib/java.util.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(), "/lib/rt.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/graphics.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/javaws.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/jaws.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/security.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/server.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/JawBridge.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/gskikm.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/ibmjceprovider.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/indicim.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/jaccess.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/ldapsec.jar:");
        bcp.createPathElement().setPath(bcpMember.toString());
        bcpMember.replace(javaHome.length(), bcpMember.length(),  "/lib/ext/oldcertpath.jar");
        bcp.createPathElement().setPath(bcpMember.toString());
        return bcp;
    }
    private boolean isJDK14OrHigher() {
        final String version = JavaEnvUtils.getJavaVersion();
        return version.equals(JavaEnvUtils.JAVA_1_4) ||
            (!version.equals(JavaEnvUtils.JAVA_1_3) &&
            !version.equals(JavaEnvUtils.JAVA_1_2) &&
            !version.equals(JavaEnvUtils.JAVA_1_1) &&
            !version.equals(JavaEnvUtils.JAVA_1_0));
    }
}
