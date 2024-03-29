package org.apache.tools.ant.taskdefs.optional.sound;
import java.io.File;
import java.util.Random;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
public class SoundTask extends Task {
    private BuildAlert success = null;
    private BuildAlert fail = null;
    public BuildAlert createSuccess() {
        success = new BuildAlert();
        return success;
    }
    public BuildAlert createFail() {
        fail = new BuildAlert();
        return fail;
     }
    public SoundTask() {
    }
    public void init() {
    }
    public void execute() {
        AntSoundPlayer soundPlayer = new AntSoundPlayer();
        if (success == null) {
            log("No nested success element found.", Project.MSG_WARN);
        } else {
            soundPlayer.addBuildSuccessfulSound(success.getSource(),
              success.getLoops(), success.getDuration());
        }
        if (fail == null) {
            log("No nested failure element found.", Project.MSG_WARN);
        } else {
            soundPlayer.addBuildFailedSound(fail.getSource(),
              fail.getLoops(), fail.getDuration());
        }
        getProject().addBuildListener(soundPlayer);
    }
    public class BuildAlert {
        private File source = null;
        private int loops = 0;
        private Long duration = null;
        public void setDuration(Long duration) {
            this.duration = duration;
        }
        public void setSource(File source) {
            this.source = source;
        }
        public void setLoops(int loops) {
            this.loops = loops;
        }
        public File getSource() {
            File nofile = null;
            if (source.exists()) {
                if (source.isDirectory()) {
                    String[] entries = source.list();
                    Vector files = new Vector();
                    for (int i = 0; i < entries.length; i++) {
                        File f = new File(source, entries[i]);
                        if (f.isFile()) {
                            files.addElement(f);
                        }
                    }
                    if (files.size() < 1) {
                        throw new BuildException("No files found in directory " + source);
                    }
                    int numfiles = files.size();
                    Random rn = new Random();
                    int x = rn.nextInt(numfiles);
                    this.source = (File) files.elementAt(x);
                }
            } else {
                log(source + ": invalid path.", Project.MSG_WARN);
                this.source = nofile;
            }
            return this.source;
        }
        public int getLoops() {
            return this.loops;
        }
        public Long getDuration() {
            return this.duration;
        }
    }
}
