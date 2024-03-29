package org.apache.tools.ant.types.optional;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
public class ScriptCondition extends AbstractScriptComponent implements Condition {
    private boolean value = false;
    public boolean eval() throws BuildException {
        initScriptRunner();
        executeScript("ant_condition");
        return getValue();
    }
    public boolean getValue() {
        return value;
    }
    public void setValue(boolean value) {
        this.value = value;
    }
}
