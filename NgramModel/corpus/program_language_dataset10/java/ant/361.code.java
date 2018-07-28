package org.apache.tools.ant.taskdefs.optional;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
public class Rpm extends Task {
    private static final String PATH1 = "PATH";
    private static final String PATH2 = "Path";
    private static final String PATH3 = "path";
    private String specFile;
    private File topDir;
    private String command = "-bb";
    private String rpmBuildCommand = null;
    private boolean cleanBuildDir = false;
    private boolean removeSpec = false;
    private boolean removeSource = false;
    private File output;
    private File error;
    private boolean failOnError = false;
    private boolean quiet = false;
    public void execute() throws BuildException {
        Commandline toExecute = new Commandline();
        toExecute.setExecutable(rpmBuildCommand == null
                                ? guessRpmBuildCommand()
                                : rpmBuildCommand);
        if (topDir != null) {
            toExecute.createArgument().setValue("--define");
            toExecute.createArgument().setValue("_topdir " + topDir);
        }
        toExecute.createArgument().setLine(command);
        if (cleanBuildDir) {
            toExecute.createArgument().setValue("--clean");
        }
        if (removeSpec) {
            toExecute.createArgument().setValue("--rmspec");
        }
        if (removeSource) {
            toExecute.createArgument().setValue("--rmsource");
        }
        toExecute.createArgument().setValue("SPECS/" + specFile);
        ExecuteStreamHandler streamhandler = null;
        OutputStream outputstream = null;
        OutputStream errorstream = null;
        if (error == null && output == null) {
            if (!quiet) {
                streamhandler = new LogStreamHandler(this, Project.MSG_INFO,
                                                     Project.MSG_WARN);
            } else {
                streamhandler = new LogStreamHandler(this, Project.MSG_DEBUG,
                                                     Project.MSG_DEBUG);
            }
        } else {
            if (output != null) {
                try {
                    BufferedOutputStream bos
                        = new BufferedOutputStream(new FileOutputStream(output));
                    outputstream = new PrintStream(bos);
                } catch (IOException e) {
                    throw new BuildException(e, getLocation());
                }
            } else if (!quiet) {
                outputstream = new LogOutputStream(this, Project.MSG_INFO);
            } else {
                outputstream = new LogOutputStream(this, Project.MSG_DEBUG);
            }
            if (error != null) {
                try {
                    BufferedOutputStream bos
                        = new BufferedOutputStream(new FileOutputStream(error));
                    errorstream = new PrintStream(bos);
                }  catch (IOException e) {
                    throw new BuildException(e, getLocation());
                }
            } else if (!quiet) {
                errorstream = new LogOutputStream(this, Project.MSG_WARN);
            } else {
                errorstream = new LogOutputStream(this, Project.MSG_DEBUG);
            }
            streamhandler = new PumpStreamHandler(outputstream, errorstream);
        }
        Execute exe = getExecute(toExecute, streamhandler);
        try {
            log("Building the RPM based on the " + specFile + " file");
            int returncode = exe.execute();
            if (Execute.isFailure(returncode)) {
                String msg = "'" + toExecute.getExecutable()
                    + "' failed with exit code " + returncode;
                if (failOnError) {
                    throw new BuildException(msg);
                }
                log(msg, Project.MSG_ERR);
            }
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        } finally {
            FileUtils.close(outputstream);
            FileUtils.close(errorstream);
        }
    }
    public void setTopDir(File td) {
        this.topDir = td;
    }
    public void setCommand(String c) {
        this.command = c;
    }
    public void setSpecFile(String sf) {
        if ((sf == null) || (sf.trim().length() == 0)) {
            throw new BuildException("You must specify a spec file", getLocation());
        }
        this.specFile = sf;
    }
    public void setCleanBuildDir(boolean cbd) {
        cleanBuildDir = cbd;
    }
    public void setRemoveSpec(boolean rs) {
        removeSpec = rs;
    }
    public void setRemoveSource(boolean rs) {
        removeSource = rs;
    }
    public void setOutput(File output) {
        this.output = output;
    }
    public void setError(File error) {
        this.error = error;
    }
    public void setRpmBuildCommand(String c) {
        this.rpmBuildCommand = c;
    }
    public void setFailOnError(boolean value) {
        failOnError = value;
    }
    public void setQuiet(boolean value) {
        quiet = value;
    }
    protected String guessRpmBuildCommand() {
        Map env = Execute.getEnvironmentVariables();
        String path = (String) env.get(PATH1);
        if (path == null) {
            path = (String) env.get(PATH2);
            if (path == null) {
                path = (String) env.get(PATH3);
            }
        }
        if (path != null) {
            Path p = new Path(getProject(), path);
            String[] pElements = p.list();
            for (int i = 0; i < pElements.length; i++) {
                File f = new File(pElements[i],
                                  "rpmbuild"
                                  + (Os.isFamily("dos") ? ".exe" : ""));
                if (f.canRead()) {
                    return f.getAbsolutePath();
                }
            }
        }
        return "rpm";
    }
    protected Execute getExecute(Commandline toExecute,
                                 ExecuteStreamHandler streamhandler) {
        Execute exe = new Execute(streamhandler, null);
        exe.setAntRun(getProject());
        if (topDir == null) {
            topDir = getProject().getBaseDir();
        }
        exe.setWorkingDirectory(topDir);
        exe.setCommandline(toExecute.getCommandline());
        return exe;
    }
}