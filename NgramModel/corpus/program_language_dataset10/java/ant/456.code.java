package org.apache.tools.ant.taskdefs.optional.javah;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.Javah;
public interface JavahAdapter {
    boolean compile(Javah javah) throws BuildException;
}
