package org.apache.tools.ant.taskdefs.compilers;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
public class Sj extends DefaultCompilerAdapter {
    public boolean execute() throws BuildException {
        attributes.log("Using symantec java compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupJavacCommand();
        String exec = getJavac().getExecutable();
        cmd.setExecutable(exec == null ? "sj" : exec);
        int firstFileName = cmd.size() - compileList.length;
        return
            executeExternalCompile(cmd.getCommandline(), firstFileName) == 0;
    }
    protected String getNoDebugArgument() {
        return null;
    }
}
