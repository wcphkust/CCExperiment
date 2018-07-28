package org.apache.tools.ant.taskdefs.optional.j2ee;
import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
public class JonasHotDeploymentTool extends GenericHotDeploymentTool implements HotDeploymentTool {
    protected static final String DEFAULT_ORB = "RMI";
    private static final String JONAS_DEPLOY_CLASS_NAME = "org.objectweb.jonas.adm.JonasAdmin";
    private static final String[] VALID_ACTIONS
        = {ACTION_DELETE, ACTION_DEPLOY, ACTION_LIST, ACTION_UNDEPLOY, ACTION_UPDATE};
    private File jonasroot;
    private String orb = null;
    private String davidHost;
    private int davidPort;
    public void setDavidhost(final String inValue) {
        davidHost = inValue;
    }
    public void setDavidport(final int inValue) {
        davidPort = inValue;
    }
    public void setJonasroot(final File inValue) {
        jonasroot = inValue;
    }
    public void setOrb(final String inValue) {
        orb = inValue;
    }
    public Path getClasspath() {
        Path aClassPath = super.getClasspath();
        if (aClassPath == null) {
            aClassPath = new Path(getTask().getProject());
        }
        if (orb != null) {
            String aOrbJar = new File(jonasroot, "lib/" + orb + "_jonas.jar").toString();
            String aConfigDir = new File(jonasroot, "config/").toString();
            Path aJOnASOrbPath = new Path(aClassPath.getProject(),
                    aOrbJar + File.pathSeparator + aConfigDir);
            aClassPath.append(aJOnASOrbPath);
        }
        return aClassPath;
    }
    public void validateAttributes() throws BuildException {
        Java java = getJava();
        String action = getTask().getAction();
        if (action == null) {
            throw new BuildException("The \"action\" attribute must be set");
        }
        if (!isActionValid()) {
            throw new BuildException("Invalid action \"" + action + "\" passed");
        }
        if (getClassName() == null) {
            setClassName(JONAS_DEPLOY_CLASS_NAME);
        }
        if (jonasroot == null || jonasroot.isDirectory()) {
            java.createJvmarg().setValue("-Dinstall.root=" + jonasroot);
            java.createJvmarg().setValue("-Djava.security.policy=" + jonasroot
                + "/config/java.policy");
            if ("DAVID".equals(orb)) {
                java.createJvmarg().setValue("-Dorg.omg.CORBA.ORBClass"
                    + "=org.objectweb.david.libs.binding.orbs.iiop.IIOPORB");
                java.createJvmarg().setValue("-Dorg.omg.CORBA.ORBSingletonClass="
                    + "org.objectweb.david.libs.binding.orbs.ORBSingletonClass");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.StubClass="
                    + "org.objectweb.david.libs.stub_factories.rmi.StubDelegate");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.PortableRemoteObjectClass="
                    + "org.objectweb.david.libs.binding.rmi.ORBPortableRemoteObjectDelegate");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.UtilClass="
                    + "org.objectweb.david.libs.helpers.RMIUtilDelegate");
                java.createJvmarg().setValue("-Ddavid.CosNaming.default_method=0");
                java.createJvmarg().setValue("-Ddavid.rmi.ValueHandlerClass="
                    + "com.sun.corba.se.internal.io.ValueHandlerImpl");
                if (davidHost != null) {
                    java.createJvmarg().setValue("-Ddavid.CosNaming.default_host="
                        + davidHost);
                }
                if (davidPort != 0) {
                    java.createJvmarg().setValue("-Ddavid.CosNaming.default_port="
                        + davidPort);
                }
            }
        }
        if (getServer() != null) {
            java.createArg().setLine("-n " + getServer());
        }
        if (action.equals(ACTION_DEPLOY)
            || action.equals(ACTION_UPDATE)
            || action.equals("redeploy")) {
            java.createArg().setLine("-a " + getTask().getSource());
        } else if (action.equals(ACTION_DELETE) || action.equals(ACTION_UNDEPLOY)) {
            java.createArg().setLine("-r " + getTask().getSource());
        } else if (action.equals(ACTION_LIST)) {
            java.createArg().setValue("-l");
        }
    }
    protected boolean isActionValid() {
        boolean valid = false;
        String action = getTask().getAction();
        for (int i = 0; i < VALID_ACTIONS.length; i++) {
            if (action.equals(VALID_ACTIONS[i])) {
                valid = true;
                break;
            }
        }
        return valid;
    }
}