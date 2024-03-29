package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.LineContainsRegExp;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.types.RegularExpression;
import org.apache.tools.ant.util.JavaEnvUtils;
public abstract class AbstractJarSignerTask extends Task {
    protected File jar;
    protected String alias;
    protected String keystore;
    protected String storepass;
    protected String storetype;
    protected String keypass;
    protected boolean verbose;
    protected String maxMemory;
    protected Vector filesets = new Vector();
    protected static final String JARSIGNER_COMMAND = "jarsigner";
    private RedirectorElement redirector;
    private Environment sysProperties = new Environment();
    public static final String ERROR_NO_SOURCE = "jar must be set through jar attribute "
            + "or nested filesets";
    private Path path = null;
    private String executable;
    public void setMaxmemory(String max) {
        maxMemory = max;
    }
    public void setJar(final File jar) {
        this.jar = jar;
    }
    public void setAlias(final String alias) {
        this.alias = alias;
    }
    public void setKeystore(final String keystore) {
        this.keystore = keystore;
    }
    public void setStorepass(final String storepass) {
        this.storepass = storepass;
    }
    public void setStoretype(final String storetype) {
        this.storetype = storetype;
    }
    public void setKeypass(final String keypass) {
        this.keypass = keypass;
    }
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }
    public void addFileset(final FileSet set) {
        filesets.addElement(set);
    }
    public void addSysproperty(Environment.Variable sysp) {
        sysProperties.addVariable(sysp);
    }
    public Path createPath() {
        if (path == null) {
            path = new Path(getProject());
        }
        return path.createPath();
    }
    protected void beginExecution() {
        redirector = createRedirector();
    }
    protected void endExecution() {
        redirector = null;
    }
    private RedirectorElement createRedirector() {
        RedirectorElement result = new RedirectorElement();
        if (storepass != null) {
            StringBuffer input = new StringBuffer(storepass).append('\n');
            if (keypass != null) {
                input.append(keypass).append('\n');
            }
            result.setInputString(input.toString());
            result.setLogInputString(false);
            LineContainsRegExp filter = new LineContainsRegExp();
            RegularExpression rx = new RegularExpression();
            rx.setPattern("^(Enter Passphrase for keystore: |Enter key password for .+: )$");
            filter.addConfiguredRegexp(rx);
            filter.setNegate(true);
            result.createErrorFilterChain().addLineContainsRegExp(filter);
        }
        return result;
    }
    public RedirectorElement getRedirector() {
        return redirector;
    }
    public void setExecutable(String executable) {
        this.executable = executable;
    }
    protected void setCommonOptions(final ExecTask cmd) {
        if (maxMemory != null) {
            addValue(cmd, "-J-Xmx" + maxMemory);
        }
        if (verbose) {
            addValue(cmd, "-verbose");
        }
        Vector props = sysProperties.getVariablesVector();
        Enumeration e = props.elements();
        while (e.hasMoreElements()) {
            Environment.Variable variable = (Environment.Variable) e.nextElement();
            declareSysProperty(cmd, variable);
        }
    }
    protected void declareSysProperty(
        ExecTask cmd, Environment.Variable property) throws BuildException {
        addValue(cmd, "-J-D" + property.getContent());
    }
    protected void bindToKeystore(final ExecTask cmd) {
        if (null != keystore) {
            addValue(cmd, "-keystore");
            String loc;
            File keystoreFile = getProject().resolveFile(keystore);
            if (keystoreFile.exists()) {
                loc = keystoreFile.getPath();
            } else {
                loc = keystore;
            }
            addValue(cmd, loc);
        }
        if (null != storetype) {
            addValue(cmd, "-storetype");
            addValue(cmd, storetype);
        }
    }
    protected ExecTask createJarSigner() {
        final ExecTask cmd = new ExecTask(this);
        if (executable == null) {
            cmd.setExecutable(JavaEnvUtils.getJdkExecutable(JARSIGNER_COMMAND));
        } else {
            cmd.setExecutable(executable);
        }
        cmd.setTaskType(JARSIGNER_COMMAND);
        cmd.setFailonerror(true);
        cmd.addConfiguredRedirector(redirector);
        return cmd;
    }
    protected Vector createUnifiedSources() {
        Vector sources = (Vector) filesets.clone();
        if (jar != null) {
            FileSet sourceJar = new FileSet();
            sourceJar.setProject(getProject());
            sourceJar.setFile(jar);
            sourceJar.setDir(jar.getParentFile());
            sources.add(sourceJar);
        }
        return sources;
    }
    protected Path createUnifiedSourcePath() {
        Path p = path == null ? new Path(getProject()) : (Path) path.clone();
        Vector s = createUnifiedSources();
        Enumeration e = s.elements();
        while (e.hasMoreElements()) {
            p.add((FileSet) e.nextElement());
        }
        return p;
    }
    protected boolean hasResources() {
        return path != null || filesets.size() > 0;
    }
    protected void addValue(final ExecTask cmd, String value) {
        cmd.createArg().setValue(value);
    }
}
