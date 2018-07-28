package org.apache.tools.ant.taskdefs.optional.ejb;
import java.io.File;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
public class WeblogicTOPLinkDeploymentTool extends WeblogicDeploymentTool {
    private static final String TL_DTD_LOC
        = "http://www.objectpeople.com/tlwl/dtd/toplink-cmp_2_5_1.dtd";
    private String toplinkDescriptor;
    private String toplinkDTD;
    public void setToplinkdescriptor(String inString) {
        this.toplinkDescriptor = inString;
    }
    public void setToplinkdtd(String inString) {
        this.toplinkDTD = inString;
    }
    protected DescriptorHandler getDescriptorHandler(File srcDir) {
        DescriptorHandler handler = super.getDescriptorHandler(srcDir);
        if (toplinkDTD != null) {
            handler.registerDTD("-//The Object People, Inc.//"
                + "DTD TOPLink for WebLogic CMP 2.5.1//EN", toplinkDTD);
        } else {
            handler.registerDTD("-//The Object People, Inc.//"
                + "DTD TOPLink for WebLogic CMP 2.5.1//EN", TL_DTD_LOC);
        }
        return handler;
    }
    protected void addVendorFiles(Hashtable ejbFiles, String ddPrefix) {
        super.addVendorFiles(ejbFiles, ddPrefix);
        File toplinkDD = new File(getConfig().descriptorDir, ddPrefix + toplinkDescriptor);
        if (toplinkDD.exists()) {
            ejbFiles.put(META_DIR + toplinkDescriptor,
                         toplinkDD);
        } else {
            log("Unable to locate toplink deployment descriptor. "
                + "It was expected to be in "
                + toplinkDD.getPath(), Project.MSG_WARN);
        }
    }
    public void validateConfigured() throws BuildException {
        super.validateConfigured();
        if (toplinkDescriptor == null) {
            throw new BuildException("The toplinkdescriptor attribute must "
                + "be specified");
        }
    }
}