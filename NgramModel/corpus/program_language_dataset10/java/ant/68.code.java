package org.apache.tools.ant;
public class ExtensionPoint extends Target {
    private static final String NO_CHILDREN_ALLOWED
        = "you must not nest child elements into an extension-point";
    public final void addTask(Task task) {
        throw new BuildException(NO_CHILDREN_ALLOWED);
    }
    public final void addDataType(RuntimeConfigurable r) {
        throw new BuildException(NO_CHILDREN_ALLOWED);
    }
}