package org.apache.tools.ant.taskdefs.optional.javacc;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.JavaEnvUtils;
public class JJDoc extends Task {
    private static final String OUTPUT_FILE       = "OUTPUT_FILE";
    private static final String TEXT              = "TEXT";
    private static final String ONE_TABLE         = "ONE_TABLE";
    private final Hashtable optionalAttrs = new Hashtable();
    private String outputFile = null;
    private boolean plainText = false;
    private static final String DEFAULT_SUFFIX_HTML = ".html";
    private static final String DEFAULT_SUFFIX_TEXT = ".txt";
    private File targetFile      = null;
    private File javaccHome      = null;
    private CommandlineJava cmdl = new CommandlineJava();
    private String maxMemory = null;
    public void setText(boolean plainText) {
        optionalAttrs.put(TEXT, plainText ? Boolean.TRUE : Boolean.FALSE);
        this.plainText = plainText;
    }
    public void setOnetable(boolean oneTable) {
        optionalAttrs.put(ONE_TABLE, oneTable ? Boolean.TRUE : Boolean.FALSE);
    }
    public void setOutputfile(String outputFile) {
        this.outputFile = outputFile;
    }
    public void setTarget(File target) {
        this.targetFile = target;
    }
    public void setJavacchome(File javaccHome) {
        this.javaccHome = javaccHome;
    }
    public void setMaxmemory(String max) {
        maxMemory = max;
    }
    public JJDoc() {
        cmdl.setVm(JavaEnvUtils.getJreExecutable("java"));
    }
    public void execute() throws BuildException {
        Enumeration iter = optionalAttrs.keys();
        while (iter.hasMoreElements()) {
            String name  = (String) iter.nextElement();
            Object value = optionalAttrs.get(name);
            cmdl.createArgument()
                .setValue("-" + name + ":" + value.toString());
        }
        if (targetFile == null || !targetFile.isFile()) {
            throw new BuildException("Invalid target: " + targetFile);
        }
        if (outputFile != null) {
            cmdl.createArgument() .setValue("-" + OUTPUT_FILE + ":"
                                            + outputFile.replace('\\', '/'));
        }
        File javaFile = new File(createOutputFileName(targetFile, outputFile,
                                                      plainText));
        if (javaFile.exists()
             && targetFile.lastModified() < javaFile.lastModified()) {
            log("Target is already built - skipping (" + targetFile + ")",
                Project.MSG_VERBOSE);
            return;
        }
        cmdl.createArgument().setValue(targetFile.getAbsolutePath());
        final Path classpath = cmdl.createClasspath(getProject());
        final File javaccJar = JavaCC.getArchiveFile(javaccHome);
        classpath.createPathElement().setPath(javaccJar.getAbsolutePath());
        classpath.addJavaRuntime();
        cmdl.setClassname(JavaCC.getMainClass(classpath,
                                              JavaCC.TASKDEF_TYPE_JJDOC));
        cmdl.setMaxmemory(maxMemory);
        final Commandline.Argument arg = cmdl.createVmArgument();
        arg.setValue("-Dinstall.root=" + javaccHome.getAbsolutePath());
        final Execute process =
            new Execute(new LogStreamHandler(this,
                                             Project.MSG_INFO,
                                             Project.MSG_INFO),
                        null);
        log(cmdl.describeCommand(), Project.MSG_VERBOSE);
        process.setCommandline(cmdl.getCommandline());
        try {
            if (process.execute() != 0) {
                throw new BuildException("JJDoc failed.");
            }
        } catch (IOException e) {
            throw new BuildException("Failed to launch JJDoc", e);
        }
    }
    private String createOutputFileName(File destFile, String optionalOutputFile,
                                        boolean plain) {
        String suffix = DEFAULT_SUFFIX_HTML;
        String javaccFile = destFile.getAbsolutePath().replace('\\', '/');
        if (plain) {
            suffix = DEFAULT_SUFFIX_TEXT;
        }
        if ((optionalOutputFile == null) || optionalOutputFile.equals("")) {
            int filePos = javaccFile.lastIndexOf("/");
            if (filePos >= 0) {
                javaccFile = javaccFile.substring(filePos + 1);
            }
            int suffixPos = javaccFile.lastIndexOf('.');
            if (suffixPos == -1) {
                optionalOutputFile = javaccFile + suffix;
            } else {
                String currentSuffix = javaccFile.substring(suffixPos);
                if (currentSuffix.equals(suffix)) {
                    optionalOutputFile = javaccFile + suffix;
                } else {
                    optionalOutputFile = javaccFile.substring(0, suffixPos)
                        + suffix;
                }
            }
        } else {
            optionalOutputFile = optionalOutputFile.replace('\\', '/');
        }
        return (getProject().getBaseDir() + "/" + optionalOutputFile)
            .replace('\\', '/');
    }
}
