package org.apache.tools.ant.taskdefs.optional;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.TeeOutputStream;
import org.apache.tools.ant.util.FileUtils;
public class ANTLR extends Task {
    private CommandlineJava commandline = new CommandlineJava();
    private File targetFile;
    private File outputDirectory;
    private File superGrammar;
    private boolean html;
    private boolean diagnostic;
    private boolean trace;
    private boolean traceParser;
    private boolean traceLexer;
    private boolean traceTreeWalker;
    private File workingdir = null;
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();
    private boolean debug;
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    public ANTLR() {
        commandline.setVm(JavaEnvUtils.getJreExecutable("java"));
        commandline.setClassname("antlr.Tool");
    }
    public void setTarget(File target) {
        log("Setting target to: " + target.toString(), Project.MSG_VERBOSE);
        this.targetFile = target;
    }
    public void setOutputdirectory(File outputDirectory) {
        log("Setting output directory to: " + outputDirectory.toString(), Project.MSG_VERBOSE);
        this.outputDirectory = outputDirectory;
    }
    public void setGlib(String superGrammar) {
        String sg = null;
        if (Os.isFamily("dos")) {
            sg = superGrammar.replace('\\', '/');
        } else {
            sg = superGrammar;
        }
        setGlib(FILE_UTILS.resolveFile(getProject().getBaseDir(), sg));
    }
    public void setGlib(File superGrammar) {
        this.superGrammar = superGrammar;
    }
    public void setDebug(boolean enable) {
        this.debug = enable;
    }
    public void setHtml(boolean enable) {
        html = enable;
    }
    public void setDiagnostic(boolean enable) {
        diagnostic = enable;
    }
    public void setTrace(boolean enable) {
        trace = enable;
    }
    public void setTraceParser(boolean enable) {
        traceParser = enable;
    }
    public void setTraceLexer(boolean enable) {
        traceLexer = enable;
    }
    public void setTraceTreeWalker(boolean enable) {
        traceTreeWalker = enable;
    }
    public void setFork(boolean s) {
    }
    public void setDir(File d) {
        this.workingdir = d;
    }
    public Path createClasspath() {
        return commandline.createClasspath(getProject()).createPath();
    }
    public Commandline.Argument createJvmarg() {
        return commandline.createVmArgument();
    }
    public void init() throws BuildException {
        addClasspathEntry("/antlr/ANTLRGrammarParseBehavior.class");
    }
    protected void addClasspathEntry(String resource) {
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        } else {
            resource = "org/apache/tools/ant/taskdefs/optional/"
                + resource;
        }
        File f = LoaderUtils.getResourceSource(getClass().getClassLoader(),
                                               resource);
        if (f != null) {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            createClasspath().setLocation(f);
        } else {
            log("Couldn\'t find " + resource, Project.MSG_VERBOSE);
        }
    }
    public void execute() throws BuildException {
        validateAttributes();
        File generatedFile = getGeneratedFile();
        boolean targetIsOutOfDate =
            targetFile.lastModified() > generatedFile.lastModified();
        boolean superGrammarIsOutOfDate  = superGrammar != null
                && (superGrammar.lastModified() > generatedFile.lastModified());
        if (targetIsOutOfDate || superGrammarIsOutOfDate) {
            if (targetIsOutOfDate) {
                log("Compiling " + targetFile + " as it is newer than "
                    + generatedFile, Project.MSG_VERBOSE);
            } else {
                log("Compiling " + targetFile + " as " + superGrammar
                    + " is newer than " + generatedFile, Project.MSG_VERBOSE);
            }
            populateAttributes();
            commandline.createArgument().setValue(targetFile.toString());
            log(commandline.describeCommand(), Project.MSG_VERBOSE);
            int err = run(commandline.getCommandline());
            if (err != 0) {
                throw new BuildException("ANTLR returned: " + err, getLocation());
            } else {
                String output = bos.toString();
                if (output.indexOf("error:") > -1) {
                    throw new BuildException("ANTLR signaled an error: "
                                             + output, getLocation());
                }
            }
        } else {
            log("Skipped grammar file. Generated file " + generatedFile
                + " is newer.", Project.MSG_VERBOSE);
        }
    }
    private void populateAttributes() {
        commandline.createArgument().setValue("-o");
        commandline.createArgument().setValue(outputDirectory.toString());
        if (superGrammar != null) {
            commandline.createArgument().setValue("-glib");
            commandline.createArgument().setValue(superGrammar.toString());
        }
        if (html) {
            commandline.createArgument().setValue("-html");
        }
        if (diagnostic) {
            commandline.createArgument().setValue("-diagnostic");
        }
        if (trace) {
            commandline.createArgument().setValue("-trace");
        }
        if (traceParser) {
            commandline.createArgument().setValue("-traceParser");
        }
        if (traceLexer) {
            commandline.createArgument().setValue("-traceLexer");
        }
        if (traceTreeWalker) {
            if (is272()) {
                commandline.createArgument().setValue("-traceTreeParser");
            } else {
                commandline.createArgument().setValue("-traceTreeWalker");
            }
        }
        if (debug) {
            commandline.createArgument().setValue("-debug");
        }
    }
    private void validateAttributes() throws BuildException {
        if (targetFile == null || !targetFile.isFile()) {
            throw new BuildException("Invalid target: " + targetFile);
        }
        if (outputDirectory == null) {
            setOutputdirectory(new File(targetFile.getParent()));
        }
        if (!outputDirectory.isDirectory()) {
            throw new BuildException("Invalid output directory: " + outputDirectory);
        }
    }
    private File getGeneratedFile() throws BuildException {
        String generatedFileName = null;
        try {
            BufferedReader in = new BufferedReader(new FileReader(targetFile));
            String line;
            while ((line = in.readLine()) != null) {
                int extendsIndex = line.indexOf(" extends ");
                if (line.startsWith("class ") && extendsIndex > -1) {
                    generatedFileName = line.substring(
                        "class ".length(), extendsIndex).trim();
                    break;
                }
            }
            in.close();
        } catch (Exception e) {
            throw new BuildException("Unable to determine generated class", e);
        }
        if (generatedFileName == null) {
            throw new BuildException("Unable to determine generated class");
        }
        return new File(outputDirectory, generatedFileName
                        + (html ? ".html" : ".java"));
    }
    private int run(String[] command) throws BuildException {
        PumpStreamHandler psh =
            new PumpStreamHandler(new LogOutputStream(this, Project.MSG_INFO),
                                  new TeeOutputStream(
                                                      new LogOutputStream(this,
                                                                          Project.MSG_WARN),
                                                      bos)
                                  );
        Execute exe = new Execute(psh, null);
        exe.setAntRun(getProject());
        if (workingdir != null) {
            exe.setWorkingDirectory(workingdir);
        }
        exe.setCommandline(command);
        try {
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        } finally {
            FileUtils.close(bos);
        }
    }
    protected boolean is272() {
        AntClassLoader l = null;
        try {
            l = getProject().createClassLoader(commandline.getClasspath());
            l.loadClass("antlr.Version");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } finally {
            if (l != null) {
                l.cleanup();
            }
        }
    }
}
