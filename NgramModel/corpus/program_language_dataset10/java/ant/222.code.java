package org.apache.tools.ant.taskdefs;
import java.io.File;
import org.apache.tools.ant.types.resources.FileResource;
public class LoadFile extends LoadResource {
    public final void setSrcFile(final File srcFile) {
        addConfigured(new FileResource(srcFile));
    }
}