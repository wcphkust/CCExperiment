package org.apache.tools.ant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.tools.ant.property.LocalProperties;
public class Target implements TaskContainer {
    private String name;
    private String ifCondition = "";
    private String unlessCondition = "";
    private List dependencies = null;
    private List children = new ArrayList();
    private Location location = Location.UNKNOWN_LOCATION;
    private Project project;
    private String description = null;
    public Target() {
    }
    public Target(Target other) {
        this.name = other.name;
        this.ifCondition = other.ifCondition;
        this.unlessCondition = other.unlessCondition;
        this.dependencies = other.dependencies;
        this.location = other.location;
        this.project = other.project;
        this.description = other.description;
        this.children = other.children;
    }
    public void setProject(Project project) {
        this.project = project;
    }
    public Project getProject() {
        return project;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public Location getLocation() {
        return location;
    }
    public void setDepends(String depS) {
        for (Iterator iter = parseDepends(depS, getName(), "depends").iterator();
             iter.hasNext(); ) {
            addDependency((String) iter.next());
        }
    }
    public static List parseDepends(String depends,
                                                String targetName,
                                                String attributeName) {
        ArrayList list = new ArrayList();
        if (depends.length() > 0) {
            StringTokenizer tok =
                new StringTokenizer(depends, ",", true);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();
                if ("".equals(token) || ",".equals(token)) {
                    throw new BuildException("Syntax Error: "
                                             + attributeName
                                             + " attribute of target \""
                                             + targetName
                                             + "\" contains an empty string.");
                }
                list.add(token);
                if (tok.hasMoreTokens()) {
                    token = tok.nextToken();
                    if (!tok.hasMoreTokens() || !",".equals(token)) {
                        throw new BuildException("Syntax Error: "
                                                 + attributeName
                                                 + " attribute for target \""
                                                 + targetName
                                                 + "\" ends with a \",\" "
                                                 + "character");
                    }
                }
            }
        }
        return list;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void addTask(Task task) {
        children.add(task);
    }
    public void addDataType(RuntimeConfigurable r) {
        children.add(r);
    }
    public Task[] getTasks() {
        List tasks = new ArrayList(children.size());
        Iterator it = children.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof Task) {
                tasks.add(o);
            }
        }
        return (Task[]) tasks.toArray(new Task[tasks.size()]);
    }
    public void addDependency(String dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList(2);
        }
        dependencies.add(dependency);
    }
    public Enumeration getDependencies() {
        return Collections
                .enumeration(dependencies == null ? Collections.EMPTY_LIST : dependencies);
    }
    public boolean dependsOn(String other) {
        Project p = getProject();
        Hashtable t = p == null ? null : p.getTargets();
        return p != null && p.topoSort(getName(), t, false).contains(t.get(other));
    }
    public void setIf(String property) {
        ifCondition = property == null ? "" : property;
    }
    public String getIf() {
        return "".equals(ifCondition) ? null : ifCondition;
    }
    public void setUnless(String property) {
        unlessCondition = property == null ? "" : property;
    }
    public String getUnless() {
        return "".equals(unlessCondition) ? null : unlessCondition;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public String toString() {
        return name;
    }
    public void execute() throws BuildException {
        if (!testIfAllows()) {
            project.log(this, "Skipped because property '" + project.replaceProperties(ifCondition)
                    + "' not set.", Project.MSG_VERBOSE);
            return;
        }
        if (!testUnlessAllows()) {
            project.log(this, "Skipped because property '"
                    + project.replaceProperties(unlessCondition) + "' set.", Project.MSG_VERBOSE);
            return;
        }
        LocalProperties localProperties = LocalProperties.get(getProject());
        localProperties.enterScope();
        try {
            for (int i = 0; i < children.size(); i++) {
                Object o = children.get(i);
                if (o instanceof Task) {
                    Task task = (Task) o;
                    task.perform();
                } else {
                    ((RuntimeConfigurable) o).maybeConfigure(project);
                }
            }
        } finally {
            localProperties.exitScope();
        }
    }
    public final void performTasks() {
        RuntimeException thrown = null;
        project.fireTargetStarted(this);
        try {
            execute();
        } catch (RuntimeException exc) {
            thrown = exc;
            throw exc;
        } finally {
            project.fireTargetFinished(this, thrown);
        }
    }
    void replaceChild(Task el, RuntimeConfigurable o) {
        int index;
        while ((index = children.indexOf(el)) >= 0) {
            children.set(index, o);
        }
    }
    void replaceChild(Task el, Task o) {
        int index;
        while ((index = children.indexOf(el)) >= 0) {
            children.set(index, o);
        }
    }
    private boolean testIfAllows() {
        PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(getProject());
        Object o = propertyHelper.parseProperties(ifCondition);
        return propertyHelper.testIfCondition(o);
    }
    private boolean testUnlessAllows() {
        PropertyHelper propertyHelper = PropertyHelper.getPropertyHelper(getProject());
        Object o = propertyHelper.parseProperties(unlessCondition);
        return propertyHelper.testUnlessCondition(o);
    }
}
