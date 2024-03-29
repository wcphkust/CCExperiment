package org.apache.tools.ant;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.SelectorScanner;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.types.selectors.TokenizedPath;
import org.apache.tools.ant.types.selectors.TokenizedPattern;
import org.apache.tools.ant.util.CollectionUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.SymbolicLinkUtils;
import org.apache.tools.ant.util.VectorSet;
public class DirectoryScanner
       implements FileScanner, SelectorScanner, ResourceFactory {
    private static final boolean ON_VMS = Os.isFamily("openvms");
    protected static final String[] DEFAULTEXCLUDES = {
        SelectorUtils.DEEP_TREE_MATCH + "/*~",
        SelectorUtils.DEEP_TREE_MATCH + "/#*#",
        SelectorUtils.DEEP_TREE_MATCH + "/.#*",
        SelectorUtils.DEEP_TREE_MATCH + "/%*%",
        SelectorUtils.DEEP_TREE_MATCH + "/._*",
        SelectorUtils.DEEP_TREE_MATCH + "/CVS",
        SelectorUtils.DEEP_TREE_MATCH + "/CVS/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.cvsignore",
        SelectorUtils.DEEP_TREE_MATCH + "/SCCS",
        SelectorUtils.DEEP_TREE_MATCH + "/SCCS/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/vssver.scc",
        SelectorUtils.DEEP_TREE_MATCH + "/.svn",
        SelectorUtils.DEEP_TREE_MATCH + "/.svn/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.git",
        SelectorUtils.DEEP_TREE_MATCH + "/.git/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.gitattributes",
        SelectorUtils.DEEP_TREE_MATCH + "/.gitignore",
        SelectorUtils.DEEP_TREE_MATCH + "/.gitmodules",
        SelectorUtils.DEEP_TREE_MATCH + "/.hg",
        SelectorUtils.DEEP_TREE_MATCH + "/.hg/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.hgignore",
        SelectorUtils.DEEP_TREE_MATCH + "/.hgsub",
        SelectorUtils.DEEP_TREE_MATCH + "/.hgsubstate",
        SelectorUtils.DEEP_TREE_MATCH + "/.hgtags",
        SelectorUtils.DEEP_TREE_MATCH + "/.bzr",
        SelectorUtils.DEEP_TREE_MATCH + "/.bzr/" + SelectorUtils.DEEP_TREE_MATCH,
        SelectorUtils.DEEP_TREE_MATCH + "/.bzrignore",
        SelectorUtils.DEEP_TREE_MATCH + "/.DS_Store"
    };
    public static final int MAX_LEVELS_OF_SYMLINKS = 5;
    public static final String DOES_NOT_EXIST_POSTFIX = " does not exist.";
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final SymbolicLinkUtils SYMLINK_UTILS =
        SymbolicLinkUtils.getSymbolicLinkUtils();
    private static Set defaultExcludes = new HashSet();
    static {
        resetDefaultExcludes();
    }
    protected File basedir;
    protected String[] includes;
    protected String[] excludes;
    protected FileSelector[] selectors = null;
    protected Vector filesIncluded;
    protected Vector filesNotIncluded;
    protected Vector filesExcluded;
    protected Vector dirsIncluded;
    protected Vector dirsNotIncluded;
    protected Vector dirsExcluded;
    protected Vector filesDeselected;
    protected Vector dirsDeselected;
    protected boolean haveSlowResults = false;
    protected boolean isCaseSensitive = true;
    protected boolean errorOnMissingDir = true;
    private boolean followSymlinks = true;
    protected boolean everythingIncluded = true;
    private Set scannedDirs = new HashSet();
    private Map includeNonPatterns = new HashMap();
    private Map excludeNonPatterns = new HashMap();
    private TokenizedPattern[] includePatterns;
    private TokenizedPattern[] excludePatterns;
    private boolean areNonPatternSetsReady = false;
    private boolean scanning = false;
    private Object scanLock = new Object();
    private boolean slowScanning = false;
    private Object slowScanLock = new Object();
    private IllegalStateException illegal = null;
    private int maxLevelsOfSymlinks = MAX_LEVELS_OF_SYMLINKS;
    private Set notFollowedSymlinks = new HashSet();
    public DirectoryScanner() {
    }
    protected static boolean matchPatternStart(String pattern, String str) {
        return SelectorUtils.matchPatternStart(pattern, str);
    }
    protected static boolean matchPatternStart(String pattern, String str,
                                               boolean isCaseSensitive) {
        return SelectorUtils.matchPatternStart(pattern, str, isCaseSensitive);
    }
    protected static boolean matchPath(String pattern, String str) {
        return SelectorUtils.matchPath(pattern, str);
    }
    protected static boolean matchPath(String pattern, String str,
                                       boolean isCaseSensitive) {
        return SelectorUtils.matchPath(pattern, str, isCaseSensitive);
    }
    public static boolean match(String pattern, String str) {
        return SelectorUtils.match(pattern, str);
    }
    protected static boolean match(String pattern, String str,
                                   boolean isCaseSensitive) {
        return SelectorUtils.match(pattern, str, isCaseSensitive);
    }
    public static String[] getDefaultExcludes() {
        return (String[]) defaultExcludes.toArray(new String[defaultExcludes
                                                             .size()]);
    }
    public static boolean addDefaultExclude(String s) {
        return defaultExcludes.add(s);
    }
    public static boolean removeDefaultExclude(String s) {
        return defaultExcludes.remove(s);
    }
    public static void resetDefaultExcludes() {
        defaultExcludes = new HashSet();
        for (int i = 0; i < DEFAULTEXCLUDES.length; i++) {
            defaultExcludes.add(DEFAULTEXCLUDES[i]);
        }
    }
    public void setBasedir(String basedir) {
        setBasedir(basedir == null ? (File) null
            : new File(basedir.replace('/', File.separatorChar).replace(
            '\\', File.separatorChar)));
    }
    public synchronized void setBasedir(File basedir) {
        this.basedir = basedir;
    }
    public synchronized File getBasedir() {
        return basedir;
    }
    public synchronized boolean isCaseSensitive() {
        return isCaseSensitive;
    }
    public synchronized void setCaseSensitive(boolean isCaseSensitive) {
        this.isCaseSensitive = isCaseSensitive;
    }
    public void setErrorOnMissingDir(boolean errorOnMissingDir) {
        this.errorOnMissingDir = errorOnMissingDir;
    }
    public synchronized boolean isFollowSymlinks() {
        return followSymlinks;
    }
    public synchronized void setFollowSymlinks(boolean followSymlinks) {
        this.followSymlinks = followSymlinks;
    }
    public void setMaxLevelsOfSymlinks(int max) {
        maxLevelsOfSymlinks = max;
    }
    public synchronized void setIncludes(String[] includes) {
        if (includes == null) {
            this.includes = null;
        } else {
            this.includes = new String[includes.length];
            for (int i = 0; i < includes.length; i++) {
                this.includes[i] = normalizePattern(includes[i]);
            }
        }
    }
    public synchronized void setExcludes(String[] excludes) {
        if (excludes == null) {
            this.excludes = null;
        } else {
            this.excludes = new String[excludes.length];
            for (int i = 0; i < excludes.length; i++) {
                this.excludes[i] = normalizePattern(excludes[i]);
            }
        }
    }
    public synchronized void addExcludes(String[] excludes) {
        if (excludes != null && excludes.length > 0) {
            if (this.excludes != null && this.excludes.length > 0) {
                String[] tmp = new String[excludes.length
                                          + this.excludes.length];
                System.arraycopy(this.excludes, 0, tmp, 0,
                                 this.excludes.length);
                for (int i = 0; i < excludes.length; i++) {
                    tmp[this.excludes.length + i] =
                        normalizePattern(excludes[i]);
                }
                this.excludes = tmp;
            } else {
                setExcludes(excludes);
            }
        }
    }
    private static String normalizePattern(String p) {
        String pattern = p.replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);
        if (pattern.endsWith(File.separator)) {
            pattern += SelectorUtils.DEEP_TREE_MATCH;
        }
        return pattern;
    }
    public synchronized void setSelectors(FileSelector[] selectors) {
        this.selectors = selectors;
    }
    public synchronized boolean isEverythingIncluded() {
        return everythingIncluded;
    }
    public void scan() throws IllegalStateException {
        synchronized (scanLock) {
            if (scanning) {
                while (scanning) {
                    try {
                        scanLock.wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
                if (illegal != null) {
                    throw illegal;
                }
                return;
            }
            scanning = true;
        }
        File savedBase = basedir;
        try {
            synchronized (this) {
                illegal = null;
                clearResults();
                boolean nullIncludes = (includes == null);
                includes = nullIncludes
                    ? new String[] {SelectorUtils.DEEP_TREE_MATCH} : includes;
                boolean nullExcludes = (excludes == null);
                excludes = nullExcludes ? new String[0] : excludes;
                if (basedir != null && !followSymlinks
                    && SYMLINK_UTILS.isSymbolicLink(basedir)) {
                    notFollowedSymlinks.add(basedir.getAbsolutePath());
                    basedir = null;
                }
                if (basedir == null) {
                    if (nullIncludes) {
                        return;
                    }
                } else {
                    if (!basedir.exists()) {
                        if (errorOnMissingDir) {
                            illegal = new IllegalStateException("basedir "
                                                                + basedir
                                                                + DOES_NOT_EXIST_POSTFIX);
                        } else {
                            return;
                        }
                    } else if (!basedir.isDirectory()) {
                        illegal = new IllegalStateException("basedir "
                                                            + basedir
                                                            + " is not a"
                                                            + " directory.");
                    }
                    if (illegal != null) {
                        throw illegal;
                    }
                }
                if (isIncluded(TokenizedPath.EMPTY_PATH)) {
                    if (!isExcluded(TokenizedPath.EMPTY_PATH)) {
                        if (isSelected("", basedir)) {
                            dirsIncluded.addElement("");
                        } else {
                            dirsDeselected.addElement("");
                        }
                    } else {
                        dirsExcluded.addElement("");
                    }
                } else {
                    dirsNotIncluded.addElement("");
                }
                checkIncludePatterns();
                clearCaches();
                includes = nullIncludes ? null : includes;
                excludes = nullExcludes ? null : excludes;
            }
        } catch (IOException ex) {
            throw new BuildException(ex);
        } finally {
            basedir = savedBase;
            synchronized (scanLock) {
                scanning = false;
                scanLock.notifyAll();
            }
        }
    }
    private void checkIncludePatterns() {
        ensureNonPatternSetsReady();
        Map newroots = new HashMap();
        for (int i = 0; i < includePatterns.length; i++) {
            String pattern = includePatterns[i].toString();
            if (!shouldSkipPattern(pattern)) {
                newroots.put(includePatterns[i].rtrimWildcardTokens(),
                             pattern);
            }
        }
        for (Iterator iter = includeNonPatterns.entrySet().iterator();
             iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            String pattern = (String) entry.getKey();
            if (!shouldSkipPattern(pattern)) {
                newroots.put((TokenizedPath) entry.getValue(), pattern);
            }
        }
        if (newroots.containsKey(TokenizedPath.EMPTY_PATH)
            && basedir != null) {
            scandir(basedir, "", true);
        } else {
            Iterator it = newroots.entrySet().iterator();
            File canonBase = null;
            if (basedir != null) {
                try {
                    canonBase = basedir.getCanonicalFile();
                } catch (IOException ex) {
                    throw new BuildException(ex);
                }
            }
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                TokenizedPath currentPath = (TokenizedPath) entry.getKey();
                String currentelement = currentPath.toString();
                if (basedir == null
                    && !FileUtils.isAbsolutePath(currentelement)) {
                    continue;
                }
                File myfile = new File(basedir, currentelement);
                if (myfile.exists()) {
                    try {
                        String path = (basedir == null)
                            ? myfile.getCanonicalPath()
                            : FILE_UTILS.removeLeadingPath(canonBase,
                                         myfile.getCanonicalFile());
                        if (!path.equals(currentelement) || ON_VMS) {
                            myfile = currentPath.findFile(basedir, true);
                            if (myfile != null && basedir != null) {
                                currentelement = FILE_UTILS.removeLeadingPath(
                                    basedir, myfile);
                                if (!currentPath.toString()
                                    .equals(currentelement)) {
                                    currentPath =
                                        new TokenizedPath(currentelement);
                                }
                            }
                        }
                    } catch (IOException ex) {
                        throw new BuildException(ex);
                    }
                }
                if ((myfile == null || !myfile.exists()) && !isCaseSensitive()) {
                    File f = currentPath.findFile(basedir, false);
                    if (f != null && f.exists()) {
                        currentelement = (basedir == null)
                            ? f.getAbsolutePath()
                            : FILE_UTILS.removeLeadingPath(basedir, f);
                        myfile = f;
                        currentPath = new TokenizedPath(currentelement);
                    }
                }
                if (myfile != null && myfile.exists()) {
                    if (!followSymlinks && currentPath.isSymlink(basedir)) {
                        if (!isExcluded(currentPath)) {
                            notFollowedSymlinks.add(myfile.getAbsolutePath());
                        }
                        continue;
                    }
                    if (myfile.isDirectory()) {
                        if (isIncluded(currentPath)
                            && currentelement.length() > 0) {
                            accountForIncludedDir(currentPath, myfile, true);
                        }  else {
                            scandir(myfile, currentPath, true);
                        }
                    } else {
                        String originalpattern = (String) entry.getValue();
                        boolean included = isCaseSensitive()
                            ? originalpattern.equals(currentelement)
                            : originalpattern.equalsIgnoreCase(currentelement);
                        if (included) {
                            accountForIncludedFile(currentPath, myfile);
                        }
                    }
                }
            }
        }
    }
    private boolean shouldSkipPattern(String pattern) {
        if (FileUtils.isAbsolutePath(pattern)) {
            if (basedir != null
                && !SelectorUtils.matchPatternStart(pattern,
                                                    basedir.getAbsolutePath(),
                                                    isCaseSensitive())) {
                return true;
            }
        } else if (basedir == null) {
            return true;
        }
        return false;
    }
    protected synchronized void clearResults() {
        filesIncluded    = new VectorSet();
        filesNotIncluded = new VectorSet();
        filesExcluded    = new VectorSet();
        filesDeselected  = new VectorSet();
        dirsIncluded     = new VectorSet();
        dirsNotIncluded  = new VectorSet();
        dirsExcluded     = new VectorSet();
        dirsDeselected   = new VectorSet();
        everythingIncluded = (basedir != null);
        scannedDirs.clear();
        notFollowedSymlinks.clear();
    }
    protected void slowScan() {
        synchronized (slowScanLock) {
            if (haveSlowResults) {
                return;
            }
            if (slowScanning) {
                while (slowScanning) {
                    try {
                        slowScanLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                return;
            }
            slowScanning = true;
        }
        try {
            synchronized (this) {
                boolean nullIncludes = (includes == null);
                includes = nullIncludes
                    ? new String[] {SelectorUtils.DEEP_TREE_MATCH} : includes;
                boolean nullExcludes = (excludes == null);
                excludes = nullExcludes ? new String[0] : excludes;
                String[] excl = new String[dirsExcluded.size()];
                dirsExcluded.copyInto(excl);
                String[] notIncl = new String[dirsNotIncluded.size()];
                dirsNotIncluded.copyInto(notIncl);
                ensureNonPatternSetsReady();
                processSlowScan(excl);
                processSlowScan(notIncl);
                clearCaches();
                includes = nullIncludes ? null : includes;
                excludes = nullExcludes ? null : excludes;
            }
        } finally {
            synchronized (slowScanLock) {
                haveSlowResults = true;
                slowScanning = false;
                slowScanLock.notifyAll();
            }
        }
    }
    private void processSlowScan(String[] arr) {
        for (int i = 0; i < arr.length; i++) {
            TokenizedPath path  = new TokenizedPath(arr[i]);
            if (!couldHoldIncluded(path) || contentsExcluded(path)) {
                scandir(new File(basedir, arr[i]), path, false);
            }
        }
    }
    protected void scandir(File dir, String vpath, boolean fast) {
        scandir(dir, new TokenizedPath(vpath), fast);
    }
    private void scandir(File dir, TokenizedPath path, boolean fast) {
        if (dir == null) {
            throw new BuildException("dir must not be null.");
        }
        String[] newfiles = dir.list();
        if (newfiles == null) {
            if (!dir.exists()) {
                throw new BuildException(dir + DOES_NOT_EXIST_POSTFIX);
            } else if (!dir.isDirectory()) {
                throw new BuildException(dir + " is not a directory.");
            } else {
                throw new BuildException("IO error scanning directory '"
                                         + dir.getAbsolutePath() + "'");
            }
        }
        scandir(dir, path, fast, newfiles, new LinkedList());
    }
    private void scandir(File dir, TokenizedPath path, boolean fast,
                         String[] newfiles, LinkedList directoryNamesFollowed) {
        String vpath = path.toString();
        if (vpath.length() > 0 && !vpath.endsWith(File.separator)) {
            vpath += File.separator;
        }
        if (fast && hasBeenScanned(vpath)) {
            return;
        }
        if (!followSymlinks) {
            ArrayList noLinks = new ArrayList();
            for (int i = 0; i < newfiles.length; i++) {
                try {
                    if (SYMLINK_UTILS.isSymbolicLink(dir, newfiles[i])) {
                        String name = vpath + newfiles[i];
                        File file = new File(dir, newfiles[i]);
                        (file.isDirectory()
                            ? dirsExcluded : filesExcluded).addElement(name);
                        if (!isExcluded(name)) {
                            notFollowedSymlinks.add(file.getAbsolutePath());
                        }
                    } else {
                        noLinks.add(newfiles[i]);
                    }
                } catch (IOException ioe) {
                    String msg = "IOException caught while checking "
                        + "for links, couldn't get canonical path!";
                    System.err.println(msg);
                    noLinks.add(newfiles[i]);
                }
            }
            newfiles = (String[]) (noLinks.toArray(new String[noLinks.size()]));
        } else {
            directoryNamesFollowed.addFirst(dir.getName());
        }
        for (int i = 0; i < newfiles.length; i++) {
            String name = vpath + newfiles[i];
            TokenizedPath newPath = new TokenizedPath(path, newfiles[i]);
            File file = new File(dir, newfiles[i]);
            String[] children = file.list();
            if (children == null || (children.length == 0 && file.isFile())) {
                if (isIncluded(newPath)) {
                    accountForIncludedFile(newPath, file);
                } else {
                    everythingIncluded = false;
                    filesNotIncluded.addElement(name);
                }
            } else { 
                if (followSymlinks
                    && causesIllegalSymlinkLoop(newfiles[i], dir,
                                                directoryNamesFollowed)) {
                    System.err.println("skipping symbolic link "
                                       + file.getAbsolutePath()
                                       + " -- too many levels of symbolic"
                                       + " links.");
                    notFollowedSymlinks.add(file.getAbsolutePath());
                    continue;
                }
                if (isIncluded(newPath)) {
                    accountForIncludedDir(newPath, file, fast, children,
                                          directoryNamesFollowed);
                } else {
                    everythingIncluded = false;
                    dirsNotIncluded.addElement(name);
                    if (fast && couldHoldIncluded(newPath)
                        && !contentsExcluded(newPath)) {
                        scandir(file, newPath, fast, children,
                                directoryNamesFollowed);
                    }
                }
                if (!fast) {
                    scandir(file, newPath, fast, children, directoryNamesFollowed);
                }
            }
        }
        if (followSymlinks) {
            directoryNamesFollowed.removeFirst();
        }
    }
    private void accountForIncludedFile(TokenizedPath name, File file) {
        processIncluded(name, file, filesIncluded, filesExcluded,
                        filesDeselected);
    }
    private void accountForIncludedDir(TokenizedPath name, File file,
                                       boolean fast) {
        processIncluded(name, file, dirsIncluded, dirsExcluded, dirsDeselected);
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name, fast);
        }
    }
    private void accountForIncludedDir(TokenizedPath name,
                                       File file, boolean fast,
                                       String[] children,
                                       LinkedList directoryNamesFollowed) {
        processIncluded(name, file, dirsIncluded, dirsExcluded, dirsDeselected);
        if (fast && couldHoldIncluded(name) && !contentsExcluded(name)) {
            scandir(file, name, fast, children, directoryNamesFollowed);
        }
    }
    private void processIncluded(TokenizedPath path,
                                 File file, Vector inc, Vector exc,
                                 Vector des) {
        String name = path.toString();
        if (inc.contains(name) || exc.contains(name) || des.contains(name)) {
            return;
        }
        boolean included = false;
        if (isExcluded(path)) {
            exc.add(name);
        } else if (isSelected(name, file)) {
            included = true;
            inc.add(name);
        } else {
            des.add(name);
        }
        everythingIncluded &= included;
    }
    protected boolean isIncluded(String name) {
        return isIncluded(new TokenizedPath(name));
    }
    private boolean isIncluded(TokenizedPath path) {
        ensureNonPatternSetsReady();
        if (isCaseSensitive()
            ? includeNonPatterns.containsKey(path.toString())
            : includeNonPatterns.containsKey(path.toString().toUpperCase())) {
            return true;
        }
        for (int i = 0; i < includePatterns.length; i++) {
            if (includePatterns[i].matchPath(path, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }
    protected boolean couldHoldIncluded(String name) {
        return couldHoldIncluded(new TokenizedPath(name));
    }
    private boolean couldHoldIncluded(TokenizedPath tokenizedName) {
        for (int i = 0; i < includePatterns.length; i++) {
            if (couldHoldIncluded(tokenizedName, includePatterns[i])) {
                return true;
            }
        }
        for (Iterator iter = includeNonPatterns.values().iterator();
             iter.hasNext(); ) {
            if (couldHoldIncluded(tokenizedName,
                                  ((TokenizedPath) iter.next()).toPattern())) {
                return true;
            }
        }
        return false;
    }
    private boolean couldHoldIncluded(TokenizedPath tokenizedName,
                                      TokenizedPattern tokenizedInclude) {
        return tokenizedInclude.matchStartOf(tokenizedName, isCaseSensitive())
            && isMorePowerfulThanExcludes(tokenizedName.toString())
            && isDeeper(tokenizedInclude, tokenizedName);
    }
    private boolean isDeeper(TokenizedPattern pattern, TokenizedPath name) {
        return pattern.containsPattern(SelectorUtils.DEEP_TREE_MATCH)
            || pattern.depth() > name.depth();
    }
    private boolean isMorePowerfulThanExcludes(String name) {
        final String soughtexclude =
            name + File.separatorChar + SelectorUtils.DEEP_TREE_MATCH;
        for (int counter = 0; counter < excludePatterns.length; counter++) {
            if (excludePatterns[counter].toString().equals(soughtexclude))  {
                return false;
            }
        }
        return true;
    }
     boolean contentsExcluded(TokenizedPath path) {
        for (int i = 0; i < excludePatterns.length; i++) {
            if (excludePatterns[i].endsWith(SelectorUtils.DEEP_TREE_MATCH)
                && excludePatterns[i].withoutLastToken()
                   .matchPath(path, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }
    protected boolean isExcluded(String name) {
        return isExcluded(new TokenizedPath(name));
    }
    private boolean isExcluded(TokenizedPath name) {
        ensureNonPatternSetsReady();
        if (isCaseSensitive()
            ? excludeNonPatterns.containsKey(name.toString())
            : excludeNonPatterns.containsKey(name.toString().toUpperCase())) {
            return true;
        }
        for (int i = 0; i < excludePatterns.length; i++) {
            if (excludePatterns[i].matchPath(name, isCaseSensitive())) {
                return true;
            }
        }
        return false;
    }
    protected boolean isSelected(String name, File file) {
        if (selectors != null) {
            for (int i = 0; i < selectors.length; i++) {
                if (!selectors[i].isSelected(basedir, name, file)) {
                    return false;
                }
            }
        }
        return true;
    }
    public String[] getIncludedFiles() {
        String[] files;
        synchronized (this) {
            if (filesIncluded == null) {
                throw new IllegalStateException("Must call scan() first");
            }
            files = new String[filesIncluded.size()];
            filesIncluded.copyInto(files);
        }
        Arrays.sort(files);
        return files;
    }
    public synchronized int getIncludedFilesCount() {
        if (filesIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        return filesIncluded.size();
    }
    public synchronized String[] getNotIncludedFiles() {
        slowScan();
        String[] files = new String[filesNotIncluded.size()];
        filesNotIncluded.copyInto(files);
        return files;
    }
    public synchronized String[] getExcludedFiles() {
        slowScan();
        String[] files = new String[filesExcluded.size()];
        filesExcluded.copyInto(files);
        return files;
    }
    public synchronized String[] getDeselectedFiles() {
        slowScan();
        String[] files = new String[filesDeselected.size()];
        filesDeselected.copyInto(files);
        return files;
    }
    public String[] getIncludedDirectories() {
        String[] directories;
        synchronized (this) {
            if (dirsIncluded == null) {
                throw new IllegalStateException("Must call scan() first");
            }
            directories = new String[dirsIncluded.size()];
            dirsIncluded.copyInto(directories);
        }
        Arrays.sort(directories);
        return directories;
    }
    public synchronized int getIncludedDirsCount() {
        if (dirsIncluded == null) {
            throw new IllegalStateException("Must call scan() first");
        }
        return dirsIncluded.size();
    }
    public synchronized String[] getNotIncludedDirectories() {
        slowScan();
        String[] directories = new String[dirsNotIncluded.size()];
        dirsNotIncluded.copyInto(directories);
        return directories;
    }
    public synchronized String[] getExcludedDirectories() {
        slowScan();
        String[] directories = new String[dirsExcluded.size()];
        dirsExcluded.copyInto(directories);
        return directories;
    }
    public synchronized String[] getDeselectedDirectories() {
        slowScan();
        String[] directories = new String[dirsDeselected.size()];
        dirsDeselected.copyInto(directories);
        return directories;
    }
    public synchronized String[] getNotFollowedSymlinks() {
        String[] links;
        synchronized (this) {
            links = (String[]) notFollowedSymlinks
                .toArray(new String[notFollowedSymlinks.size()]);
        }
        Arrays.sort(links);
        return links;
    }
    public synchronized void addDefaultExcludes() {
        int excludesLength = excludes == null ? 0 : excludes.length;
        String[] newExcludes;
        newExcludes = new String[excludesLength + defaultExcludes.size()];
        if (excludesLength > 0) {
            System.arraycopy(excludes, 0, newExcludes, 0, excludesLength);
        }
        String[] defaultExcludesTemp = getDefaultExcludes();
        for (int i = 0; i < defaultExcludesTemp.length; i++) {
            newExcludes[i + excludesLength] =
                defaultExcludesTemp[i].replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);
        }
        excludes = newExcludes;
    }
    public synchronized Resource getResource(String name) {
        return new FileResource(basedir, name);
    }
    private boolean hasBeenScanned(String vpath) {
        return !scannedDirs.add(vpath);
    }
     Set getScannedDirs() {
        return scannedDirs;
    }
    private synchronized void clearCaches() {
        includeNonPatterns.clear();
        excludeNonPatterns.clear();
        includePatterns = null;
        excludePatterns = null;
        areNonPatternSetsReady = false;
    }
     synchronized void ensureNonPatternSetsReady() {
        if (!areNonPatternSetsReady) {
            includePatterns = fillNonPatternSet(includeNonPatterns, includes);
            excludePatterns = fillNonPatternSet(excludeNonPatterns, excludes);
            areNonPatternSetsReady = true;
        }
    }
    private TokenizedPattern[] fillNonPatternSet(Map map, String[] patterns) {
        ArrayList al = new ArrayList(patterns.length);
        for (int i = 0; i < patterns.length; i++) {
            if (!SelectorUtils.hasWildcards(patterns[i])) {
                String s = isCaseSensitive()
                    ? patterns[i] : patterns[i].toUpperCase();
                map.put(s, new TokenizedPath(s));
            } else {
                al.add(new TokenizedPattern(patterns[i]));
            }
        }
        return (TokenizedPattern[]) al.toArray(new TokenizedPattern[al.size()]);
    }
    private boolean causesIllegalSymlinkLoop(String dirName, File parent,
                                             LinkedList directoryNamesFollowed) {
        try {
            if (directoryNamesFollowed.size() >= maxLevelsOfSymlinks
                && CollectionUtils.frequency(directoryNamesFollowed, dirName)
                   >= maxLevelsOfSymlinks
                && SYMLINK_UTILS.isSymbolicLink(parent, dirName)) {
                ArrayList files = new ArrayList();
                File f = FILE_UTILS.resolveFile(parent, dirName);
                String target = f.getCanonicalPath();
                files.add(target);
                String relPath = "";
                for (Iterator i = directoryNamesFollowed.iterator();
                     i.hasNext(); ) {
                    relPath += "../";
                    String dir = (String) i.next();
                    if (dirName.equals(dir)) {
                        f = FILE_UTILS.resolveFile(parent, relPath + dir);
                        files.add(f.getCanonicalPath());
                        if (files.size() > maxLevelsOfSymlinks
                            && CollectionUtils.frequency(files, target)
                                 > maxLevelsOfSymlinks) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (IOException ex) {
            throw new BuildException("Caught error while checking for"
                                     + " symbolic links", ex);
        }
    }
}
