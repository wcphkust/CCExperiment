package org.apache.tools.ant.taskdefs.optional.perforce;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
public class P4Change extends P4Base {
    protected String emptyChangeList = null;
    protected String description = "AutoSubmit By Ant";
    public void execute() throws BuildException {
        if (emptyChangeList == null) {
            emptyChangeList = getEmptyChangeList();
        }
        final Project myProj = getProject();
        P4Handler handler = new P4HandlerAdapter() {
            public void process(String line) {
                if (util.match("/Change/", line)) {
                    line = util.substitute("s/[^0-9]//g", line);
                    int changenumber = Integer.parseInt(line);
                    log("Change Number is " + changenumber, Project.MSG_INFO);
                    myProj.setProperty("p4.change", "" + changenumber);
                } else if (util.match("/error/", line)) {
                    throw new BuildException("Perforce Error, check client settings and/or server");
                }
            }
        };
        handler.setOutput(emptyChangeList);
        execP4Command("change -i", handler);
    }
    public String getEmptyChangeList() throws BuildException {
        final StringBuffer stringbuf = new StringBuffer();
        execP4Command("change -o", new P4HandlerAdapter() {
            public void process(String line) {
                if (!util.match("/^#/", line)) {
                    if (util.match("/error/", line)) {
                        log("Client Error", Project.MSG_VERBOSE);
                        throw new BuildException("Perforce Error, "
                        + "check client settings and/or server");
                    } else if (util.match("/<enter description here>/", line)) {
                        description = backslash(description);
                        line = util.substitute("s/<enter description here>/"
                            + description + "/", line);
                    } else if (util.match("/\\/\\//", line)) {
                        return;
                    }
                    stringbuf.append(line);
                    stringbuf.append("\n");
                }
            }
        });
        return stringbuf.toString();
    }
    public static final String backslash(String value) {
        final StringBuffer buf = new StringBuffer(value.length());
        final int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '/') {
                buf.append('\\');
            }
            buf.append(c);
        }
        return buf.toString();
    }
    public void setDescription(String desc) {
        this.description = desc;
    }
} 