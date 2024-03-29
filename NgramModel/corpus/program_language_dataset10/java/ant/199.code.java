package org.apache.tools.ant.taskdefs;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.ant.util.SourceFileScanner;
public class ExecuteOn extends ExecTask {
    protected Vector filesets = new Vector(); 
    private Union resources = null;
    private boolean relative = false;
    private boolean parallel = false;
    private boolean forwardSlash = false;
    protected String type = FileDirBoth.FILE;
    protected Commandline.Marker srcFilePos = null;
    private boolean skipEmpty = false;
    protected Commandline.Marker targetFilePos = null;
    protected Mapper mapperElement = null;
    protected FileNameMapper mapper = null;
    protected File destDir = null;
    private int maxParallel = -1;
    private boolean addSourceFile = true;
    private boolean verbose = false;
    private boolean ignoreMissing = true;
    private boolean force = false;
    protected boolean srcIsFirst = true;
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }
    public void addDirset(DirSet set) {
        filesets.addElement(set);
    }
    public void addFilelist(FileList list) {
        add(list);
    }
    public void add(ResourceCollection rc) {
        if (resources == null) {
            resources = new Union();
        }
        resources.add(rc);
    }
    public void setRelative(boolean relative) {
        this.relative = relative;
    }
    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }
    public void setType(FileDirBoth type) {
        this.type = type.getValue();
    }
    public void setSkipEmptyFilesets(boolean skip) {
        skipEmpty = skip;
    }
    public void setDest(File destDir) {
        this.destDir = destDir;
    }
    public void setForwardslash(boolean forwardSlash) {
        this.forwardSlash = forwardSlash;
    }
    public void setMaxParallel(int max) {
        maxParallel = max;
    }
    public void setAddsourcefile(boolean b) {
        addSourceFile = b;
    }
    public void setVerbose(boolean b) {
        verbose = b;
    }
    public void setIgnoremissing(boolean b) {
        ignoreMissing = b;
    }
    public void setForce(boolean b) {
        force = b;
    }
    public Commandline.Marker createSrcfile() {
        if (srcFilePos != null) {
            throw new BuildException(getTaskType() + " doesn\'t support multiple "
                                     + "srcfile elements.", getLocation());
        }
        srcFilePos = cmdl.createMarker();
        return srcFilePos;
    }
    public Commandline.Marker createTargetfile() {
        if (targetFilePos != null) {
            throw new BuildException(getTaskType() + " doesn\'t support multiple "
                                     + "targetfile elements.", getLocation());
        }
        targetFilePos = cmdl.createMarker();
        srcIsFirst = (srcFilePos != null);
        return targetFilePos;
    }
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }
    protected void checkConfiguration() {
        if ("execon".equals(getTaskName())) {
            log("!! execon is deprecated. Use apply instead. !!");
        }
        super.checkConfiguration();
        if (filesets.size() == 0 && resources == null) {
            throw new BuildException("no resources specified",
                                     getLocation());
        }
        if (targetFilePos != null && mapperElement == null) {
            throw new BuildException("targetfile specified without mapper",
                                     getLocation());
        }
        if (destDir != null && mapperElement == null) {
            throw new BuildException("dest specified without mapper",
                                     getLocation());
        }
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        }
    }
    protected ExecuteStreamHandler createHandler() throws BuildException {
        return (redirectorElement == null)
            ? super.createHandler() : new PumpStreamHandler();
    }
    protected void setupRedirector() {
        super.setupRedirector();
        redirector.setAppendProperties(true);
    }
    protected void runExec(Execute exe) throws BuildException {
        int totalFiles = 0;
        int totalDirs = 0;
        boolean haveExecuted = false;
        try {
            Vector fileNames = new Vector();
            Vector baseDirs = new Vector();
            for (int i = 0; i < filesets.size(); i++) {
                String currentType = type;
                AbstractFileSet fs = (AbstractFileSet) filesets.elementAt(i);
                if (fs instanceof DirSet) {
                    if (!FileDirBoth.DIR.equals(type)) {
                        log("Found a nested dirset but type is " + type + ". "
                            + "Temporarily switching to type=\"dir\" on the"
                            + " assumption that you really did mean"
                            + " <dirset> not <fileset>.", Project.MSG_DEBUG);
                        currentType = FileDirBoth.DIR;
                    }
                }
                File base = fs.getDir(getProject());
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                if (!FileDirBoth.DIR.equals(currentType)) {
                    String[] s = getFiles(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        totalFiles++;
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }
                if (!FileDirBoth.FILE.equals(currentType)) {
                    String[] s = getDirs(base, ds);
                    for (int j = 0; j < s.length; j++) {
                        totalDirs++;
                        fileNames.addElement(s[j]);
                        baseDirs.addElement(base);
                    }
                }
                if (fileNames.size() == 0 && skipEmpty) {
                    logSkippingFileset(currentType, ds, base);
                    continue;
                }
                if (!parallel) {
                    String[] s = new String[fileNames.size()];
                    fileNames.copyInto(s);
                    for (int j = 0; j < s.length; j++) {
                        String[] command = getCommandline(s[j], base);
                        log(Commandline.describeCommand(command),
                            Project.MSG_VERBOSE);
                        exe.setCommandline(command);
                        if (redirectorElement != null) {
                            setupRedirector();
                            redirectorElement.configure(redirector, s[j]);
                        }
                        if (redirectorElement != null || haveExecuted) {
                            exe.setStreamHandler(redirector.createHandler());
                        }
                        runExecute(exe);
                        haveExecuted = true;
                    }
                    fileNames.removeAllElements();
                    baseDirs.removeAllElements();
                }
            }
            if (resources != null) {
                Iterator iter = resources.iterator();
                while (iter.hasNext()) {
                    Resource res = (Resource) iter.next();
                    if (!res.isExists() && ignoreMissing) {
                        continue;
                    }
                    File base = null;
                    String name = res.getName();
                    FileProvider fp = (FileProvider) res.as(FileProvider.class);
                    if (fp != null) {
                        FileResource fr = ResourceUtils.asFileResource(fp);
                        base = fr.getBaseDir();
                        if (base == null) {
                            name = fr.getFile().getAbsolutePath();
                        }
                    }
                    if (restrict(new String[] {name}, base).length == 0) {
                        continue;
                    }
                    if ((!res.isDirectory() || !res.isExists())
                        && !FileDirBoth.DIR.equals(type)) {
                        totalFiles++;
                    } else if (res.isDirectory()
                               && !FileDirBoth.FILE.equals(type)) {
                        totalDirs++;
                    } else {
                        continue;
                    }
                    baseDirs.add(base);
                    fileNames.add(name);
                    if (!parallel) {
                        String[] command = getCommandline(name, base);
                        log(Commandline.describeCommand(command),
                            Project.MSG_VERBOSE);
                        exe.setCommandline(command);
                        if (redirectorElement != null) {
                            setupRedirector();
                            redirectorElement.configure(redirector, name);
                        }
                        if (redirectorElement != null || haveExecuted) {
                            exe.setStreamHandler(redirector.createHandler());
                        }
                        runExecute(exe);
                        haveExecuted = true;
                        fileNames.removeAllElements();
                        baseDirs.removeAllElements();
                    }
                }
            }
            if (parallel && (fileNames.size() > 0 || !skipEmpty)) {
                runParallel(exe, fileNames, baseDirs);
                haveExecuted = true;
            }
            if (haveExecuted) {
                log("Applied " + cmdl.getExecutable() + " to "
                    + totalFiles + " file"
                    + (totalFiles != 1 ? "s" : "") + " and "
                    + totalDirs + " director"
                    + (totalDirs != 1 ? "ies" : "y") + ".",
                    verbose ? Project.MSG_INFO : Project.MSG_VERBOSE);
            }
        } catch (IOException e) {
            throw new BuildException("Execute failed: " + e, e, getLocation());
        } finally {
            logFlush();
            redirector.setAppendProperties(false);
            redirector.setProperties();
        }
    }
    private void logSkippingFileset(
        String currentType, DirectoryScanner ds, File base) {
        int includedCount
            = ((!FileDirBoth.DIR.equals(currentType))
               ? ds.getIncludedFilesCount() : 0)
            + ((!FileDirBoth.FILE.equals(currentType))
               ? ds.getIncludedDirsCount() : 0);
        log("Skipping fileset for directory " + base + ". It is "
            + ((includedCount > 0) ? "up to date." : "empty."),
             verbose ? Project.MSG_INFO : Project.MSG_VERBOSE);
    }
    protected String[] getCommandline(String[] srcFiles, File[] baseDirs) {
        final char fileSeparator = File.separatorChar;
        Vector targets = new Vector();
        if (targetFilePos != null) {
            HashSet addedFiles = new HashSet();
            for (int i = 0; i < srcFiles.length; i++) {
                String[] subTargets = mapper.mapFileName(srcFiles[i]);
                if (subTargets != null) {
                    for (int j = 0; j < subTargets.length; j++) {
                        String name = null;
                        if (!relative) {
                            name = (new File(destDir, subTargets[j])).getAbsolutePath();
                        } else {
                            name = subTargets[j];
                        }
                        if (forwardSlash && fileSeparator != '/') {
                            name = name.replace(fileSeparator, '/');
                        }
                        if (!addedFiles.contains(name)) {
                            targets.addElement(name);
                            addedFiles.add(name);
                        }
                    }
                }
            }
        }
        String[] targetFiles = (String[]) targets.toArray(new String[targets.size()]);
        if (!addSourceFile) {
            srcFiles = new String[0];
        }
        String[] orig = cmdl.getCommandline();
        String[] result
            = new String[orig.length + srcFiles.length + targetFiles.length];
        int srcIndex = orig.length;
        if (srcFilePos != null) {
            srcIndex = srcFilePos.getPosition();
        }
        if (targetFilePos != null) {
            int targetIndex = targetFilePos.getPosition();
            if (srcIndex < targetIndex
                || (srcIndex == targetIndex && srcIsFirst)) {
                System.arraycopy(orig, 0, result, 0, srcIndex);
                System.arraycopy(orig, srcIndex, result,
                                 srcIndex + srcFiles.length,
                                 targetIndex - srcIndex);
                insertTargetFiles(targetFiles, result,
                                  targetIndex + srcFiles.length,
                                  targetFilePos.getPrefix(),
                                  targetFilePos.getSuffix());
                System.arraycopy(orig, targetIndex, result,
                    targetIndex + srcFiles.length + targetFiles.length,
                    orig.length - targetIndex);
            } else {
                System.arraycopy(orig, 0, result, 0, targetIndex);
                insertTargetFiles(targetFiles, result, targetIndex,
                                  targetFilePos.getPrefix(),
                                  targetFilePos.getSuffix());
                System.arraycopy(orig, targetIndex, result,
                                 targetIndex + targetFiles.length,
                                 srcIndex - targetIndex);
                System.arraycopy(orig, srcIndex, result,
                    srcIndex + srcFiles.length + targetFiles.length,
                    orig.length - srcIndex);
                srcIndex += targetFiles.length;
            }
        } else { 
            System.arraycopy(orig, 0, result, 0, srcIndex);
            System.arraycopy(orig, srcIndex, result,
                             srcIndex + srcFiles.length,
                             orig.length - srcIndex);
        }
        for (int i = 0; i < srcFiles.length; i++) {
            String src;
            if (relative) {
                src = srcFiles[i];
            } else {
                src = new File(baseDirs[i], srcFiles[i]).getAbsolutePath();
            }
            if (forwardSlash && fileSeparator != '/') {
                src = src.replace(fileSeparator, '/');
            }
            if (srcFilePos != null &&
                (srcFilePos.getPrefix().length() > 0
                 || srcFilePos.getSuffix().length() > 0)) {
                src = srcFilePos.getPrefix() + src + srcFilePos.getSuffix();
            }
            result[srcIndex + i] = src;
        }
        return result;
    }
    protected String[] getCommandline(String srcFile, File baseDir) {
        return getCommandline(new String[] {srcFile}, new File[] {baseDir});
    }
    protected String[] getFiles(File baseDir, DirectoryScanner ds) {
        return restrict(ds.getIncludedFiles(), baseDir);
    }
    protected String[] getDirs(File baseDir, DirectoryScanner ds) {
        return restrict(ds.getIncludedDirectories(), baseDir);
    }
    protected String[] getFilesAndDirs(FileList list) {
        return restrict(list.getFiles(getProject()), list.getDir(getProject()));
    }
    private String[] restrict(String[] s, File baseDir) {
        return (mapper == null || force) ? s
            : new SourceFileScanner(this).restrict(s, baseDir, destDir, mapper);
    }
    protected void runParallel(Execute exe, Vector fileNames,
                               Vector baseDirs)
        throws IOException, BuildException {
        String[] s = new String[fileNames.size()];
        fileNames.copyInto(s);
        File[] b = new File[baseDirs.size()];
        baseDirs.copyInto(b);
        if (maxParallel <= 0
            || s.length == 0 ) {
            String[] command = getCommandline(s, b);
            log(Commandline.describeCommand(command), Project.MSG_VERBOSE);
            exe.setCommandline(command);
            if (redirectorElement != null) {
                setupRedirector();
                redirectorElement.configure(redirector, null);
                exe.setStreamHandler(redirector.createHandler());
            }
            runExecute(exe);
        } else {
            int stillToDo = fileNames.size();
            int currentOffset = 0;
            while (stillToDo > 0) {
                int currentAmount = Math.min(stillToDo, maxParallel);
                String[] cs = new String[currentAmount];
                System.arraycopy(s, currentOffset, cs, 0, currentAmount);
                File[] cb = new File[currentAmount];
                System.arraycopy(b, currentOffset, cb, 0, currentAmount);
                String[] command = getCommandline(cs, cb);
                log(Commandline.describeCommand(command), Project.MSG_VERBOSE);
                exe.setCommandline(command);
                if (redirectorElement != null) {
                    setupRedirector();
                    redirectorElement.configure(redirector, null);
                }
                if (redirectorElement != null || currentOffset > 0) {
                    exe.setStreamHandler(redirector.createHandler());
                }
                runExecute(exe);
                stillToDo -= currentAmount;
                currentOffset += currentAmount;
            }
        }
    }
    private static void insertTargetFiles(String[] targetFiles,
                                          String[] arguments,
                                          int insertPosition,
                                          String prefix, String suffix) {
        if (prefix.length() == 0 && suffix.length() == 0) {
            System.arraycopy(targetFiles, 0, arguments, insertPosition,
                             targetFiles.length);
        } else {
            for (int i = 0; i < targetFiles.length; i++) {
                arguments[insertPosition + i] =
                    prefix + targetFiles[i] + suffix;
            }
        }
    }
    public static class FileDirBoth extends EnumeratedAttribute {
        public static final String FILE = "file";
        public static final String DIR = "dir";
       public String[] getValues() {
            return new String[] {FILE, DIR, "both"};
        }
    }
}
