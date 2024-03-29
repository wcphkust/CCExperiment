package org.apache.tools.ant.taskdefs.compilers;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
public final class CompilerAdapterFactory {
    private static final String MODERN_COMPILER = "com.sun.tools.javac.Main";
    private CompilerAdapterFactory() {
    }
    public static CompilerAdapter getCompiler(String compilerType, Task task)
        throws BuildException {
        return getCompiler(compilerType, task, null);
    }
    public static CompilerAdapter getCompiler(String compilerType, Task task,
                                              Path classpath)
        throws BuildException {
            if (compilerType.equalsIgnoreCase("jikes")) {
                return new Jikes();
            }
            if (compilerType.equalsIgnoreCase("extjavac")) {
                return new JavacExternal();
            }
            if (compilerType.equalsIgnoreCase("classic")
                || compilerType.equalsIgnoreCase("javac1.1")
                || compilerType.equalsIgnoreCase("javac1.2")) {
                task.log("This version of java does "
                                         + "not support the classic "
                                         + "compiler; upgrading to modern",
                                         Project.MSG_WARN);
                compilerType = "modern";
            }
            if (compilerType.equalsIgnoreCase("modern")
                || compilerType.equalsIgnoreCase("javac1.3")
                || compilerType.equalsIgnoreCase("javac1.4")
                || compilerType.equalsIgnoreCase("javac1.5")
                || compilerType.equalsIgnoreCase("javac1.6")
                || compilerType.equalsIgnoreCase("javac1.7")) {
                if (doesModernCompilerExist()) {
                    return new Javac13();
                } else {
                    throw new BuildException("Unable to find a javac "
                                             + "compiler;\n"
                                             + MODERN_COMPILER
                                             + " is not on the "
                                             + "classpath.\n"
                                             + "Perhaps JAVA_HOME does not"
                                             + " point to the JDK.\n"
                            + "It is currently set to \""
                            + JavaEnvUtils.getJavaHome()
                            + "\"");
                }
            }
            if (compilerType.equalsIgnoreCase("jvc")
                || compilerType.equalsIgnoreCase("microsoft")) {
                return new Jvc();
            }
            if (compilerType.equalsIgnoreCase("kjc")) {
                return new Kjc();
            }
            if (compilerType.equalsIgnoreCase("gcj")) {
                return new Gcj();
            }
            if (compilerType.equalsIgnoreCase("sj")
                || compilerType.equalsIgnoreCase("symantec")) {
                return new Sj();
            }
            return resolveClassName(compilerType,
                                task.getProject().createClassLoader(classpath));
        }
    private static boolean doesModernCompilerExist() {
        try {
            Class.forName(MODERN_COMPILER);
            return true;
        } catch (ClassNotFoundException cnfe) {
            try {
                ClassLoader cl = CompilerAdapterFactory.class.getClassLoader();
                if (cl != null) {
                    cl.loadClass(MODERN_COMPILER);
                    return true;
                }
            } catch (ClassNotFoundException cnfe2) {
            }
        }
        return false;
    }
    private static CompilerAdapter resolveClassName(String className,
                                                    ClassLoader loader)
        throws BuildException {
        return (CompilerAdapter) ClasspathUtils.newInstance(className,
                loader != null ? loader :
                CompilerAdapterFactory.class.getClassLoader(),
                CompilerAdapter.class);
    }
}
