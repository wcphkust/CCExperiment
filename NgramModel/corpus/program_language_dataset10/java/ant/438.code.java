package org.apache.tools.ant.taskdefs.optional.extension;
import org.apache.tools.ant.types.FileSet;
public class LibFileSet
    extends FileSet {
    private boolean includeURL;
    private boolean includeImpl;
    private String urlBase;
    public void setIncludeUrl(boolean includeURL) {
        this.includeURL = includeURL;
    }
    public void setIncludeImpl(boolean includeImpl) {
        this.includeImpl = includeImpl;
    }
    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }
    boolean isIncludeURL() {
        return includeURL;
    }
    boolean isIncludeImpl() {
        return includeImpl;
    }
    String getUrlBase() {
        return urlBase;
    }
}
