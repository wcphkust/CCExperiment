package org.apache.tools.ant.taskdefs.condition;
import java.util.Locale;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
public class Http extends ProjectComponent implements Condition {
    private static final int ERROR_BEGINS = 400;
    private static final String DEFAULT_REQUEST_METHOD = "GET";
    private String spec = null;
    private String requestMethod = DEFAULT_REQUEST_METHOD;
    public void setUrl(String url) {
        spec = url;
    }
    private int errorsBeginAt = ERROR_BEGINS;
    public void setErrorsBeginAt(int errorsBeginAt) {
        this.errorsBeginAt = errorsBeginAt;
    }
    public void setRequestMethod(String method) {
        this.requestMethod = method == null ? DEFAULT_REQUEST_METHOD
            : method.toUpperCase(Locale.ENGLISH);
    }
    public boolean eval() throws BuildException {
        if (spec == null) {
            throw new BuildException("No url specified in http condition");
        }
        log("Checking for " + spec, Project.MSG_VERBOSE);
        try {
            URL url = new URL(spec);
            try {
                URLConnection conn = url.openConnection();
                if (conn instanceof HttpURLConnection) {
                    HttpURLConnection http = (HttpURLConnection) conn;
                    http.setRequestMethod(requestMethod);
                    int code = http.getResponseCode();
                    log("Result code for " + spec + " was " + code,
                        Project.MSG_VERBOSE);
                    if (code > 0 && code < errorsBeginAt) {
                        return true;
                    }
                    return false;
                }
            } catch (java.net.ProtocolException pe) {
                throw new BuildException("Invalid HTTP protocol: "
                                         + requestMethod, pe);
            } catch (java.io.IOException e) {
                return false;
            }
        } catch (MalformedURLException e) {
            throw new BuildException("Badly formed URL: " + spec, e);
        }
        return true;
    }
}
