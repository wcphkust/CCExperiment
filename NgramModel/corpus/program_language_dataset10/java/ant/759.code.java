package org.apache.tools.ant.types.spi;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.BuildException;
public class Provider extends ProjectComponent {
    private String type;
    public String getClassName() {
        return type;
    }
    public void setClassName(String type) {
        this.type = type;
    }
    public void check() {
        if (type == null) {
            throw new BuildException(
                "classname attribute must be set for provider element",
                getLocation());
        }
        if (type.length() == 0) {
            throw new BuildException(
                "Invalid empty classname", getLocation());
        }
    }
}
