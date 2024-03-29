package org.apache.tools.ant.taskdefs.optional.ejb;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.xml.parsers.SAXParser;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
public class JonasDeploymentTool extends GenericDeploymentTool {
    protected static final String EJB_JAR_1_1_PUBLIC_ID
        = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    protected static final String EJB_JAR_2_0_PUBLIC_ID
        = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    protected static final String JONAS_EJB_JAR_2_4_PUBLIC_ID
        = "-//ObjectWeb//DTD JOnAS 2.4//EN";
    protected static final String JONAS_EJB_JAR_2_5_PUBLIC_ID
        = "-//ObjectWeb//DTD JOnAS 2.5//EN";
    protected static final String RMI_ORB = "RMI";
    protected static final String JEREMIE_ORB = "JEREMIE";
    protected static final String DAVID_ORB = "DAVID";
    protected static final String EJB_JAR_1_1_DTD = "ejb-jar_1_1.dtd";
    protected static final String EJB_JAR_2_0_DTD = "ejb-jar_2_0.dtd";
    protected static final String JONAS_EJB_JAR_2_4_DTD
        = "jonas-ejb-jar_2_4.dtd";
    protected static final String JONAS_EJB_JAR_2_5_DTD
        = "jonas-ejb-jar_2_5.dtd";
    protected static final String JONAS_DD = "jonas-ejb-jar.xml";
    protected static final String GENIC_CLASS =
    "org.objectweb.jonas_ejb.genic.GenIC";
    protected static final String OLD_GENIC_CLASS_1 =
        "org.objectweb.jonas_ejb.tools.GenWholeIC";
    protected static final String OLD_GENIC_CLASS_2 =
        "org.objectweb.jonas_ejb.tools.GenIC";
    private String descriptorName;
    private String jonasDescriptorName;
    private File outputdir;
    private boolean keepgenerated = false;
    private boolean nocompil = false;
    private boolean novalidation = false;
    private String javac;
    private String javacopts;
    private String rmicopts;
    private boolean secpropag = false;
    private boolean verbose = false;
    private String additionalargs;
    private File jonasroot;
    private boolean keepgeneric = false;
    private String suffix = ".jar";
    private String orb;
    private boolean nogenic = false;
    public void setKeepgenerated(boolean aBoolean) {
        keepgenerated = aBoolean;
    }
    public void setAdditionalargs(String aString) {
        additionalargs = aString;
    }
    public void setNocompil(boolean aBoolean) {
        nocompil = aBoolean;
    }
    public void setNovalidation(boolean aBoolean) {
        novalidation = aBoolean;
    }
    public void setJavac(String aString) {
        javac = aString;
    }
    public void setJavacopts(String aString) {
        javacopts = aString;
    }
    public void setRmicopts(String aString) {
        rmicopts = aString;
    }
    public void setSecpropag(boolean aBoolean) {
        secpropag = aBoolean;
    }
    public void setVerbose(boolean aBoolean) {
        verbose = aBoolean;
    }
    public void setJonasroot(File aFile) {
        jonasroot = aFile;
    }
    public void setKeepgeneric(boolean aBoolean) {
        keepgeneric = aBoolean;
    }
    public void setJarsuffix(String aString) {
        suffix = aString;
    }
    public void setOrb(String aString) {
        orb = aString;
    }
    public void setNogenic(boolean aBoolean) {
        nogenic = aBoolean;
    }
    public void processDescriptor(String aDescriptorName, SAXParser saxParser) {
        descriptorName = aDescriptorName;
        log("JOnAS Deployment Tool processing: " + descriptorName,
            Project.MSG_VERBOSE);
        super.processDescriptor(descriptorName, saxParser);
        if (outputdir != null) {
            log("Deleting temp output directory '" + outputdir + "'.", Project.MSG_VERBOSE);
            deleteAllFiles(outputdir);
        }
    }
    protected void writeJar(String baseName, File jarfile, Hashtable ejbFiles, String publicId)
    throws BuildException {
        File genericJarFile = super.getVendorOutputJarFile(baseName);
        super.writeJar(baseName, genericJarFile, ejbFiles, publicId);
        addGenICGeneratedFiles(genericJarFile, ejbFiles);
        super.writeJar(baseName, getVendorOutputJarFile(baseName), ejbFiles, publicId);
        if (!keepgeneric) {
            log("Deleting generic JAR " + genericJarFile.toString(), Project.MSG_VERBOSE);
            genericJarFile.delete();
        }
    }
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
    jonasDescriptorName = getJonasDescriptorName();
        File jonasDD = new File(getConfig().descriptorDir, jonasDescriptorName);
        if (jonasDD.exists()) {
            ejbFiles.put(META_DIR + JONAS_DD, jonasDD);
        } else {
            log("Unable to locate the JOnAS deployment descriptor. It was expected to be in: "
                + jonasDD.getPath() + ".", Project.MSG_WARN);
        }
    }
    protected File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName + suffix);
    }
    private String getJonasDescriptorName() {
        String jonasDN; 
        boolean jonasConvention = false; 
        String path;            
        String fileName;        
        String baseName;        
        String remainder;       
        int startOfFileName = descriptorName.lastIndexOf(File.separatorChar);
        if (startOfFileName != -1) {
            path = descriptorName.substring(0, startOfFileName + 1);
            fileName = descriptorName.substring(startOfFileName + 1);
        } else {
            path = "";
            fileName = descriptorName;
        }
        if (fileName.startsWith(EJB_DD)) {
            return path + JONAS_DD;
        }
        int endOfBaseName = descriptorName.indexOf(getConfig().baseNameTerminator, startOfFileName);
        if (endOfBaseName < 0) {
            endOfBaseName = descriptorName.lastIndexOf('.') - 1;
            if (endOfBaseName < 0) {
                endOfBaseName = descriptorName.length() - 1;
            }
            jonasConvention = true;
        }
        baseName = descriptorName.substring(startOfFileName + 1, endOfBaseName + 1);
        remainder = descriptorName.substring(endOfBaseName + 1);
        if (jonasConvention) {
            jonasDN = path + "jonas-" + baseName + ".xml";
        } else {
            jonasDN = path + baseName + "jonas-" + remainder;
        }
        log("Standard EJB descriptor name: " + descriptorName, Project.MSG_VERBOSE);
        log("JOnAS-specific descriptor name: " + jonasDN, Project.MSG_VERBOSE);
        return jonasDN;
    }
    protected String getJarBaseName(String descriptorFileName) {
        String baseName = null;
        if (getConfig().namingScheme.getValue().equals(EjbJar.NamingScheme.DESCRIPTOR)) {
            if (descriptorFileName.indexOf(getConfig().baseNameTerminator) == -1) {
                String aCanonicalDescriptor = descriptorFileName.replace('\\', '/');
                int lastSeparatorIndex = aCanonicalDescriptor.lastIndexOf('/');
                int endOfBaseName;
                if (lastSeparatorIndex != -1) {
                    endOfBaseName = descriptorFileName.indexOf(".xml", lastSeparatorIndex);
                } else {
                    endOfBaseName = descriptorFileName.indexOf(".xml");
                }
                if (endOfBaseName != -1) {
                    baseName = descriptorFileName.substring(0, endOfBaseName);
                }
            }
        }
        if (baseName == null) {
            baseName = super.getJarBaseName(descriptorFileName);
        }
        log("JAR base name: " + baseName, Project.MSG_VERBOSE);
        return baseName;
    }
    protected void registerKnownDTDs(DescriptorHandler handler) {
        handler.registerDTD(EJB_JAR_1_1_PUBLIC_ID,
                    jonasroot + File.separator + "xml" + File.separator + EJB_JAR_1_1_DTD);
        handler.registerDTD(EJB_JAR_2_0_PUBLIC_ID,
                    jonasroot + File.separator + "xml" + File.separator + EJB_JAR_2_0_DTD);
        handler.registerDTD(JONAS_EJB_JAR_2_4_PUBLIC_ID,
                    jonasroot + File.separator + "xml" + File.separator + JONAS_EJB_JAR_2_4_DTD);
        handler.registerDTD(JONAS_EJB_JAR_2_5_PUBLIC_ID,
                    jonasroot + File.separator + "xml" + File.separator + JONAS_EJB_JAR_2_5_DTD);
    }
    private void addGenICGeneratedFiles(
        File genericJarFile, Hashtable ejbFiles) {
        Java genicTask = null;    
        String genicClass = null; 
        if (nogenic) {
            return;
        }
        genicTask = new Java(getTask());
        genicTask.setTaskName("genic");
        genicTask.setFork(true);
        genicTask.createJvmarg().setValue("-Dinstall.root=" + jonasroot);
        String jonasConfigDir = jonasroot + File.separator + "config";
        File javaPolicyFile = new File(jonasConfigDir, "java.policy");
        if (javaPolicyFile.exists()) {
            genicTask.createJvmarg().setValue("-Djava.security.policy="
                              + javaPolicyFile.toString());
        }
        try {
            outputdir = createTempDir();
        } catch (IOException aIOException) {
            String msg = "Cannot create temp dir: " + aIOException.getMessage();
            throw new BuildException(msg, aIOException);
        }
        log("Using temporary output directory: " + outputdir, Project.MSG_VERBOSE);
        genicTask.createArg().setValue("-d");
        genicTask.createArg().setFile(outputdir);
        String key;
        File f;
        Enumeration keys = ejbFiles.keys();
        while (keys.hasMoreElements()) {
            key = (String) keys.nextElement();
            f = new File(outputdir + File.separator + key);
            f.getParentFile().mkdirs();
        }
        log("Worked around a bug of GenIC 2.5.", Project.MSG_VERBOSE);
        Path classpath = getCombinedClasspath();
        if (classpath == null) {
            classpath = new Path(getTask().getProject());
        }
        classpath.append(new Path(classpath.getProject(), jonasConfigDir));
        classpath.append(new Path(classpath.getProject(), outputdir.toString()));
        if (orb != null) {
            String orbJar = jonasroot + File.separator + "lib"
                + File.separator + orb + "_jonas.jar";
            classpath.append(new Path(classpath.getProject(), orbJar));
        }
        log("Using classpath: " + classpath.toString(), Project.MSG_VERBOSE);
        genicTask.setClasspath(classpath);
        genicClass = getGenicClassName(classpath);
        if (genicClass == null) {
            log("Cannot find GenIC class in classpath.", Project.MSG_ERR);
            throw new BuildException("GenIC class not found, please check the classpath.");
        } else {
            log("Using '" + genicClass + "' GenIC class." , Project.MSG_VERBOSE);
            genicTask.setClassname(genicClass);
        }
        if (keepgenerated) {
            genicTask.createArg().setValue("-keepgenerated");
        }
        if (nocompil) {
            genicTask.createArg().setValue("-nocompil");
        }
        if (novalidation) {
            genicTask.createArg().setValue("-novalidation");
        }
        if (javac != null) {
            genicTask.createArg().setValue("-javac");
            genicTask.createArg().setLine(javac);
        }
        if (javacopts != null && !javacopts.equals("")) {
            genicTask.createArg().setValue("-javacopts");
            genicTask.createArg().setLine(javacopts);
        }
        if (rmicopts != null && !rmicopts.equals("")) {
            genicTask.createArg().setValue("-rmicopts");
            genicTask.createArg().setLine(rmicopts);
        }
        if (secpropag) {
            genicTask.createArg().setValue("-secpropag");
        }
        if (verbose) {
            genicTask.createArg().setValue("-verbose");
        }
        if (additionalargs != null) {
            genicTask.createArg().setValue(additionalargs);
        }
        genicTask.createArg().setValue("-noaddinjar");
        genicTask.createArg().setValue(genericJarFile.getPath());
        log("Calling " + genicClass + " for " + getConfig().descriptorDir
            + File.separator + descriptorName + ".", Project.MSG_VERBOSE);
        if (genicTask.executeJava() != 0) {
            log("Deleting temp output directory '" + outputdir + "'.", Project.MSG_VERBOSE);
            deleteAllFiles(outputdir);
            if (!keepgeneric) {
                log("Deleting generic JAR " + genericJarFile.toString(),
                    Project.MSG_VERBOSE);
                genericJarFile.delete();
            }
            throw new BuildException("GenIC reported an error.");
        }
        addAllFiles(outputdir, "", ejbFiles);
    }
    String getGenicClassName(Path classpath) {
        log("Looking for GenIC class in classpath: "
            + classpath.toString(), Project.MSG_VERBOSE);
        AntClassLoader cl = null;
        try {
            cl = classpath.getProject().createClassLoader(classpath);
            try {
                cl.loadClass(JonasDeploymentTool.GENIC_CLASS);
                log("Found GenIC class '" + JonasDeploymentTool.GENIC_CLASS
                    + "' in classpath.", Project.MSG_VERBOSE);
                return JonasDeploymentTool.GENIC_CLASS;
            } catch (ClassNotFoundException cnf1) {
                log("GenIC class '" + JonasDeploymentTool.GENIC_CLASS
                    + "' not found in classpath.",
                    Project.MSG_VERBOSE);
            }
            try {
                cl.loadClass(JonasDeploymentTool.OLD_GENIC_CLASS_1);
                log("Found GenIC class '"
                    + JonasDeploymentTool.OLD_GENIC_CLASS_1
                    + "' in classpath.", Project.MSG_VERBOSE);
                return JonasDeploymentTool.OLD_GENIC_CLASS_1;
            } catch (ClassNotFoundException cnf2) {
                log("GenIC class '" + JonasDeploymentTool.OLD_GENIC_CLASS_1
                    + "' not found in classpath.",
                    Project.MSG_VERBOSE);
            }
            try {
                cl.loadClass(JonasDeploymentTool.OLD_GENIC_CLASS_2);
                log("Found GenIC class '"
                    + JonasDeploymentTool.OLD_GENIC_CLASS_2
                    + "' in classpath.", Project.MSG_VERBOSE);
                return JonasDeploymentTool.OLD_GENIC_CLASS_2;
            } catch (ClassNotFoundException cnf3) {
                log("GenIC class '" + JonasDeploymentTool.OLD_GENIC_CLASS_2
                    + "' not found in classpath.",
                    Project.MSG_VERBOSE);
            }
        } finally {
            if (cl != null) {
                cl.cleanup();
            }
        }
        return null;
    }
    protected void checkConfiguration(String descriptorFileName,
                      SAXParser saxParser) throws BuildException {
        if (jonasroot == null) {
            throw new BuildException("The jonasroot attribut is not set.");
        } else if (!jonasroot.isDirectory()) {
            throw new BuildException("The jonasroot attribut '" + jonasroot
                + "' is not a valid directory.");
        }
        if (orb != null && !orb.equals(RMI_ORB) && !orb.equals(JEREMIE_ORB)
            && !orb.equals(DAVID_ORB)) {
            throw new BuildException("The orb attribut '" + orb
                + "' is not valid (must be either "
                + RMI_ORB + ", " + JEREMIE_ORB + " or " + DAVID_ORB + ").");
        }
        if (additionalargs != null && additionalargs.equals("")) {
            throw new BuildException("Empty additionalargs attribut.");
        }
        if (javac != null && javac.equals("")) {
            throw new BuildException("Empty javac attribut.");
        }
    }
    private File createTempDir() throws IOException {
        File tmpDir = File.createTempFile("genic", null, null);
        tmpDir.delete();
        if (!tmpDir.mkdir()) {
            throw new IOException("Cannot create the temporary directory '" + tmpDir + "'.");
        }
        return tmpDir;
    }
    private void deleteAllFiles(File aFile) {
        if (aFile.isDirectory()) {
            File[] someFiles = aFile.listFiles();
            for (int i = 0; i < someFiles.length; i++) {
                deleteAllFiles(someFiles[i]);
            }
        }
        aFile.delete();
    }
    private void addAllFiles(File file, String rootDir, Hashtable hashtable) {
        if (!file.exists()) {
            throw new IllegalArgumentException();
        }
        String newRootDir;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (rootDir.length() > 0) {
                    newRootDir = rootDir + File.separator + files[i].getName();
                } else {
                    newRootDir = files[i].getName();
                }
                addAllFiles(files[i], newRootDir, hashtable);
            }
        } else {
            hashtable.put(rootDir, file);
        }
    }
}
