package org.apache.tools.ant.taskdefs.condition;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
public class IsTrue extends ProjectComponent implements Condition {
    private Boolean value = null;
    public void setValue(boolean value) {
        this.value = value ? Boolean.TRUE : Boolean.FALSE;
    }
    public boolean eval() throws BuildException {
        if (value == null) {
            throw new BuildException("Nothing to test for truth");
        }
        return value.booleanValue();
    }
}
