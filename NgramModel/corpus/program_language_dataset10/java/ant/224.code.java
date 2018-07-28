package org.apache.tools.ant.taskdefs;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.util.FileUtils;
public class LoadResource extends Task {
    private Resource src;
    private boolean failOnError = true;
    private boolean quiet = false;
    private String encoding = null;
    private String property = null;
    private final Vector filterChains = new Vector();
    public final void setEncoding(final String encoding) {
        this.encoding = encoding;
    }
    public final void setProperty(final String property) {
        this.property = property;
    }
    public final void setFailonerror(final boolean fail) {
        failOnError = fail;
    }
    public void setQuiet(final boolean quiet) {
        this.quiet = quiet;
        if (quiet) {
            this.failOnError = false;
        }
    }
    public final void execute()
        throws BuildException {
        if (src == null) {
            throw new BuildException("source resource not defined");
        }
        if (property == null) {
            throw new BuildException("output property not defined");
        }
        if (quiet && failOnError) {
            throw new BuildException("quiet and failonerror cannot both be "
                                     + "set to true");
        }
        if (!src.isExists()) {
            String message = src + " doesn't exist";
            if (failOnError) {
                throw new BuildException(message);
            } else {
                log(message, quiet ? Project.MSG_WARN : Project.MSG_ERR);
                return;
            }
        }
        InputStream is = null;
        BufferedInputStream bis = null;
        Reader instream = null;
        log("loading " + src + " into property " + property,
            Project.MSG_VERBOSE);
        try {
            final long len = src.getSize();
            log("resource size = "
                + (len != Resource.UNKNOWN_SIZE ? String.valueOf(len)
                   : "unknown"), Project.MSG_DEBUG);
            final int size = (int) len;
            is = src.getInputStream();
            bis = new BufferedInputStream(is);
            if (encoding == null) {
                instream = new InputStreamReader(bis);
            } else {
                instream = new InputStreamReader(bis, encoding);
            }
            String text = "";
            if (size != 0) {
                ChainReaderHelper crh = new ChainReaderHelper();
                if (len != Resource.UNKNOWN_SIZE) {
                    crh.setBufferSize(size);
                }
                crh.setPrimaryReader(instream);
                crh.setFilterChains(filterChains);
                crh.setProject(getProject());
                instream = crh.getAssembledReader();
                text = crh.readFully(instream);
            } else {
                log("Do not set property " + property + " as its length is 0.");
            }
            if (text != null) {
                if (text.length() > 0) {
                    getProject().setNewProperty(property, text);
                    log("loaded " + text.length() + " characters",
                        Project.MSG_VERBOSE);
                    log(property + " := " + text, Project.MSG_DEBUG);
                }
            }
        } catch (final IOException ioe) {
            final String message = "Unable to load resource: "
                + ioe.toString();
            if (failOnError) {
                throw new BuildException(message, ioe, getLocation());
            } else {
                log(message, quiet ? Project.MSG_VERBOSE : Project.MSG_ERR);
            }
        } catch (final BuildException be) {
            if (failOnError) {
                throw be;
            } else {
                log(be.getMessage(),
                    quiet ? Project.MSG_VERBOSE : Project.MSG_ERR);
            }
        } finally {
            FileUtils.close(is);
        }
    }
    public final void addFilterChain(FilterChain filter) {
        filterChains.addElement(filter);
    }
    public void addConfigured(ResourceCollection a) {
        if (a.size() != 1) {
            throw new BuildException("only single argument resource collections"
                                     + " are supported");
        }
        src = (Resource) a.iterator().next();
    }
}