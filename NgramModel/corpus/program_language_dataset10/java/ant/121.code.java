package org.apache.tools.ant.helper;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.util.FileUtils;
public class AntXMLContext {
    private Project project;
    private File buildFile;
    private URL buildFileURL;
    private Vector targetVector = new Vector();
    private File buildFileParent;
    private URL buildFileParentURL;
    private String currentProjectName;
    private Locator locator;
    private Target implicitTarget = new Target();
    private Target currentTarget = null;
    private Vector wStack = new Vector();
    private boolean ignoreProjectTag = false;
    private Map prefixMapping = new HashMap();
    private Map currentTargets = null;
    public AntXMLContext(Project project) {
        this.project = project;
        implicitTarget.setProject(project);
        implicitTarget.setName("");
        targetVector.addElement(implicitTarget);
    }
    public void setBuildFile(File buildFile) {
        this.buildFile = buildFile;
        if (buildFile != null) {
            this.buildFileParent = new File(buildFile.getParent());
            implicitTarget.setLocation(new Location(buildFile.getAbsolutePath()));
            try {
                setBuildFile(FileUtils.getFileUtils().getFileURL(buildFile));
            } catch (MalformedURLException ex) {
                throw new BuildException(ex);
            }
        } else {
            this.buildFileParent = null;
        }
    }
    public void setBuildFile(URL buildFile) throws MalformedURLException {
        this.buildFileURL = buildFile;
        this.buildFileParentURL = new URL(buildFile, ".");
        if (implicitTarget.getLocation() == null) {
            implicitTarget.setLocation(new Location(buildFile.toString()));
        }
    }
    public File getBuildFile() {
        return buildFile;
    }
    public File getBuildFileParent() {
        return buildFileParent;
    }
    public URL getBuildFileURL() {
        return buildFileURL;
    }
    public URL getBuildFileParentURL() {
        return buildFileParentURL;
    }
    public Project getProject() {
        return project;
    }
    public String getCurrentProjectName() {
        return currentProjectName;
    }
    public void setCurrentProjectName(String name) {
        this.currentProjectName = name;
    }
    public RuntimeConfigurable currentWrapper() {
        if (wStack.size() < 1) {
            return null;
        }
        return (RuntimeConfigurable) wStack.elementAt(wStack.size() - 1);
    }
    public RuntimeConfigurable parentWrapper() {
        if (wStack.size() < 2) {
            return null;
        }
        return (RuntimeConfigurable) wStack.elementAt(wStack.size() - 2);
    }
    public void pushWrapper(RuntimeConfigurable wrapper) {
        wStack.addElement(wrapper);
    }
    public void popWrapper() {
        if (wStack.size() > 0) {
            wStack.removeElementAt(wStack.size() - 1);
        }
    }
    public Vector getWrapperStack() {
        return wStack;
    }
    public void addTarget(Target target) {
        targetVector.addElement(target);
        currentTarget = target;
    }
    public Target getCurrentTarget() {
        return currentTarget;
    }
    public Target getImplicitTarget() {
        return implicitTarget;
    }
    public void setCurrentTarget(Target target) {
        this.currentTarget = target;
    }
    public void setImplicitTarget(Target target) {
        this.implicitTarget = target;
    }
    public Vector getTargets() {
        return targetVector;
    }
    public void configureId(Object element, Attributes attr) {
        String id = attr.getValue("id");
        if (id != null) {
            project.addIdReference(id, element);
        }
    }
    public Locator getLocator() {
        return locator;
    }
    public void setLocator(Locator locator) {
        this.locator = locator;
    }
    public boolean isIgnoringProjectTag() {
        return ignoreProjectTag;
    }
    public void setIgnoreProjectTag(boolean flag) {
        this.ignoreProjectTag = flag;
    }
    public void startPrefixMapping(String prefix, String uri) {
        List list = (List) prefixMapping.get(prefix);
        if (list == null) {
            list = new ArrayList();
            prefixMapping.put(prefix, list);
        }
        list.add(uri);
    }
    public void endPrefixMapping(String prefix) {
        List list = (List) prefixMapping.get(prefix);
        if (list == null || list.size() == 0) {
            return; 
        }
        list.remove(list.size() - 1);
    }
    public String getPrefixMapping(String prefix) {
        List list = (List) prefixMapping.get(prefix);
        if (list == null || list.size() == 0) {
            return null;
        }
        return (String) list.get(list.size() - 1);
    }
    public Map getCurrentTargets() {
        return currentTargets;
    }
    public void setCurrentTargets(Map currentTargets) {
        this.currentTargets = currentTargets;
    }
}
