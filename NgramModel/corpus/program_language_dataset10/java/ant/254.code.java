package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Comparison;
import org.apache.tools.ant.types.ResourceCollection;
public class ResourceCount extends Task implements Condition {
    private static final String ONE_NESTED_MESSAGE
        = "ResourceCount can count resources from exactly one nested ResourceCollection.";
    private static final String COUNT_REQUIRED
        = "Use of the ResourceCount condition requires that the count attribute be set.";
    private ResourceCollection rc;
    private Comparison when = Comparison.EQUAL;
    private Integer count;
    private String property;
    public void add(ResourceCollection r) {
        if (rc != null) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        rc = r;
    }
    public void setRefid(Reference r) {
        Object o = r.getReferencedObject();
        if (!(o instanceof ResourceCollection)) {
            throw new BuildException(r.getRefId()
                + " doesn\'t denote a ResourceCollection");
        }
        add((ResourceCollection) o);
    }
    public void execute() {
        if (rc == null) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        if (property == null) {
            log("resource count = " + rc.size());
        } else {
            getProject().setNewProperty(property, Integer.toString(rc.size()));
        }
    }
    public boolean eval() {
        if (rc == null) {
            throw new BuildException(ONE_NESTED_MESSAGE);
        }
        if (count == null) {
            throw new BuildException(COUNT_REQUIRED);
        }
        return when.evaluate(new Integer(rc.size()).compareTo(count));
    }
    public void setCount(int c) {
        count = new Integer(c);
    }
    public void setWhen(Comparison c) {
        when = c;
    }
    public void setProperty(String p) {
        property = p;
    }
}
