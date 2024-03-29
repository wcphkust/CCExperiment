package org.apache.tools.ant.taskdefs.compilers;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Apt;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;
public class AptCompilerAdapter extends DefaultCompilerAdapter {
    private static final int APT_COMPILER_SUCCESS = 0;
    public static final String APT_ENTRY_POINT = "com.sun.tools.apt.Main";
    public static final String APT_METHOD_NAME = "process";
    protected Apt getApt() {
        return (Apt) getJavac();
    }
    static void setAptCommandlineSwitches(Apt apt, Commandline cmd) {
        if (!apt.isCompile()) {
            cmd.createArgument().setValue("-nocompile");
        }
        String factory = apt.getFactory();
        if (factory != null) {
            cmd.createArgument().setValue("-factory");
            cmd.createArgument().setValue(factory);
        }
        Path factoryPath = apt.getFactoryPath();
        if (factoryPath != null) {
            cmd.createArgument().setValue("-factorypath");
            cmd.createArgument().setPath(factoryPath);
        }
        File preprocessDir = apt.getPreprocessDir();
        if (preprocessDir != null) {
            cmd.createArgument().setValue("-s");
            cmd.createArgument().setFile(preprocessDir);
        }
        Vector options = apt.getOptions();
        Enumeration elements = options.elements();
        Apt.Option opt;
        StringBuffer arg = null;
        while (elements.hasMoreElements()) {
            opt = (Apt.Option) elements.nextElement();
            arg = new StringBuffer();
            arg.append("-A").append(opt.getName());
            if (opt.getValue() != null) {
                arg.append("=").append(opt.getValue());
            }
            cmd.createArgument().setValue(arg.toString());
        }
    }
    protected void setAptCommandlineSwitches(Commandline cmd) {
        Apt apt = getApt();
        setAptCommandlineSwitches(apt, cmd);
    }
    public boolean execute() throws BuildException {
        attributes.log("Using apt compiler", Project.MSG_VERBOSE);
        Commandline cmd = setupModernJavacCommand();
        setAptCommandlineSwitches(cmd);
        try {
            Class c = Class.forName(APT_ENTRY_POINT);
            Object compiler = c.newInstance();
            Method compile = c.getMethod(APT_METHOD_NAME,
                    new Class[]{(new String[]{}).getClass()});
            int result = ((Integer) compile.invoke
                    (compiler, new Object[]{cmd.getArguments()}))
                    .intValue();
            return (result == APT_COMPILER_SUCCESS);
        } catch (BuildException be) {
            throw be;
        } catch (Exception ex) {
            throw new BuildException("Error starting apt compiler",
                    ex, location);
        }
    }
}
