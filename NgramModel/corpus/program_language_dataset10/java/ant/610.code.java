package org.apache.tools.ant.types;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.PathTokenizer;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.FileResourceIterator;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
public class Path extends DataType implements Cloneable, ResourceCollection {
    public static Path systemClasspath =
        new Path(null, System.getProperty("java.class.path"));
    public static Path systemBootClasspath =
        new Path(null, System.getProperty("sun.boot.class.path"));
    private static final Iterator EMPTY_ITERATOR
        = Collections.EMPTY_SET.iterator();
    public class PathElement implements ResourceCollection {
        private String[] parts;
        public void setLocation(File loc) {
            parts = new String[] {translateFile(loc.getAbsolutePath())};
        }
        public void setPath(String path) {
            parts = Path.translatePath(getProject(), path);
        }
        public String[] getParts() {
            return parts;
        }
        public Iterator iterator() {
            return new FileResourceIterator(getProject(), null, parts);
        }
        public boolean isFilesystemOnly() {
            return true;
        }
        public int size() {
            return parts == null ? 0 : parts.length;
        }
    }
    private Boolean preserveBC;
    private Union union = null;
    private boolean cache = false;
    public Path(Project p, String path) {
        this(p);
        createPathElement().setPath(path);
    }
    public Path(Project project) {
        setProject(project);
    }
    public void setLocation(File location) throws BuildException {
        checkAttributesAllowed();
        createPathElement().setLocation(location);
    }
    public void setPath(String path) throws BuildException {
        checkAttributesAllowed();
        createPathElement().setPath(path);
    }
    public void setRefid(Reference r) throws BuildException {
        if (union != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }
    public PathElement createPathElement() throws BuildException {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        PathElement pe = new PathElement();
        add(pe);
        return pe;
    }
    public void addFileset(FileSet fs) throws BuildException {
        if (fs.getProject() == null) {
            fs.setProject(getProject());
        }
        add(fs);
    }
    public void addFilelist(FileList fl) throws BuildException {
        if (fl.getProject() == null) {
            fl.setProject(getProject());
        }
        add(fl);
    }
    public void addDirset(DirSet dset) throws BuildException {
        if (dset.getProject() == null) {
            dset.setProject(getProject());
        }
        add(dset);
    }
    public void add(Path path) throws BuildException {
        if (path == this) {
            throw circularReference();
        }
        if (path.getProject() == null) {
            path.setProject(getProject());
        }
        add((ResourceCollection) path);
    }
    public void add(ResourceCollection c) {
        checkChildrenAllowed();
        if (c == null) {
            return;
        }
        if (union == null) {
            union = new Union();
            union.setProject(getProject());
            union.setCache(cache);
        }
        union.add(c);
        setChecked(false);
    }
    public Path createPath() throws BuildException {
        Path p = new Path(getProject());
        add(p);
        return p;
    }
    public void append(Path other) {
        if (other == null) {
            return;
        }
        add(other);
    }
     public void addExisting(Path source) {
         addExisting(source, false);
     }
    public void addExisting(Path source, boolean tryUserDir) {
        String[] list = source.list();
        File userDir = (tryUserDir) ? new File(System.getProperty("user.dir"))
                : null;
        for (int i = 0; i < list.length; i++) {
            File f = resolveFile(getProject(), list[i]);
            if (tryUserDir && !f.exists()) {
                f = new File(userDir, list[i]);
            }
            if (f.exists()) {
                setLocation(f);
            } else if (f.getParentFile() != null && f.getParentFile().exists()
                       && containsWildcards(f.getName())) {
                setLocation(f);
                log("adding " + f + " which contains wildcards and may not"
                    + " do what you intend it to do depending on your OS or"
                    + " version of Java", Project.MSG_VERBOSE);
            } else {
                log("dropping " + f + " from path as it doesn't exist",
                    Project.MSG_VERBOSE);
            }
        }
    }
    public void setCache(boolean b) {
        checkAttributesAllowed();
        cache = b;
        if (union != null) {
            union.setCache(b);
        }
    }
    public String[] list() {
        if (isReference()) {
            return ((Path) getCheckedRef()).list();
        }
        return assertFilesystemOnly(union) == null
            ? new String[0] : union.list();
    }
    public String toString() {
        return isReference() ? getCheckedRef().toString()
            : union == null ? "" : union.toString();
    }
    public static String[] translatePath(Project project, String source) {
        final Vector result = new Vector();
        if (source == null) {
            return new String[0];
        }
        PathTokenizer tok = new PathTokenizer(source);
        StringBuffer element = new StringBuffer();
        while (tok.hasMoreTokens()) {
            String pathElement = tok.nextToken();
            try {
                element.append(resolveFile(project, pathElement).getPath());
            } catch (BuildException e) {
                project.log("Dropping path element " + pathElement
                    + " as it is not valid relative to the project",
                    Project.MSG_VERBOSE);
            }
            for (int i = 0; i < element.length(); i++) {
                translateFileSep(element, i);
            }
            result.addElement(element.toString());
            element = new StringBuffer();
        }
        String[] res = new String[result.size()];
        result.copyInto(res);
        return res;
    }
    public static String translateFile(String source) {
        if (source == null) {
          return "";
        }
        final StringBuffer result = new StringBuffer(source);
        for (int i = 0; i < result.length(); i++) {
            translateFileSep(result, i);
        }
        return result.toString();
    }
    protected static boolean translateFileSep(StringBuffer buffer, int pos) {
        if (buffer.charAt(pos) == '/' || buffer.charAt(pos) == '\\') {
            buffer.setCharAt(pos, File.separatorChar);
            return true;
        }
        return false;
    }
    public synchronized int size() {
        if (isReference()) {
            return ((Path) getCheckedRef()).size();
        }
        dieOnCircularReference();
        return union == null ? 0 : assertFilesystemOnly(union).size();
    }
    public Object clone() {
        try {
            Path result = (Path) super.clone();
            result.union = union == null ? union : (Union) union.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
    }
    protected synchronized void dieOnCircularReference(Stack stk, Project p)
        throws BuildException {
        if (isChecked()) {
            return;
        }
        if (isReference()) {
            super.dieOnCircularReference(stk, p);
        } else {
            if (union != null) {
                pushAndInvokeCircularReferenceCheck(union, stk, p);
            }
            setChecked(true);
        }
    }
    private static File resolveFile(Project project, String relativeName) {
        return FileUtils.getFileUtils().resolveFile(
            (project == null) ? null : project.getBaseDir(), relativeName);
    }
    public Path concatSystemClasspath() {
        return concatSystemClasspath("last");
    }
    public Path concatSystemClasspath(String defValue) {
        return concatSpecialPath(defValue, Path.systemClasspath);
    }
    public Path concatSystemBootClasspath(String defValue) {
        return concatSpecialPath(defValue, Path.systemBootClasspath);
    }
    private Path concatSpecialPath(String defValue, Path p) {
        Path result = new Path(getProject());
        String order = defValue;
        String o = getProject() != null 
            ? getProject().getProperty(MagicNames.BUILD_SYSCLASSPATH)
            : System.getProperty(MagicNames.BUILD_SYSCLASSPATH);
        if (o != null) {
            order = o;
        }
        if (order.equals("only")) {
            result.addExisting(p, true);
        } else if (order.equals("first")) {
            result.addExisting(p, true);
            result.addExisting(this);
        } else if (order.equals("ignore")) {
            result.addExisting(this);
        } else {
            if (!order.equals("last")) {
                log("invalid value for " + MagicNames.BUILD_SYSCLASSPATH
                    + ": " + order,
                    Project.MSG_WARN);
            }
            result.addExisting(this);
            result.addExisting(p, true);
        }
        return result;
    }
    public void addJavaRuntime() {
        if (JavaEnvUtils.isKaffe()) {
            File kaffeShare = new File(System.getProperty("java.home")
                                       + File.separator + "share"
                                       + File.separator + "kaffe");
            if (kaffeShare.isDirectory()) {
                FileSet kaffeJarFiles = new FileSet();
                kaffeJarFiles.setDir(kaffeShare);
                kaffeJarFiles.setIncludes("*.jar");
                addFileset(kaffeJarFiles);
            }
        } else if ("GNU libgcj".equals(System.getProperty("java.vm.name"))) {
            addExisting(systemBootClasspath);
        }
        if (System.getProperty("java.vendor").toLowerCase(Locale.ENGLISH).indexOf("microsoft") >= 0) {
            FileSet msZipFiles = new FileSet();
            msZipFiles.setDir(new File(System.getProperty("java.home")
                + File.separator + "Packages"));
            msZipFiles.setIncludes("*.ZIP");
            addFileset(msZipFiles);
        } else {
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + "lib"
                                 + File.separator + "rt.jar"));
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + "jre"
                                 + File.separator + "lib"
                                 + File.separator + "rt.jar"));
            String[] secJars = {"jce", "jsse"};
            for (int i = 0; i < secJars.length; i++) {
                addExisting(new Path(null,
                                     System.getProperty("java.home")
                                     + File.separator + "lib"
                                     + File.separator + secJars[i] + ".jar"));
                addExisting(new Path(null,
                                     System.getProperty("java.home")
                                     + File.separator + ".."
                                     + File.separator + "Classes"
                                     + File.separator + secJars[i] + ".jar"));
            }
            String[] ibmJars
                = {"core", "graphics", "security", "server", "xml"};
            for (int i = 0; i < ibmJars.length; i++) {
                addExisting(new Path(null,
                                     System.getProperty("java.home")
                                     + File.separator + "lib"
                                     + File.separator + ibmJars[i] + ".jar"));
            }
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + ".."
                                 + File.separator + "Classes"
                                 + File.separator + "classes.jar"));
            addExisting(new Path(null,
                                 System.getProperty("java.home")
                                 + File.separator + ".."
                                 + File.separator + "Classes"
                                 + File.separator + "ui.jar"));
        }
    }
    public void addExtdirs(Path extdirs) {
        if (extdirs == null) {
            String extProp = System.getProperty("java.ext.dirs");
            if (extProp != null) {
                extdirs = new Path(getProject(), extProp);
            } else {
                return;
            }
        }
        String[] dirs = extdirs.list();
        for (int i = 0; i < dirs.length; i++) {
            File dir = resolveFile(getProject(), dirs[i]);
            if (dir.exists() && dir.isDirectory()) {
                FileSet fs = new FileSet();
                fs.setDir(dir);
                fs.setIncludes("*");
                addFileset(fs);
            }
        }
    }
    public final synchronized Iterator iterator() {
        if (isReference()) {
            return ((Path) getCheckedRef()).iterator();
        }
        dieOnCircularReference();
        if (getPreserveBC()) {
            return new FileResourceIterator(getProject(), null, list());
        }
        return union == null ? EMPTY_ITERATOR
            : assertFilesystemOnly(union).iterator();
    }
    public synchronized boolean isFilesystemOnly() {
        if (isReference()) {
            return ((Path) getCheckedRef()).isFilesystemOnly();
        }
        dieOnCircularReference();
        assertFilesystemOnly(union);
        return true;
    }
    protected ResourceCollection assertFilesystemOnly(ResourceCollection rc) {
        if (rc != null && !(rc.isFilesystemOnly())) {
            throw new BuildException(getDataTypeName()
                + " allows only filesystem resources.");
        }
        return rc;
    }
    protected boolean delegateIteratorToList() {
        if (getClass().equals(Path.class)) {
            return false;
        }
        try {
            Method listMethod = getClass().getMethod("list", (Class[]) null);
            return !listMethod.getDeclaringClass().equals(Path.class);
        } catch (Exception e) {
            return false;
        }
    }
    private synchronized boolean getPreserveBC() {
        if (preserveBC == null) {
            preserveBC = delegateIteratorToList() ? Boolean.TRUE : Boolean.FALSE;
        }
        return preserveBC.booleanValue();
    }
    private static boolean containsWildcards(String path) {
        return path != null
            && (path.indexOf("*") > -1 || path.indexOf("?") > -1);
    }
}
