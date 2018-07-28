package org.apache.tools.ant.taskdefs.optional.ccm;
import java.io.File;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;
public class CCMCheck extends Continuus {
    private File file = null;
    private String comment = null;
    private String task = null;
    protected Vector filesets = new Vector();
    public CCMCheck() {
        super();
    }
    public File getFile() {
        return file;
    }
    public void setFile(File v) {
        log("working file " + v, Project.MSG_VERBOSE);
        this.file = v;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String v) {
        this.comment = v;
    }
    public String getTask() {
        return task;
    }
    public void setTask(String v) {
        this.task = v;
    }
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }
    public void execute() throws BuildException {
        if (file == null && filesets.size() == 0) {
            throw new BuildException(
                "Specify at least one source - a file or a fileset.");
        }
        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("CCMCheck cannot be generated for directories");
        }
        if (file != null  && filesets.size() > 0) {
            throw new BuildException("Choose between file and fileset !");
        }
        if (getFile() != null) {
            doit();
            return;
        }
        int sizeofFileSet = filesets.size();
        for (int i = 0; i < sizeofFileSet; i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            String[] srcFiles = ds.getIncludedFiles();
            for (int j = 0; j < srcFiles.length; j++) {
                File src = new File(fs.getDir(getProject()), srcFiles[j]);
                setFile(src);
                doit();
            }
        }
    }
    private void doit() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());
        checkOptions(commandLine);
        int result = run(commandLine);
        if (Execute.isFailure(result)) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, getLocation());
        }
    }
    private void checkOptions(Commandline cmd) {
        if (getComment() != null) {
            cmd.createArgument().setValue(FLAG_COMMENT);
            cmd.createArgument().setValue(getComment());
        }
        if (getTask() != null) {
            cmd.createArgument().setValue(FLAG_TASK);
            cmd.createArgument().setValue(getTask());
        }
        if (getFile() != null) {
            cmd.createArgument().setValue(file.getAbsolutePath());
        }
    }
    public static final String FLAG_COMMENT = "/comment";
    public static final String FLAG_TASK = "/task";
}