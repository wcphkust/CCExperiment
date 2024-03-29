package org.apache.tools.ant.taskdefs.optional.script;
import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.taskdefs.DefBase;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.ScriptRunnerBase;
import org.apache.tools.ant.util.ScriptRunnerHelper;
public class ScriptDef extends DefBase {
    private ScriptRunnerHelper helper = new ScriptRunnerHelper();
    private String name;
    private List attributes = new ArrayList();
    private List nestedElements = new ArrayList();
    private Set attributeSet;
    private Map nestedElementMap;
    public void setProject(Project project) {
        super.setProject(project);
        helper.setProjectComponent(this);
        helper.setSetBeans(false);
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isAttributeSupported(String attributeName) {
        return attributeSet.contains(attributeName);
    }
    public static class Attribute {
        private String name;
        public void setName(String name) {
            this.name = name.toLowerCase(Locale.ENGLISH);
        }
    }
    public void addAttribute(Attribute attribute) {
        attributes.add(attribute);
    }
    public static class NestedElement {
        private String name;
        private String type;
        private String className;
        public void setName(String name) {
            this.name = name.toLowerCase(Locale.ENGLISH);
        }
        public void setType(String type) {
            this.type = type;
        }
        public void setClassName(String className) {
            this.className = className;
        }
    }
    public void addElement(NestedElement nestedElement) {
        nestedElements.add(nestedElement);
    }
    public void execute() {
        if (name == null) {
            throw new BuildException("scriptdef requires a name attribute to "
                + "name the script");
        }
        if (helper.getLanguage() == null) {
            throw new BuildException("<scriptdef> requires a language attribute "
                + "to specify the script language");
        }
        if (getAntlibClassLoader() != null || hasCpDelegate()) {
            helper.setClassLoader(createLoader());
        }
        attributeSet = new HashSet();
        for (Iterator i = attributes.iterator(); i.hasNext();) {
            Attribute attribute = (Attribute) i.next();
            if (attribute.name == null) {
                throw new BuildException("scriptdef <attribute> elements "
                    + "must specify an attribute name");
            }
            if (attributeSet.contains(attribute.name)) {
                throw new BuildException("scriptdef <" + name + "> declares "
                    + "the " + attribute.name + " attribute more than once");
            }
            attributeSet.add(attribute.name);
        }
        nestedElementMap = new HashMap();
        for (Iterator i = nestedElements.iterator(); i.hasNext();) {
            NestedElement nestedElement = (NestedElement) i.next();
            if (nestedElement.name == null) {
                throw new BuildException("scriptdef <element> elements "
                    + "must specify an element name");
            }
            if (nestedElementMap.containsKey(nestedElement.name)) {
                throw new BuildException("scriptdef <" + name + "> declares "
                    + "the " + nestedElement.name + " nested element more "
                    + "than once");
            }
            if (nestedElement.className == null
                && nestedElement.type == null) {
                throw new BuildException("scriptdef <element> elements "
                    + "must specify either a classname or type attribute");
            }
            if (nestedElement.className != null
                && nestedElement.type != null) {
                throw new BuildException("scriptdef <element> elements "
                    + "must specify only one of the classname and type "
                    + "attributes");
            }
            nestedElementMap.put(nestedElement.name, nestedElement);
        }
        Map scriptRepository = lookupScriptRepository();
        name = ProjectHelper.genComponentName(getURI(), name);
        scriptRepository.put(name, this);
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(name);
        def.setClass(ScriptDefBase.class);
        ComponentHelper.getComponentHelper(
            getProject()).addDataTypeDefinition(def);
    }
    private Map lookupScriptRepository() {
        Map scriptRepository = null;
        Project p = getProject();
        synchronized (p) {
            scriptRepository =
                    (Map) p.getReference(MagicNames.SCRIPT_REPOSITORY);
            if (scriptRepository == null) {
                scriptRepository = new HashMap();
                p.addReference(MagicNames.SCRIPT_REPOSITORY,
                        scriptRepository);
            }
        }
        return scriptRepository;
    }
    public Object createNestedElement(String elementName) {
        NestedElement definition
            = (NestedElement) nestedElementMap.get(elementName);
        if (definition == null) {
            throw new BuildException("<" + name + "> does not support "
                + "the <" + elementName + "> nested element");
        }
        Object instance = null;
        String classname = definition.className;
        if (classname == null) {
            instance = getProject().createTask(definition.type);
            if (instance == null) {
                instance = getProject().createDataType(definition.type);
            }
        } else {
            ClassLoader loader = createLoader();
            try {
                instance = ClasspathUtils.newInstance(classname, loader);
            } catch (BuildException e) {
                instance = ClasspathUtils.newInstance(classname, ScriptDef.class.getClassLoader());
            }
            getProject().setProjectReference(instance);
        }
        if (instance == null) {
            throw new BuildException("<" + name + "> is unable to create "
                + "the <" + elementName + "> nested element");
        }
        return instance;
    }
    public void executeScript(Map attributes, Map elements) {
        executeScript(attributes, elements, null);
    }
    public void executeScript(Map attributes, Map elements, ScriptDefBase instance) {
        ScriptRunnerBase runner = helper.getScriptRunner();
        runner.addBean("attributes", attributes);
        runner.addBean("elements", elements);
        runner.addBean("project", getProject());
        if (instance != null) {
            runner.addBean("self", instance);
        }
        runner.executeScript("scriptdef_" + name);
    }
    public void setManager(String manager) {
        helper.setManager(manager);
    }
    public void setLanguage(String language) {
        helper.setLanguage(language);
    }
    public void setSrc(File file) {
        helper.setSrc(file);
    }
    public void addText(String text) {
        helper.addText(text);
    }
    public void add(ResourceCollection resource) {
        helper.add(resource);
    }
}
