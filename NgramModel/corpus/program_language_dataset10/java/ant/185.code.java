package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.ClasspathUtils;
public abstract class DefBase extends AntlibDefinition {
    private ClassLoader createdLoader;
    private ClasspathUtils.Delegate cpDelegate;
    protected boolean hasCpDelegate() {
        return cpDelegate != null;
    }
    public void setReverseLoader(boolean reverseLoader) {
        getDelegate().setReverseLoader(reverseLoader);
        log("The reverseloader attribute is DEPRECATED. It will be removed",
            Project.MSG_WARN);
    }
    public Path getClasspath() {
        return getDelegate().getClasspath();
    }
    public boolean isReverseLoader() {
        return getDelegate().isReverseLoader();
    }
    public String getLoaderId() {
        return getDelegate().getClassLoadId();
    }
    public String getClasspathId() {
        return getDelegate().getClassLoadId();
    }
    public void setClasspath(Path classpath) {
        getDelegate().setClasspath(classpath);
    }
    public Path createClasspath() {
        return getDelegate().createClasspath();
    }
    public void setClasspathRef(Reference r) {
        getDelegate().setClasspathref(r);
    }
    public void setLoaderRef(Reference r) {
        getDelegate().setLoaderRef(r);
    }
    protected ClassLoader createLoader() {
        if (getAntlibClassLoader() != null && cpDelegate == null) {
            return getAntlibClassLoader();
        }
        if (createdLoader == null) {
            createdLoader = getDelegate().getClassLoader();
            ((AntClassLoader) createdLoader)
                .addSystemPackageRoot("org.apache.tools.ant");
        }
        return createdLoader;
    }
    public void init() throws BuildException {
        super.init();
    }
    private ClasspathUtils.Delegate getDelegate() {
        if (cpDelegate == null) {
            cpDelegate = ClasspathUtils.getDelegate(this);
        }
        return cpDelegate;
    }
}
