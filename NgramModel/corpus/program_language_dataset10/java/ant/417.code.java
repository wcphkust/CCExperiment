package org.apache.tools.ant.taskdefs.optional.ejb;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import javax.xml.parsers.SAXParser;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.xml.sax.SAXException;
public class IPlanetDeploymentTool extends GenericDeploymentTool {
    private File    iashome;
    private String  jarSuffix     = ".jar";
    private boolean keepgenerated = false;
    private boolean debug         = false;
    private String  descriptorName;
    private String  iasDescriptorName;
    private String  displayName;
    private static final String IAS_DD = "ias-ejb-jar.xml";
    public void setIashome(File iashome) {
        this.iashome = iashome;
    }
    public void setKeepgenerated(boolean keepgenerated) {
        this.keepgenerated = keepgenerated;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public void setSuffix(String jarSuffix) {
        this.jarSuffix = jarSuffix;
    }
    public void setGenericJarSuffix(String inString) {
        log("Since a generic JAR file is not created during processing, the "
                + "iPlanet Deployment Tool does not support the "
                + "\"genericjarsuffix\" attribute.  It will be ignored.",
            Project.MSG_WARN);
    }
    public void processDescriptor(String descriptorName, SAXParser saxParser) {
        this.descriptorName = descriptorName;
        this.iasDescriptorName = null;
        log("iPlanet Deployment Tool processing: " + descriptorName + " (and "
                + getIasDescriptorName() + ")", Project.MSG_VERBOSE);
        super.processDescriptor(descriptorName, saxParser);
    }
    protected void checkConfiguration(String descriptorFileName,
                                    SAXParser saxParser) throws BuildException {
        int startOfName = descriptorFileName.lastIndexOf(File.separatorChar) + 1;
        String stdXml = descriptorFileName.substring(startOfName);
        if (stdXml.equals(EJB_DD) && (getConfig().baseJarName == null)) {
            String msg = "No name specified for the completed JAR file.  The EJB"
                            + " descriptor should be prepended with the JAR "
                            + "name or it should be specified using the "
                            + "attribute \"basejarname\" in the \"ejbjar\" task.";
            throw new BuildException(msg, getLocation());
        }
        File iasDescriptor = new File(getConfig().descriptorDir,
                                        getIasDescriptorName());
        if ((!iasDescriptor.exists()) || (!iasDescriptor.isFile())) {
            String msg = "The iAS-specific EJB descriptor ("
                            + iasDescriptor + ") was not found.";
            throw new BuildException(msg, getLocation());
        }
        if ((iashome != null) && (!iashome.isDirectory())) {
            String msg = "If \"iashome\" is specified, it must be a valid "
                            + "directory (it was set to " + iashome + ").";
            throw new BuildException(msg, getLocation());
        }
    }
    protected Hashtable parseEjbFiles(String descriptorFileName,
                         SAXParser saxParser) throws IOException, SAXException {
        Hashtable files;
        IPlanetEjbc ejbc = new IPlanetEjbc(
                                    new File(getConfig().descriptorDir,
                                                descriptorFileName),
                                    new File(getConfig().descriptorDir,
                                                getIasDescriptorName()),
                                    getConfig().srcDir,
                                    getCombinedClasspath().toString(),
                                    saxParser);
        ejbc.setRetainSource(keepgenerated);
        ejbc.setDebugOutput(debug);
        if (iashome != null) {
            ejbc.setIasHomeDir(iashome);
        }
        if (getConfig().dtdLocations != null) {
            for (Iterator i = getConfig().dtdLocations.iterator();
                 i.hasNext(); ) {
                EjbJar.DTDLocation dtdLocation =
                    (EjbJar.DTDLocation) i.next();
                ejbc.registerDTD(dtdLocation.getPublicId(),
                                 dtdLocation.getLocation());
            }
        }
        try {
            ejbc.execute();
        } catch (IPlanetEjbc.EjbcException e) {
            throw new BuildException("An error has occurred while trying to "
                        + "execute the iAS ejbc utility", e, getLocation());
        }
        displayName    = ejbc.getDisplayName();
        files          = ejbc.getEjbFiles();
        String[] cmpDescriptors = ejbc.getCmpDescriptors();
        if (cmpDescriptors.length > 0) {
            File baseDir = getConfig().descriptorDir;
            int endOfPath = descriptorFileName.lastIndexOf(File.separator);
            String relativePath = descriptorFileName.substring(0, endOfPath + 1);
            for (int i = 0; i < cmpDescriptors.length; i++) {
                int endOfCmp = cmpDescriptors[i].lastIndexOf('/');
                String cmpDescriptor = cmpDescriptors[i].substring(endOfCmp + 1);
                File   cmpFile = new File(baseDir, relativePath + cmpDescriptor);
                if (!cmpFile.exists()) {
                    throw new BuildException("The CMP descriptor file ("
                            + cmpFile + ") could not be found.", getLocation());
                }
                files.put(cmpDescriptors[i], cmpFile);
            }
        }
        return files;
    }
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
        ejbFiles.put(META_DIR + IAS_DD, new File(getConfig().descriptorDir,
                     getIasDescriptorName()));
    }
    File getVendorOutputJarFile(String baseName) {
        File jarFile = new File(getDestDir(), baseName + jarSuffix);
        log("JAR file name: " + jarFile.toString(), Project.MSG_VERBOSE);
        return jarFile;
    }
    protected String getPublicId() {
        return null;
    }
    private String getIasDescriptorName() {
        if (iasDescriptorName != null) {
            return iasDescriptorName;
        }
        String path = "";   
        String basename;    
        String remainder;   
        int startOfFileName = descriptorName.lastIndexOf(File.separatorChar);
        if (startOfFileName != -1) {
            path = descriptorName.substring(0, startOfFileName + 1);
        }
        if (descriptorName.substring(startOfFileName + 1).equals(EJB_DD)) {
            basename = "";
            remainder = EJB_DD;
        } else {
            int endOfBaseName = descriptorName.indexOf(
                                                getConfig().baseNameTerminator,
                                                startOfFileName);
            if (endOfBaseName < 0) {
                endOfBaseName = descriptorName.lastIndexOf('.') - 1;
                if (endOfBaseName < 0) {
                    endOfBaseName = descriptorName.length() - 1;
                }
            }
            basename = descriptorName.substring(startOfFileName + 1,
                                                endOfBaseName + 1);
            remainder = descriptorName.substring(endOfBaseName + 1);
        }
        iasDescriptorName = path + basename + "ias-" + remainder;
        return iasDescriptorName;
    }
}