package org.apache.tools.ant.taskdefs;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
public class KeySubst extends Task {
    private File source = null;
    private File dest = null;
    private String sep = "*";
    private Hashtable replacements = new Hashtable();
    public void execute() throws BuildException {
        log("!! KeySubst is deprecated. Use Filter + Copy instead. !!");
        log("Performing Substitutions");
        if (source == null || dest == null) {
            log("Source and destinations must not be null");
            return;
        }
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new FileReader(source));
            dest.delete();
            bw = new BufferedWriter(new FileWriter(dest));
            String line = null;
            String newline = null;
            line = br.readLine();
            while (line != null) {
                if (line.length() == 0) {
                    bw.newLine();
                } else {
                    newline = KeySubst.replace(line, replacements);
                    bw.write(newline);
                    bw.newLine();
                }
                line = br.readLine();
            }
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            FileUtils.close(bw);
            FileUtils.close(br);
        }
    }
    public void setSrc(File s) {
        this.source = s;
    }
    public void setDest(File dest) {
        this.dest = dest;
    }
    public void setSep(String sep) {
        this.sep = sep;
    }
    public void setKeys(String keys) {
        if (keys != null && keys.length() > 0) {
            StringTokenizer tok =
            new StringTokenizer(keys, this.sep, false);
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken().trim();
                StringTokenizer itok =
                new StringTokenizer(token, "=", false);
                String name = itok.nextToken();
                String value = itok.nextToken();
                replacements.put(name, value);
            }
        }
    }
    public static void main(String[] args) {
        try {
            Hashtable hash = new Hashtable();
            hash.put("VERSION", "1.0.3");
            hash.put("b", "ffff");
            System.out.println(KeySubst.replace("$f ${VERSION} f ${b} jj $",
                                                hash));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String replace(String origString, Hashtable keys)
        throws BuildException {
        StringBuffer finalString = new StringBuffer();
        int index = 0;
        int i = 0;
        String key = null;
        while ((index = origString.indexOf("${", i)) > -1) {
            key = origString.substring(index + 2, origString.indexOf("}",
                                       index + 3));
            finalString.append (origString.substring(i, index));
            if (keys.containsKey(key)) {
                finalString.append (keys.get(key));
            } else {
                finalString.append ("${");
                finalString.append (key);
                finalString.append ("}");
            }
            i = index + 3 + key.length();
        }
        finalString.append (origString.substring(i));
        return finalString.toString();
    }
}
