package org.apache.tools.ant.util.depend;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import org.apache.tools.ant.types.Path;
public interface DependencyAnalyzer {
    void addSourcePath(Path sourcePath);
    void addClassPath(Path classpath);
    void addRootClass(String classname);
    Enumeration getFileDependencies();
    Enumeration getClassDependencies();
    void reset();
    void config(String name, Object info);
    void setClosure(boolean closure);
    File getClassContainer(String classname) throws IOException;
    File getSourceContainer(String classname) throws IOException;
}