package org.apache.tools.ant.types;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.PropertyResource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
public class PropertySet extends DataType implements ResourceCollection {
    private boolean dynamic = true;
    private boolean negate = false;
    private Set cachedNames;
    private Vector ptyRefs = new Vector();
    private Vector setRefs = new Vector();
    private Mapper mapper;
    public static class PropertyRef {
        private int count;
        private String name;
        private String regex;
        private String prefix;
        private String builtin;
        public void setName(String name) {
            assertValid("name", name);
            this.name = name;
        }
        public void setRegex(String regex) {
            assertValid("regex", regex);
            this.regex = regex;
        }
        public void setPrefix(String prefix) {
            assertValid("prefix", prefix);
            this.prefix = prefix;
        }
        public void setBuiltin(BuiltinPropertySetName b) {
            String pBuiltIn = b.getValue();
            assertValid("builtin", pBuiltIn);
            this.builtin = pBuiltIn;
        }
        private void assertValid(String attr, String value) {
            if (value == null || value.length() < 1) {
                throw new BuildException("Invalid attribute: " + attr);
            }
            if (++count != 1) {
                throw new BuildException("Attributes name, regex, and "
                    + "prefix are mutually exclusive");
            }
        }
        public String toString() {
            return "name=" + name + ", regex=" + regex + ", prefix=" + prefix
                + ", builtin=" + builtin;
        }
    } 
    public void appendName(String name) {
        PropertyRef r = new PropertyRef();
        r.setName(name);
        addPropertyref(r);
    }
    public void appendRegex(String regex) {
        PropertyRef r = new PropertyRef();
        r.setRegex(regex);
        addPropertyref(r);
    }
    public void appendPrefix(String prefix) {
        PropertyRef r = new PropertyRef();
        r.setPrefix(prefix);
        addPropertyref(r);
    }
    public void appendBuiltin(BuiltinPropertySetName b) {
        PropertyRef r = new PropertyRef();
        r.setBuiltin(b);
        addPropertyref(r);
    }
    public void setMapper(String type, String from, String to) {
        Mapper m = createMapper();
        Mapper.MapperType mapperType = new Mapper.MapperType();
        mapperType.setValue(type);
        m.setType(mapperType);
        m.setFrom(from);
        m.setTo(to);
    }
    public void addPropertyref(PropertyRef ref) {
        assertNotReference();
        setChecked(false);
        ptyRefs.addElement(ref);
    }
    public void addPropertyset(PropertySet ref) {
        assertNotReference();
        setChecked(false);
        setRefs.addElement(ref);
    }
    public Mapper createMapper() {
        assertNotReference();
        if (mapper != null) {
            throw new BuildException("Too many <mapper>s!");
        }
        mapper = new Mapper(getProject());
        setChecked(false);
        return mapper;
    }
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }
    public void setDynamic(boolean dynamic) {
        assertNotReference();
        this.dynamic = dynamic;
    }
    public void setNegate(boolean negate) {
        assertNotReference();
        this.negate = negate;
    }
    public boolean getDynamic() {
        if (isReference()) {
            return getRef().dynamic;
        }
        dieOnCircularReference();
        return dynamic;
    }
    public Mapper getMapper() {
        if (isReference()) {
            return getRef().mapper;
        }
        dieOnCircularReference();
        return mapper;
    }
    private Hashtable getAllSystemProperties() {
        Hashtable ret = new Hashtable();
        for (Enumeration e = System.getProperties().propertyNames();
             e.hasMoreElements();) {
            String name = (String) e.nextElement();
            ret.put(name, System.getProperties().getProperty(name));
        }
        return ret;
    }
    public Properties getProperties() {
        if (isReference()) {
            return getRef().getProperties();
        }
        dieOnCircularReference();
        Set names = null;
        Project prj = getProject();
        Hashtable props =
            prj == null ? getAllSystemProperties() : prj.getProperties();
        for (Enumeration e = setRefs.elements(); e.hasMoreElements();) {
            PropertySet set = (PropertySet) e.nextElement();
            props.putAll(set.getProperties());
        }
        if (getDynamic() || cachedNames == null) {
            names = new HashSet();
            addPropertyNames(names, props);
            for (Enumeration e = setRefs.elements(); e.hasMoreElements();) {
                PropertySet set = (PropertySet) e.nextElement();
                names.addAll(set.getProperties().keySet());
            }
            if (negate) {
                HashSet complement = new HashSet(props.keySet());
                complement.removeAll(names);
                names = complement;
            }
            if (!getDynamic()) {
                cachedNames = names;
            }
        } else {
            names = cachedNames;
        }
        FileNameMapper m = null;
        Mapper myMapper = getMapper();
        if (myMapper != null) {
            m = myMapper.getImplementation();
        }
        Properties properties = new Properties();
        for (Iterator iter = names.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String value = (String) props.get(name);
            if (value != null) {
                if (m != null) {
                    String[] newname = m.mapFileName(name);
                    if (newname != null) {
                        name = newname[0];
                    }
                }
                properties.setProperty(name, value);
            }
        }
        return properties;
    }
    private void addPropertyNames(Set names, Hashtable properties) {
        if (isReference()) {
            getRef().addPropertyNames(names, properties);
        }
        dieOnCircularReference();
        for (Enumeration e = ptyRefs.elements(); e.hasMoreElements();) {
            PropertyRef r = (PropertyRef) e.nextElement();
            if (r.name != null) {
                if (properties.get(r.name) != null) {
                    names.add(r.name);
                }
            } else if (r.prefix != null) {
                for (Enumeration p = properties.keys(); p.hasMoreElements();) {
                    String name = (String) p.nextElement();
                    if (name.startsWith(r.prefix)) {
                        names.add(name);
                    }
                }
            } else if (r.regex != null) {
                RegexpMatcherFactory matchMaker = new RegexpMatcherFactory();
                RegexpMatcher matcher = matchMaker.newRegexpMatcher();
                matcher.setPattern(r.regex);
                for (Enumeration p = properties.keys(); p.hasMoreElements();) {
                    String name = (String) p.nextElement();
                    if (matcher.matches(name)) {
                        names.add(name);
                    }
                }
            } else if (r.builtin != null) {
                if (r.builtin.equals(BuiltinPropertySetName.ALL)) {
                    names.addAll(properties.keySet());
                } else if (r.builtin.equals(BuiltinPropertySetName.SYSTEM)) {
                    names.addAll(System.getProperties().keySet());
                } else if (r.builtin.equals(BuiltinPropertySetName
                                              .COMMANDLINE)) {
                    names.addAll(getProject().getUserProperties().keySet());
                } else {
                    throw new BuildException("Impossible: Invalid builtin "
                                             + "attribute!");
                }
            } else {
                throw new BuildException("Impossible: Invalid PropertyRef!");
            }
        }
    }
    protected PropertySet getRef() {
        return (PropertySet) getCheckedRef(PropertySet.class, "propertyset");
    }
    public final void setRefid(Reference r) {
        if (!noAttributeSet) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }
    protected final void assertNotReference() {
        if (isReference()) {
            throw tooManyAttributes();
        }
        noAttributeSet = false;
    }
    private boolean noAttributeSet = true;
    public static class BuiltinPropertySetName extends EnumeratedAttribute {
        static final String ALL = "all";
        static final String SYSTEM = "system";
        static final String COMMANDLINE = "commandline";
        public String[] getValues() {
            return new String[] {ALL, SYSTEM, COMMANDLINE};
        }
    }
    public String toString() {
        if (isReference()) {
            return getRef().toString();
        }
        dieOnCircularReference();
        StringBuffer b = new StringBuffer();
        TreeMap sorted = new TreeMap(getProperties());
        for (Iterator i = sorted.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (b.length() != 0) {
                b.append(", ");
            }
            b.append(e.getKey().toString());
            b.append("=");
            b.append(e.getValue().toString());
        }
        return b.toString();
    }
    public Iterator iterator() {
        if (isReference()) {
            return getRef().iterator();
        }
        dieOnCircularReference();
        final Enumeration e = getProperties().propertyNames();
        return new Iterator() {
            public boolean hasNext() {
                return e.hasMoreElements();
            }
            public Object next() {
                return new PropertyResource(getProject(), (String) e.nextElement());
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    public int size() {
        return isReference() ? getRef().size() : getProperties().size();
    }
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        }
        dieOnCircularReference();
        return false;
    }
    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (mapper != null) {
                pushAndInvokeCircularReferenceCheck(mapper, stk, p);
            }
            for (Iterator i = setRefs.iterator(); i.hasNext(); ) {
                pushAndInvokeCircularReferenceCheck((PropertySet) i.next(), stk,
                                                    p);
            }
            setChecked(true);
        }
    }
}
