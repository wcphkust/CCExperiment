package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.RedirectorElement;
import org.apache.tools.ant.util.FileUtils;
public class ExecTask extends Task {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private String os;
    private String osFamily;
    private File dir;
    protected boolean failOnError = false;
    protected boolean newEnvironment = false;
    private Long timeout = null;
    private Environment env = new Environment();
    protected Commandline cmdl = new Commandline();
    private String resultProperty;
    private boolean failIfExecFails = true;
    private String executable;
    private boolean resolveExecutable = false;
    private boolean searchPath = false;
    private boolean spawn = false;
    private boolean incompatibleWithSpawn = false;
    private String inputString;
    private File input;
    private File output;
    private File error;
    protected Redirector redirector = new Redirector(this);
    protected RedirectorElement redirectorElement;
    private boolean vmLauncher = true;
    public ExecTask() {
    }
    public ExecTask(Task owner) {
        bindToOwner(owner);
    }
    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }
    public void setTimeout(Long value) {
        timeout = value;
        incompatibleWithSpawn = true;
    }
    public void setTimeout(Integer value) {
        setTimeout(
            (Long) ((value == null) ? null : new Long(value.intValue())));
    }
    public void setExecutable(String value) {
        this.executable = value;
        cmdl.setExecutable(value);
    }
    public void setDir(File d) {
        this.dir = d;
    }
    public void setOs(String os) {
        this.os = os;
    }
    public final String getOs() {
        return os;
    }
    public void setCommand(Commandline cmdl) {
        log("The command attribute is deprecated.\n"
            + "Please use the executable attribute and nested arg elements.",
            Project.MSG_WARN);
        this.cmdl = cmdl;
    }
    public void setOutput(File out) {
        this.output = out;
        incompatibleWithSpawn = true;
    }
    public void setInput(File input) {
        if (inputString != null) {
            throw new BuildException("The \"input\" and \"inputstring\" "
                + "attributes cannot both be specified");
        }
        this.input = input;
        incompatibleWithSpawn = true;
    }
    public void setInputString(String inputString) {
        if (input != null) {
            throw new BuildException("The \"input\" and \"inputstring\" "
                + "attributes cannot both be specified");
        }
        this.inputString = inputString;
        incompatibleWithSpawn = true;
    }
    public void setLogError(boolean logError) {
        redirector.setLogError(logError);
        incompatibleWithSpawn |= logError;
    }
    public void setError(File error) {
        this.error = error;
        incompatibleWithSpawn = true;
    }
    public void setOutputproperty(String outputProp) {
        redirector.setOutputProperty(outputProp);
        incompatibleWithSpawn = true;
    }
    public void setErrorProperty(String errorProperty) {
        redirector.setErrorProperty(errorProperty);
        incompatibleWithSpawn = true;
    }
    public void setFailonerror(boolean fail) {
        failOnError = fail;
        incompatibleWithSpawn |= fail;
    }
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }
    public void setResolveExecutable(boolean resolveExecutable) {
        this.resolveExecutable = resolveExecutable;
    }
    public void setSearchPath(boolean searchPath) {
        this.searchPath = searchPath;
    }
    public boolean getResolveExecutable() {
        return resolveExecutable;
    }
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }
    public Commandline.Argument createArg() {
        return cmdl.createArgument();
    }
    public void setResultProperty(String resultProperty) {
        this.resultProperty = resultProperty;
        incompatibleWithSpawn = true;
    }
    protected void maybeSetResultPropertyValue(int result) {
        if (resultProperty != null) {
            String res = Integer.toString(result);
            getProject().setNewProperty(resultProperty, res);
        }
    }
    public void setFailIfExecutionFails(boolean flag) {
        failIfExecFails = flag;
        incompatibleWithSpawn = true;
    }
    public void setAppend(boolean append) {
        redirector.setAppend(append);
        incompatibleWithSpawn = true;
    }
    public void addConfiguredRedirector(RedirectorElement redirectorElement) {
        if (this.redirectorElement != null) {
            throw new BuildException("cannot have > 1 nested <redirector>s");
        }
        this.redirectorElement = redirectorElement;
        incompatibleWithSpawn = true;
    }
    public void setOsFamily(String osFamily) {
        this.osFamily = osFamily.toLowerCase(Locale.ENGLISH);
    }
    public final String getOsFamily() {
        return osFamily;
    }
    protected String resolveExecutable(String exec, boolean mustSearchPath) {
        if (!resolveExecutable) {
            return exec;
        }
        File executableFile = getProject().resolveFile(exec);
        if (executableFile.exists()) {
            return executableFile.getAbsolutePath();
        }
        if (dir != null) {
            executableFile = FILE_UTILS.resolveFile(dir, exec);
            if (executableFile.exists()) {
                return executableFile.getAbsolutePath();
            }
        }
        if (mustSearchPath) {
            Path p = null;
            String[] environment = env.getVariables();
            if (environment != null) {
                for (int i = 0; i < environment.length; i++) {
                    if (isPath(environment[i])) {
                        p = new Path(getProject(), getPath(environment[i]));
                        break;
                    }
                }
            }
            if (p == null) {
                String path = getPath(Execute.getEnvironmentVariables());
                if (path != null) {
                    p = new Path(getProject(), path);
                }
            }
            if (p != null) {
                String[] dirs = p.list();
                for (int i = 0; i < dirs.length; i++) {
                    executableFile
                        = FILE_UTILS.resolveFile(new File(dirs[i]), exec);
                    if (executableFile.exists()) {
                        return executableFile.getAbsolutePath();
                    }
                }
            }
        }
        return exec;
    }
    public void execute() throws BuildException {
        if (!isValidOs()) {
            return;
        }
        File savedDir = dir; 
        cmdl.setExecutable(resolveExecutable(executable, searchPath));
        checkConfiguration();
        try {
            runExec(prepareExec());
        } finally {
            dir = savedDir;
        }
    }
    protected void checkConfiguration() throws BuildException {
        if (cmdl.getExecutable() == null) {
            throw new BuildException("no executable specified", getLocation());
        }
        if (dir != null && !dir.exists()) {
            throw new BuildException("The directory " + dir + " does not exist");
        }
        if (dir != null && !dir.isDirectory()) {
            throw new BuildException(dir + " is not a directory");
        }
        if (spawn && incompatibleWithSpawn) {
            getProject().log("spawn does not allow attributes related to input, "
            + "output, error, result", Project.MSG_ERR);
            getProject().log("spawn also does not allow timeout", Project.MSG_ERR);
            getProject().log("finally, spawn is not compatible "
                + "with a nested I/O <redirector>", Project.MSG_ERR);
            throw new BuildException("You have used an attribute "
                + "or nested element which is not compatible with spawn");
        }
        setupRedirector();
    }
    protected void setupRedirector() {
        redirector.setInput(input);
        redirector.setInputString(inputString);
        redirector.setOutput(output);
        redirector.setError(error);
    }
    protected boolean isValidOs() {
        if (osFamily != null && !Os.isFamily(osFamily)) {
            return false;
        }
        String myos = System.getProperty("os.name");
        log("Current OS is " + myos, Project.MSG_VERBOSE);
        if ((os != null) && (os.indexOf(myos) < 0)) {
            log("This OS, " + myos
                    + " was not found in the specified list of valid OSes: " + os,
                    Project.MSG_VERBOSE);
            return false;
        }
        return true;
    }
    public void setVMLauncher(boolean vmLauncher) {
        this.vmLauncher = vmLauncher;
    }
    protected Execute prepareExec() throws BuildException {
        if (dir == null) {
            dir = getProject().getBaseDir();
        }
        if (redirectorElement != null) {
            redirectorElement.configure(redirector);
        }
        Execute exe = new Execute(createHandler(), createWatchdog());
        exe.setAntRun(getProject());
        exe.setWorkingDirectory(dir);
        exe.setVMLauncher(vmLauncher);
        String[] environment = env.getVariables();
        if (environment != null) {
            for (int i = 0; i < environment.length; i++) {
                log("Setting environment variable: " + environment[i],
                    Project.MSG_VERBOSE);
            }
        }
        exe.setNewenvironment(newEnvironment);
        exe.setEnvironment(environment);
        return exe;
    }
    protected final void runExecute(Execute exe) throws IOException {
        int returnCode = -1; 
        if (!spawn) {
            returnCode = exe.execute();
            if (exe.killedProcess()) {
                String msg = "Timeout: killed the sub-process";
                if (failOnError) {
                    throw new BuildException(msg);
                } else {
                    log(msg, Project.MSG_WARN);
                }
            }
            maybeSetResultPropertyValue(returnCode);
            redirector.complete();
            if (Execute.isFailure(returnCode)) {
                if (failOnError) {
                    throw new BuildException(getTaskType() + " returned: "
                        + returnCode, getLocation());
                } else {
                    log("Result: " + returnCode, Project.MSG_ERR);
                }
            }
        } else {
            exe.spawn();
        }
    }
    protected void runExec(Execute exe) throws BuildException {
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);
        exe.setCommandline(cmdl.getCommandline());
        try {
            runExecute(exe);
        } catch (IOException e) {
            if (failIfExecFails) {
                throw new BuildException("Execute failed: " + e.toString(), e,
                                         getLocation());
            } else {
                log("Execute failed: " + e.toString(), Project.MSG_ERR);
            }
        } finally {
            logFlush();
        }
    }
    protected ExecuteStreamHandler createHandler() throws BuildException {
        return redirector.createHandler();
    }
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        return (timeout == null)
            ? null : new ExecuteWatchdog(timeout.longValue());
    }
    protected void logFlush() {
    }
    private boolean isPath(String line) {
        return line.startsWith("PATH=")
            || line.startsWith("Path=");
    }
    private String getPath(String line) {
        return line.substring("PATH=".length());
    }
    private String getPath(Map map) {
        String p = (String) map.get("PATH");
        return p != null ? p : (String) map.get("Path");
    }
}