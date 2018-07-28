package org.apache.tools.ant.taskdefs.optional.ejb;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
public class BorlandDeploymentTool extends GenericDeploymentTool
                                   implements ExecuteStreamHandler {
    public static final String PUBLICID_BORLAND_EJB
    = "-//Inprise Corporation//DTD Enterprise JavaBeans 1.1//EN";
    protected static final String DEFAULT_BAS45_EJB11_DTD_LOCATION
    = "/com/inprise/j2ee/xml/dtds/ejb-jar.dtd";
    protected static final String DEFAULT_BAS_DTD_LOCATION
    = "/com/inprise/j2ee/xml/dtds/ejb-inprise.dtd";
    protected static final String BAS_DD = "ejb-inprise.xml";
    protected static final String BES_DD = "ejb-borland.xml";
    protected static final String JAVA2IIOP = "java2iiop";
    protected static final String VERIFY = "com.inprise.ejb.util.Verify";
    private String jarSuffix = "-ejb.jar";
    private String borlandDTD;
    private boolean java2iiopdebug = false;
    private String java2iioparams = null;
    private boolean generateclient = false;
    static final int    BES       = 5;
    static final int    BAS       = 4;
    private int version = BAS;
    private boolean verify     = true;
    private String  verifyArgs = "";
    private Hashtable genfiles = new Hashtable();
    public void setDebug(boolean debug) {
        this.java2iiopdebug = debug;
    }
    public void setVerify(boolean verify) {
        this.verify = verify;
    }
    public void setSuffix(String inString) {
        this.jarSuffix = inString;
    }
    public void setVerifyArgs(String args) {
        this.verifyArgs = args;
    }
    public void setBASdtd(String inString) {
        this.borlandDTD = inString;
    }
    public void setGenerateclient(boolean b) {
        this.generateclient = b;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public void setJava2iiopParams(String params) {
        this.java2iioparams = params;
    }
    protected DescriptorHandler getBorlandDescriptorHandler(final File srcDir) {
        DescriptorHandler handler =
            new DescriptorHandler(getTask(), srcDir) {
                    protected void processElement() {
                        if (currentElement.equals("type-storage")) {
                            String fileNameWithMETA = currentText;
                            String fileName
                                = fileNameWithMETA.substring(META_DIR.length(),
                                    fileNameWithMETA.length());
                            File descriptorFile = new File(srcDir, fileName);
                            ejbFiles.put(fileNameWithMETA, descriptorFile);
                        }
                    }
                };
        handler.registerDTD(PUBLICID_BORLAND_EJB,
                            borlandDTD == null ? DEFAULT_BAS_DTD_LOCATION : borlandDTD);
        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            EjbJar.DTDLocation dtdLocation = (EjbJar.DTDLocation) i.next();
            handler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return handler;
    }
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
        if (!(version == BES || version == BAS)) {
            throw new BuildException("version " + version + " is not supported");
        }
        String dd = (version == BES ? BES_DD : BAS_DD);
        log("vendor file : " + ddPrefix + dd, Project.MSG_DEBUG);
        File borlandDD = new File(getConfig().descriptorDir, ddPrefix + dd);
        if (borlandDD.exists()) {
            log("Borland specific file found " + borlandDD,  Project.MSG_VERBOSE);
            ejbFiles.put(META_DIR + dd ,  borlandDD);
        } else {
            log("Unable to locate borland deployment descriptor. "
                + "It was expected to be in "
                + borlandDD.getPath(), Project.MSG_WARN);
            return;
        }
    }
    File getVendorOutputJarFile(String baseName) {
        return new File(getDestDir(), baseName +  jarSuffix);
    }
    private void verifyBorlandJar(File sourceJar) {
        if (version == BAS) {
            verifyBorlandJarV4(sourceJar);
            return;
        }
        if (version == BES) {
            verifyBorlandJarV5(sourceJar);
            return;
        }
        log("verify jar skipped because the version is invalid ["
            + version + "]", Project.MSG_WARN);
    }
    private void verifyBorlandJarV5(File sourceJar) {
        log("verify BES " + sourceJar, Project.MSG_INFO);
        try {
            ExecTask execTask = null;
            execTask = new ExecTask(getTask());
            execTask.setDir(new File("."));
            execTask.setExecutable("iastool");
            if (getCombinedClasspath() != null)  {
                execTask.createArg().setValue("-VBJclasspath");
                execTask.createArg().setValue(getCombinedClasspath().toString());
            }
            if (java2iiopdebug) {
                execTask.createArg().setValue("-debug");
            }
            execTask.createArg().setValue("-verify");
            execTask.createArg().setValue("-src");
            execTask.createArg().setValue(sourceJar.getPath());
            log("Calling iastool", Project.MSG_VERBOSE);
            execTask.execute();
        } catch (Exception e) {
            String msg = "Exception while calling generateclient Details: "
                + e.toString();
            throw new BuildException(msg, e);
        }
    }
    private void verifyBorlandJarV4(File sourceJar) {
        org.apache.tools.ant.taskdefs.Java javaTask = null;
        log("verify BAS " + sourceJar, Project.MSG_INFO);
        try  {
            String args = verifyArgs;
            args += " " + sourceJar.getPath();
            javaTask = new Java(getTask());
            javaTask.setTaskName("verify");
            javaTask.setClassname(VERIFY);
            Commandline.Argument arguments = javaTask.createArg();
            arguments.setLine(args);
            Path classpath = getCombinedClasspath();
            if (classpath != null)  {
                javaTask.setClasspath(classpath);
                javaTask.setFork(true);
            }
            log("Calling " + VERIFY + " for " + sourceJar.toString(),
                Project.MSG_VERBOSE);
            javaTask.execute();
        } catch (Exception e) {
            String msg = "Exception while calling " + VERIFY + " Details: "
                + e.toString();
            throw new BuildException(msg, e);
        }
    }
    private void generateClient(File sourceJar) {
        getTask().getProject().addTaskDefinition("internal_bas_generateclient",
            org.apache.tools.ant.taskdefs.optional.ejb.BorlandGenerateClient.class);
        org.apache.tools.ant.taskdefs.optional.ejb.BorlandGenerateClient gentask = null;
        log("generate client for " + sourceJar, Project.MSG_INFO);
        try {
            Project project = getTask().getProject();
            gentask
                = (BorlandGenerateClient) project.createTask("internal_bas_generateclient");
            gentask.setEjbjar(sourceJar);
            gentask.setDebug(java2iiopdebug);
            Path classpath = getCombinedClasspath();
            if (classpath != null) {
                gentask.setClasspath(classpath);
            }
            gentask.setVersion(version);
            gentask.setTaskName("generate client");
            gentask.execute();
        } catch (Exception e) {
            String msg = "Exception while calling " + VERIFY + " Details: "
                + e.toString();
            throw new BuildException(msg, e);
        }
    }
    private void buildBorlandStubs(Iterator ithomes) {
        Execute execTask = null;
        execTask = new Execute(this);
        Project project = getTask().getProject();
        execTask.setAntRun(project);
        execTask.setWorkingDirectory(project.getBaseDir());
        Commandline commandline = new Commandline();
        commandline.setExecutable(JAVA2IIOP);
        if (java2iiopdebug) {
            commandline.createArgument().setValue("-VBJdebug");
        }
        commandline.createArgument().setValue("-VBJclasspath");
        commandline.createArgument().setPath(getCombinedClasspath());
        commandline.createArgument().setValue("-list_files");
        commandline.createArgument().setValue("-no_tie");
        if (java2iioparams != null) {
            log("additional  " + java2iioparams + " to java2iiop ", 0);
            commandline.createArgument().setLine(java2iioparams);
        }
        commandline.createArgument().setValue("-root_dir");
        commandline.createArgument().setValue(getConfig().srcDir.getAbsolutePath());
        commandline.createArgument().setValue("-compile");
        while (ithomes.hasNext()) {
            commandline.createArgument().setValue(ithomes.next().toString());
        }
        try {
            log("Calling java2iiop", Project.MSG_VERBOSE);
            log(commandline.describeCommand(), Project.MSG_DEBUG);
            execTask.setCommandline(commandline.getCommandline());
            int result = execTask.execute();
            if (Execute.isFailure(result)) {
                String msg = "Failed executing java2iiop (ret code is "
                    + result + ")";
                throw new BuildException(msg, getTask().getLocation());
            }
        } catch (java.io.IOException e) {
            log("java2iiop exception :" + e.getMessage(), Project.MSG_ERR);
            throw new BuildException(e, getTask().getLocation());
        }
    }
    protected void writeJar(String baseName, File jarFile, Hashtable files, String publicId)
        throws BuildException {
        Vector homes = new Vector();
        Iterator it = files.keySet().iterator();
        while (it.hasNext()) {
            String clazz = (String) it.next();
            if (clazz.endsWith("Home.class")) {
                String home = toClass(clazz);
                homes.add(home);
                log(" Home " + home, Project.MSG_VERBOSE);
            }
        }
        buildBorlandStubs(homes.iterator());
        files.putAll(genfiles);
        super.writeJar(baseName, jarFile, files, publicId);
        if (verify) {
            verifyBorlandJar(jarFile);
        }
        if (generateclient) {
            generateClient(jarFile);
        }
        genfiles.clear();
    }
    private String toClass(String filename) {
        String classname = filename.substring(0, filename.lastIndexOf(".class"));
        classname = classname.replace('\\', '.');
        return classname;
    }
    private  String toClassFile(String filename) {
        String classfile = filename.substring(0, filename.lastIndexOf(".java"));
        classfile = classfile + ".class";
        return classfile;
    }
    public void start() throws IOException  { }
    public void stop()  {  }
    public void setProcessInputStream(OutputStream param1) throws IOException   { }
    public void setProcessOutputStream(InputStream is) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String javafile;
            while ((javafile = reader.readLine()) != null) {
                if (javafile.endsWith(".java")) {
                    String classfile = toClassFile(javafile);
                    String key = classfile.substring(
                        getConfig().srcDir.getAbsolutePath().length() + 1);
                    genfiles.put(key, new File(classfile));
                }
            }
            reader.close();
        } catch (Exception e) {
            String msg = "Exception while parsing  java2iiop output. Details: " + e.toString();
            throw new BuildException(msg, e);
        }
    }
    public void setProcessErrorStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String s = reader.readLine();
        if (s != null) {
            log("[java2iiop] " + s, Project.MSG_ERR);
        }
    }
}