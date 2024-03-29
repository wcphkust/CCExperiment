package org.apache.tools.ant.dispatch;
import org.apache.tools.ant.Task;
public abstract class DispatchTask extends Task implements Dispatchable {
    private String action;
    public String getActionParameterName() {
        return "action";
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getAction() {
        return action;
    }
}
