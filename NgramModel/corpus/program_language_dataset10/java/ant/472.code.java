package org.apache.tools.ant.taskdefs.optional.jsp.compilers;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.jsp.JspNameMangler;
import org.apache.tools.ant.taskdefs.optional.jsp.Jasper41Mangler;
public final class JspCompilerAdapterFactory {
    private JspCompilerAdapterFactory() {
    }
    public static JspCompilerAdapter getCompiler(String compilerType, Task task)
        throws BuildException {
        return getCompiler(compilerType, task,
                           task.getProject().createClassLoader(null));
    }
    public static JspCompilerAdapter getCompiler(String compilerType, Task task,
                                                 AntClassLoader loader)
        throws BuildException {
        if (compilerType.equalsIgnoreCase("jasper")) {
            return new JasperC(new JspNameMangler());
        }
        if (compilerType.equalsIgnoreCase("jasper41")) {
            return new JasperC(new Jasper41Mangler());
        }
        return resolveClassName(compilerType, loader);
    }
    private static JspCompilerAdapter resolveClassName(String className,
                                                       AntClassLoader classloader)
        throws BuildException {
        try {
            Class c = classloader.findClass(className);
            Object o = c.newInstance();
            return (JspCompilerAdapter) o;
        } catch (ClassNotFoundException cnfe) {
            throw new BuildException(className + " can\'t be found.", cnfe);
        } catch (ClassCastException cce) {
            throw new BuildException(className + " isn\'t the classname of "
                                     + "a compiler adapter.", cce);
        } catch (Throwable t) {
            throw new BuildException(className + " caused an interesting "
                                     + "exception.", t);
        }
    }
}
