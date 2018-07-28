package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.ZipOutputStream;
public class War extends Jar {
    private File deploymentDescriptor;
    private boolean needxmlfile = true;
    private File addedWebXmlFile;
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final String XML_DESCRIPTOR_PATH = "WEB-INF/web.xml";
    public War() {
        super();
        archiveType = "war";
        emptyBehavior = "create";
    }
    public void setWarfile(File warFile) {
        setDestFile(warFile);
    }
    public void setWebxml(File descr) {
        deploymentDescriptor = descr;
        if (!deploymentDescriptor.exists()) {
            throw new BuildException("Deployment descriptor: "
                                     + deploymentDescriptor
                                     + " does not exist.");
        }
        ZipFileSet fs = new ZipFileSet();
        fs.setFile(deploymentDescriptor);
        fs.setFullpath(XML_DESCRIPTOR_PATH);
        super.addFileset(fs);
    }
    public void setNeedxmlfile(boolean needxmlfile) {
        this.needxmlfile = needxmlfile;
    }
    public void addLib(ZipFileSet fs) {
        fs.setPrefix("WEB-INF/lib/");
        super.addFileset(fs);
    }
    public void addClasses(ZipFileSet fs) {
        fs.setPrefix("WEB-INF/classes/");
        super.addFileset(fs);
    }
    public void addWebinf(ZipFileSet fs) {
        fs.setPrefix("WEB-INF/");
        super.addFileset(fs);
    }
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
        super.initZipOutputStream(zOut);
    }
    protected void zipFile(File file, ZipOutputStream zOut, String vPath,
                           int mode)
        throws IOException {
        boolean addFile = true;
        if (XML_DESCRIPTOR_PATH.equalsIgnoreCase(vPath)) {
            if (addedWebXmlFile != null) {
                addFile = false;
                if (!FILE_UTILS.fileNameEquals(addedWebXmlFile, file)) {
                    logWhenWriting("Warning: selected " + archiveType
                                   + " files include a second "
                                   + XML_DESCRIPTOR_PATH
                                   + " which will be ignored.\n"
                                   + "The duplicate entry is at " + file + '\n'
                                   + "The file that will be used is "
                                   + addedWebXmlFile,
                                   Project.MSG_WARN);
                }
            } else {
                addedWebXmlFile = file;
                addFile = true;
                deploymentDescriptor = file;
            }
        }
        if (addFile) {
            super.zipFile(file, zOut, vPath, mode);
        }
    }
    protected void cleanUp() {
        if (addedWebXmlFile == null
            && deploymentDescriptor == null
            && needxmlfile
            && !isInUpdateMode()
            && hasUpdatedFile()) {
            throw new BuildException("No WEB-INF/web.xml file was added.\n"
                    + "If this is your intent, set needxmlfile='false' ");
        }
        addedWebXmlFile = null;
        super.cleanUp();
    }
}