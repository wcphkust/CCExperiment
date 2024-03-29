package org.apache.tools.ant.taskdefs;
import java.util.Iterator;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.property.LocalProperties;
public class Sequential extends Task implements TaskContainer {
    private Vector nestedTasks = new Vector();
    public void addTask(Task nestedTask) {
        nestedTasks.addElement(nestedTask);
    }
    public void execute() throws BuildException {
        LocalProperties localProperties
            = LocalProperties.get(getProject());
        localProperties.enterScope();
        try {
            for (Iterator i = nestedTasks.iterator(); i.hasNext();) {
                Task nestedTask = (Task) i.next();
                nestedTask.perform();
            }
        } finally {
            localProperties.exitScope();
        }
    }
}
