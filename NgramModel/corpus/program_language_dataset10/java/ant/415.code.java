package org.apache.tools.ant.taskdefs.optional.ejb;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import javax.xml.parsers.SAXParser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.depend.DependencyAnalyzer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
public class GenericDeploymentTool implements EJBDeploymentTool {
    public static final int DEFAULT_BUFFER_SIZE = 1024;
    public static final int JAR_COMPRESS_LEVEL  = 9;
    protected static final String META_DIR  = "META-INF/";
    protected static final String MANIFEST  = META_DIR + "MANIFEST.MF";
    protected static final String EJB_DD    = "ejb-jar.xml";
    public static final String ANALYZER_SUPER = "super";
    public static final String ANALYZER_FULL = "full";
    public static final String ANALYZER_NONE = "none";
    public static final String DEFAULT_ANALYZER = ANALYZER_SUPER;
    public static final String ANALYZER_CLASS_SUPER
        = "org.apache.tools.ant.util.depend.bcel.AncestorAnalyzer";
    public static final String ANALYZER_CLASS_FULL
        = "org.apache.tools.ant.util.depend.bcel.FullAnalyzer";
    private EjbJar.Config config;
    private File destDir;
    private Path classpath;
    private String genericJarSuffix = "-generic.jar";
    private Task task;
    private ClassLoader classpathLoader = null;
    private Set addedfiles;
    private DescriptorHandler handler;
    private DependencyAnalyzer dependencyAnalyzer;
    public GenericDeploymentTool() {
    }
    public void setDestdir(File inDir) {
        this.destDir = inDir;
    }
    protected File getDestDir() {
        return destDir;
    }
    public void setTask(Task task) {
        this.task = task;
    }
    protected Task getTask() {
        return task;
    }
    protected EjbJar.Config getConfig() {
        return config;
    }
    protected boolean usingBaseJarName() {
        return config.baseJarName != null;
    }
    public void setGenericJarSuffix(String inString) {
        this.genericJarSuffix = inString;
    }
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(task.getProject());
        }
        return classpath.createPath();
    }
    public void setClasspath(Path classpath) {
        this.classpath = classpath;
    }
    protected Path getCombinedClasspath() {
        Path combinedPath = classpath;
        if (config.classpath != null) {
            if (combinedPath == null) {
                combinedPath = config.classpath;
            } else {
                combinedPath.append(config.classpath);
            }
        }
        return combinedPath;
    }
    protected void log(String message, int level) {
        getTask().log(message, level);
    }
    protected Location getLocation() {
        return getTask().getLocation();
    }
    private void createAnalyzer() {
        String analyzer = config.analyzer;
        if (analyzer == null) {
            analyzer = DEFAULT_ANALYZER;
        }
        if (analyzer.equals(ANALYZER_NONE)) {
            return;
        }
        String analyzerClassName = null;
        if (analyzer.equals(ANALYZER_SUPER)) {
            analyzerClassName = ANALYZER_CLASS_SUPER;
        } else if (analyzer.equals(ANALYZER_FULL)) {
            analyzerClassName = ANALYZER_CLASS_FULL;
        } else {
            analyzerClassName = analyzer;
        }
        try {
            Class analyzerClass = Class.forName(analyzerClassName);
            dependencyAnalyzer
                = (DependencyAnalyzer) analyzerClass.newInstance();
            dependencyAnalyzer.addClassPath(new Path(task.getProject(),
                config.srcDir.getPath()));
            dependencyAnalyzer.addClassPath(config.classpath);
        } catch (NoClassDefFoundError e) {
            dependencyAnalyzer = null;
            task.log("Unable to load dependency analyzer: " + analyzerClassName
                + " - dependent class not found: " + e.getMessage(),
                Project.MSG_WARN);
        } catch (Exception e) {
            dependencyAnalyzer = null;
            task.log("Unable to load dependency analyzer: " + analyzerClassName
                     + " - exception: " + e.getMessage(),
                Project.MSG_WARN);
        }
    }
    public void configure(EjbJar.Config config) {
        this.config = config;
        createAnalyzer();
        classpathLoader = null;
    }
    protected void addFileToJar(JarOutputStream jStream,
                                File inputFile,
                                String logicalFilename)
        throws BuildException {
        FileInputStream iStream = null;
        try {
            if (!addedfiles.contains(logicalFilename)) {
                iStream = new FileInputStream(inputFile);
                ZipEntry zipEntry = new ZipEntry(logicalFilename.replace('\\', '/'));
                jStream.putNextEntry(zipEntry);
                byte[] byteBuffer = new byte[2 * DEFAULT_BUFFER_SIZE];
                int count = 0;
                do {
                    jStream.write(byteBuffer, 0, count);
                    count = iStream.read(byteBuffer, 0, byteBuffer.length);
                } while (count != -1);
                addedfiles.add(logicalFilename);
           }
        } catch (IOException ioe) {
            log("WARNING: IOException while adding entry "
                + logicalFilename + " to jarfile from "
                + inputFile.getPath() + " "  + ioe.getClass().getName()
                + "-" + ioe.getMessage(), Project.MSG_WARN);
        } finally {
            if (iStream != null) {
                try {
                    iStream.close();
                } catch (IOException closeException) {
                }
            }
        }
    }
    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler h = new DescriptorHandler(getTask(), srcDir);
        registerKnownDTDs(h);
        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation) i.next();
            h.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return h;
    }
    protected void registerKnownDTDs(DescriptorHandler handler) {
    }
    public void processDescriptor(String descriptorFileName, SAXParser saxParser) {
        checkConfiguration(descriptorFileName, saxParser);
        try {
            handler = getDescriptorHandler(config.srcDir);
            Hashtable ejbFiles = parseEjbFiles(descriptorFileName, saxParser);
            addSupportClasses(ejbFiles);
            String baseName = getJarBaseName(descriptorFileName);
            String ddPrefix = getVendorDDPrefix(baseName, descriptorFileName);
            File manifestFile = getManifestFile(ddPrefix);
            if (manifestFile != null) {
                ejbFiles.put(MANIFEST, manifestFile);
            }
            ejbFiles.put(META_DIR + EJB_DD,
                         new File(config.descriptorDir, descriptorFileName));
            addVendorFiles(ejbFiles, ddPrefix);
            checkAndAddDependants(ejbFiles);
            if (config.flatDestDir && baseName.length() != 0) {
                int startName = baseName.lastIndexOf(File.separator);
                if (startName == -1) {
                    startName = 0;
                }
                int endName   = baseName.length();
                baseName = baseName.substring(startName, endName);
            }
            File jarFile = getVendorOutputJarFile(baseName);
            if (needToRebuild(ejbFiles, jarFile)) {
                log("building "
                              + jarFile.getName()
                              + " with "
                              + String.valueOf(ejbFiles.size())
                              + " files",
                              Project.MSG_INFO);
                String publicId = getPublicId();
                writeJar(baseName, jarFile, ejbFiles, publicId);
            } else {
                log(jarFile.toString() + " is up to date.",
                              Project.MSG_VERBOSE);
            }
        } catch (SAXException se) {
            String msg = "SAXException while parsing '"
                + descriptorFileName
                + "'. This probably indicates badly-formed XML."
                + "  Details: "
                + se.getMessage();
            throw new BuildException(msg, se);
        } catch (IOException ioe) {
            String msg = "IOException while parsing'"
                + descriptorFileName
                + "'.  This probably indicates that the descriptor"
                + " doesn't exist. Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        }
    }
    protected void checkConfiguration(String descriptorFileName,
                                    SAXParser saxParser) throws BuildException {
    }
    protected Hashtable parseEjbFiles(String descriptorFileName, SAXParser saxParser)
                            throws IOException, SAXException {
        FileInputStream descriptorStream = null;
        Hashtable ejbFiles = null;
        try {
            descriptorStream
                = new FileInputStream(new File(config.descriptorDir, descriptorFileName));
            saxParser.parse(new InputSource(descriptorStream), handler);
            ejbFiles = handler.getFiles();
        } finally {
            if (descriptorStream != null) {
                try {
                    descriptorStream.close();
                } catch (IOException closeException) {
                }
            }
        }
        return ejbFiles;
    }
    protected void addSupportClasses(Hashtable ejbFiles) {
        Project project = task.getProject();
        for (Iterator i = config.supportFileSets.iterator(); i.hasNext();) {
            FileSet supportFileSet = (FileSet) i.next();
            File supportBaseDir = supportFileSet.getDir(project);
            DirectoryScanner supportScanner = supportFileSet.getDirectoryScanner(project);
            supportScanner.scan();
            String[] supportFiles = supportScanner.getIncludedFiles();
            for (int j = 0; j < supportFiles.length; ++j) {
                ejbFiles.put(supportFiles[j], new File(supportBaseDir, supportFiles[j]));
            }
        }
    }
    protected String getJarBaseName(String descriptorFileName) {
        String baseName = "";
        if (config.namingScheme.getValue().equals(EjbJar.NamingScheme.BASEJARNAME)) {
            String canonicalDescriptor = descriptorFileName.replace('\\', '/');
            int index = canonicalDescriptor.lastIndexOf('/');
            if (index != -1) {
                baseName = descriptorFileName.substring(0, index + 1);
            }
            baseName += config.baseJarName;
        } else if (config.namingScheme.getValue().equals(EjbJar.NamingScheme.DESCRIPTOR)) {
            int lastSeparatorIndex = descriptorFileName.lastIndexOf(File.separator);
            int endBaseName = -1;
            if (lastSeparatorIndex != -1) {
                endBaseName = descriptorFileName.indexOf(config.baseNameTerminator,
                                                            lastSeparatorIndex);
            } else {
                endBaseName = descriptorFileName.indexOf(config.baseNameTerminator);
            }
            if (endBaseName != -1) {
                baseName = descriptorFileName.substring(0, endBaseName);
            } else {
                throw new BuildException("Unable to determine jar name "
                    + "from descriptor \"" + descriptorFileName + "\"");
            }
        } else if (config.namingScheme.getValue().equals(EjbJar.NamingScheme.DIRECTORY)) {
            File descriptorFile = new File(config.descriptorDir, descriptorFileName);
            String path = descriptorFile.getAbsolutePath();
            int lastSeparatorIndex
                = path.lastIndexOf(File.separator);
            if (lastSeparatorIndex == -1) {
                throw new BuildException("Unable to determine directory name holding descriptor");
            }
            String dirName = path.substring(0, lastSeparatorIndex);
            int dirSeparatorIndex = dirName.lastIndexOf(File.separator);
            if (dirSeparatorIndex != -1) {
                dirName = dirName.substring(dirSeparatorIndex + 1);
            }
            baseName = dirName;
        } else if (config.namingScheme.getValue().equals(EjbJar.NamingScheme.EJB_NAME)) {
            baseName = handler.getEjbName();
        }
        return baseName;
    }
    public String getVendorDDPrefix(String baseName, String descriptorFileName) {
        String ddPrefix = null;
        if (config.namingScheme.getValue().equals(EjbJar.NamingScheme.DESCRIPTOR)) {
            ddPrefix = baseName + config.baseNameTerminator;
        } else if (config.namingScheme.getValue().equals(EjbJar.NamingScheme.BASEJARNAME)
            || config.namingScheme.getValue().equals(EjbJar.NamingScheme.EJB_NAME)
            || config.namingScheme.getValue().equals(EjbJar.NamingScheme.DIRECTORY)) {
            String canonicalDescriptor = descriptorFileName.replace('\\', '/');
            int index = canonicalDescriptor.lastIndexOf('/');
            if (index == -1) {
                ddPrefix = "";
            } else {
                ddPrefix = descriptorFileName.substring(0, index + 1);
            }
        }
        return ddPrefix;
    }
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
    }
    File getVendorOutputJarFile(String baseName) {
        return new File(destDir, baseName + genericJarSuffix);
    }
    protected boolean needToRebuild(Hashtable ejbFiles, File jarFile) {
        if (jarFile.exists()) {
            long lastBuild = jarFile.lastModified();
            Iterator fileIter = ejbFiles.values().iterator();
            while (fileIter.hasNext()) {
                File currentFile = (File) fileIter.next();
                if (lastBuild < currentFile.lastModified()) {
                    log("Build needed because " + currentFile.getPath() + " is out of date",
                        Project.MSG_VERBOSE);
                    return true;
                }
            }
            return false;
        }
        return true;
    }
    protected String getPublicId() {
        return handler.getPublicId();
    }
    protected File getManifestFile(String prefix) {
        File manifestFile
            = new File(getConfig().descriptorDir, prefix + "manifest.mf");
        if (manifestFile.exists()) {
            return manifestFile;
        }
        if (config.manifest != null) {
            return config.manifest;
        }
        return null;
    }
    protected void writeJar(String baseName, File jarfile, Hashtable files,
                            String publicId) throws BuildException {
        JarOutputStream jarStream = null;
        try {
            if (addedfiles == null) {
                addedfiles = new HashSet();
            } else {
                addedfiles.clear();
            }
            if (jarfile.exists()) {
                jarfile.delete();
            }
            jarfile.getParentFile().mkdirs();
            jarfile.createNewFile();
            InputStream in = null;
            Manifest manifest = null;
            try {
                File manifestFile = (File) files.get(MANIFEST);
                if (manifestFile != null && manifestFile.exists()) {
                    in = new FileInputStream(manifestFile);
                } else {
                    String defaultManifest = "/org/apache/tools/ant/defaultManifest.mf";
                    in = this.getClass().getResourceAsStream(defaultManifest);
                    if (in == null) {
                        throw new BuildException("Could not find "
                            + "default manifest: " + defaultManifest);
                    }
                }
                manifest = new Manifest(in);
            } catch (IOException e) {
                throw new BuildException ("Unable to read manifest", e, getLocation());
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            jarStream = new JarOutputStream(new FileOutputStream(jarfile), manifest);
            jarStream.setMethod(JarOutputStream.DEFLATED);
            for (Iterator entryIterator = files.keySet().iterator(); entryIterator.hasNext();) {
                String entryName = (String) entryIterator.next();
                if (entryName.equals(MANIFEST)) {
                    continue;
                }
                File entryFile = (File) files.get(entryName);
                log("adding file '" + entryName + "'",
                              Project.MSG_VERBOSE);
                addFileToJar(jarStream, entryFile, entryName);
                InnerClassFilenameFilter flt = new InnerClassFilenameFilter(entryFile.getName());
                File entryDir = entryFile.getParentFile();
                String[] innerfiles = entryDir.list(flt);
                if (innerfiles != null) {
                    for (int i = 0, n = innerfiles.length; i < n; i++) {
                        int entryIndex = entryName.lastIndexOf(entryFile.getName()) - 1;
                        if (entryIndex < 0) {
                            entryName = innerfiles[i];
                        } else {
                            entryName = entryName.substring(0, entryIndex)
                                + File.separatorChar + innerfiles[i];
                        }
                        entryFile = new File(config.srcDir, entryName);
                        log("adding innerclass file '" + entryName + "'",
                                Project.MSG_VERBOSE);
                        addFileToJar(jarStream, entryFile, entryName);
                    }
                }
            }
        } catch (IOException ioe) {
            String msg = "IOException while processing ejb-jar file '"
                + jarfile.toString()
                + "'. Details: "
                + ioe.getMessage();
            throw new BuildException(msg, ioe);
        } finally {
            if (jarStream != null) {
                try {
                    jarStream.close();
                } catch (IOException closeException) {
                }
            }
        }
    } 
    protected void checkAndAddDependants(Hashtable checkEntries)
        throws BuildException {
        if (dependencyAnalyzer == null) {
            return;
        }
        dependencyAnalyzer.reset();
        Iterator i = checkEntries.keySet().iterator();
        while (i.hasNext()) {
            String entryName = (String) i.next();
            if (entryName.endsWith(".class")) {
                String className = entryName.substring(0,
                    entryName.length() - ".class".length());
                className = className.replace(File.separatorChar, '/');
                className = className.replace('/', '.');
                dependencyAnalyzer.addRootClass(className);
            }
        }
        Enumeration e = dependencyAnalyzer.getClassDependencies();
        while (e.hasMoreElements()) {
            String classname = (String) e.nextElement();
            String location
                = classname.replace('.', File.separatorChar) + ".class";
            File classFile = new File(config.srcDir, location);
            if (classFile.exists()) {
                checkEntries.put(location, classFile);
                log("dependent class: " + classname + " - " + classFile,
                    Project.MSG_VERBOSE);
            }
        }
    }
    protected ClassLoader getClassLoaderForBuild() {
        if (classpathLoader != null) {
            return classpathLoader;
        }
        Path combinedClasspath = getCombinedClasspath();
        if (combinedClasspath == null) {
            classpathLoader = getClass().getClassLoader();
        } else {
            classpathLoader
                = getTask().getProject().createClassLoader(combinedClasspath);
        }
        return classpathLoader;
    }
    public void validateConfigured() throws BuildException {
        if ((destDir == null) || (!destDir.isDirectory())) {
            String msg = "A valid destination directory must be specified "
                            + "using the \"destdir\" attribute.";
            throw new BuildException(msg, getLocation());
        }
    }
}