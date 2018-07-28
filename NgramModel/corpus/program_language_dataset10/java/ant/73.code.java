package org.apache.tools.ant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.property.ResolvePropertyMap;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.ProxySetup;
public class Main implements AntMain {
    private static final Set LAUNCH_COMMANDS = new HashSet();
    static {
        LAUNCH_COMMANDS.add("-lib");
        LAUNCH_COMMANDS.add("-cp");
        LAUNCH_COMMANDS.add("-noclasspath");
        LAUNCH_COMMANDS.add("--noclasspath");
        LAUNCH_COMMANDS.add("-nouserlib");
        LAUNCH_COMMANDS.add("-main");
    }
    public static final String DEFAULT_BUILD_FILENAME = "build.xml";
    private int msgOutputLevel = Project.MSG_INFO;
    private File buildFile; 
    private static PrintStream out = System.out;
    private static PrintStream err = System.err;
    private Vector targets = new Vector();
    private Properties definedProps = new Properties();
    private Vector listeners = new Vector(1);
    private Vector propertyFiles = new Vector(1);
    private boolean allowInput = true;
    private boolean keepGoingMode = false;
    private String loggerClassname = null;
    private String inputHandlerClassname = null;
    private boolean emacsMode = false;
    private boolean readyToRun = false;
    private boolean projectHelp = false;
    private static boolean isLogFileUsed = false;
    private Integer threadPriority = null;
    private boolean proxy = false;
    private static void printMessage(Throwable t) {
        String message = t.getMessage();
        if (message != null) {
            System.err.println(message);
        }
    }
    public static void start(String[] args, Properties additionalUserProperties,
                             ClassLoader coreLoader) {
        Main m = new Main();
        m.startAnt(args, additionalUserProperties, coreLoader);
    }
    public void startAnt(String[] args, Properties additionalUserProperties,
                         ClassLoader coreLoader) {
        try {
            processArgs(args);
        } catch (Throwable exc) {
            handleLogfile();
            printMessage(exc);
            exit(1);
            return;
        }
        if (additionalUserProperties != null) {
            for (Enumeration e = additionalUserProperties.keys();
                    e.hasMoreElements();) {
                String key = (String) e.nextElement();
                String property = additionalUserProperties.getProperty(key);
                definedProps.put(key, property);
            }
        }
        int exitCode = 1;
        try {
            try {
                runBuild(coreLoader);
                exitCode = 0;
            } catch (ExitStatusException ese) {
                exitCode = ese.getStatus();
                if (exitCode != 0) {
                    throw ese;
                }
            }
        } catch (BuildException be) {
            if (err != System.err) {
                printMessage(be);
            }
        } catch (Throwable exc) {
            exc.printStackTrace();
            printMessage(exc);
        } finally {
            handleLogfile();
        }
        exit(exitCode);
    }
    protected void exit(int exitCode) {
        System.exit(exitCode);
    }
    private static void handleLogfile() {
        if (isLogFileUsed) {
            FileUtils.close(out);
            FileUtils.close(err);
        }
    }
    public static void main(String[] args) {
        start(args, null, null);
    }
    public Main() {
    }
    protected Main(String[] args) throws BuildException {
        processArgs(args);
    }
    private void processArgs(String[] args) {
        String searchForThis = null;
        boolean searchForFile = false;
        PrintStream logTo = null;
        boolean justPrintUsage = false;
        boolean justPrintVersion = false;
        boolean justPrintDiagnostics = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-help") || arg.equals("-h")) {
                justPrintUsage = true;
            } else if (arg.equals("-version")) {
                justPrintVersion = true;
            } else if (arg.equals("-diagnostics")) {
                justPrintDiagnostics = true;
            } else if (arg.equals("-quiet") || arg.equals("-q")) {
                msgOutputLevel = Project.MSG_WARN;
            } else if (arg.equals("-verbose") || arg.equals("-v")) {
                msgOutputLevel = Project.MSG_VERBOSE;
            } else if (arg.equals("-debug") || arg.equals("-d")) {
                msgOutputLevel = Project.MSG_DEBUG;
            } else if (arg.equals("-noinput")) {
                allowInput = false;
            } else if (arg.equals("-logfile") || arg.equals("-l")) {
                try {
                    File logFile = new File(args[i + 1]);
                    i++;
                    logTo = new PrintStream(new FileOutputStream(logFile));
                    isLogFileUsed = true;
                } catch (IOException ioe) {
                    String msg = "Cannot write on the specified log file. "
                        + "Make sure the path exists and you have write "
                        + "permissions.";
                    throw new BuildException(msg);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    String msg = "You must specify a log file when "
                        + "using the -log argument";
                    throw new BuildException(msg);
                }
            } else if (arg.equals("-buildfile") || arg.equals("-file")
                       || arg.equals("-f")) {
                i = handleArgBuildFile(args, i);
            } else if (arg.equals("-listener")) {
                i = handleArgListener(args, i);
            } else if (arg.startsWith("-D")) {
                i = handleArgDefine(args, i);
            } else if (arg.equals("-logger")) {
                i = handleArgLogger(args, i);
            } else if (arg.equals("-inputhandler")) {
                i = handleArgInputHandler(args, i);
            } else if (arg.equals("-emacs") || arg.equals("-e")) {
                emacsMode = true;
            } else if (arg.equals("-projecthelp") || arg.equals("-p")) {
                projectHelp = true;
            } else if (arg.equals("-find") || arg.equals("-s")) {
                searchForFile = true;
                if (i < args.length - 1) {
                    searchForThis = args[++i];
                }
            } else if (arg.startsWith("-propertyfile")) {
                i = handleArgPropertyFile(args, i);
            } else if (arg.equals("-k") || arg.equals("-keep-going")) {
                keepGoingMode = true;
            } else if (arg.equals("-nice")) {
                i = handleArgNice(args, i);
            } else if (LAUNCH_COMMANDS.contains(arg)) {
                String msg = "Ant's Main method is being handed "
                        + "an option " + arg + " that is only for the launcher class."
                        + "\nThis can be caused by a version mismatch between "
                        + "the ant script/.bat file and Ant itself.";
                throw new BuildException(msg);
            } else if (arg.equals("-autoproxy")) {
                proxy = true;
            } else if (arg.startsWith("-")) {
                String msg = "Unknown argument: " + arg;
                System.err.println(msg);
                printUsage();
                throw new BuildException("");
            } else {
                targets.addElement(arg);
            }
        }
        if (msgOutputLevel >= Project.MSG_VERBOSE || justPrintVersion) {
            printVersion(msgOutputLevel);
        }
        if (justPrintUsage || justPrintVersion || justPrintDiagnostics) {
            if (justPrintUsage) {
                printUsage();
            }
            if (justPrintDiagnostics) {
                Diagnostics.doReport(System.out, msgOutputLevel);
            }
            return;
        }
        if (buildFile == null) {
            if (searchForFile) {
                if (searchForThis != null) {
                    buildFile = findBuildFile(System.getProperty("user.dir"), searchForThis);
                    if (buildFile == null) {
                        throw new BuildException("Could not locate a build file!");
                    }
                } else {
                    Iterator it = ProjectHelperRepository.getInstance().getHelpers();
                    do {
                        ProjectHelper helper = (ProjectHelper) it.next();
                        searchForThis = helper.getDefaultBuildFile();
                        if (msgOutputLevel >= Project.MSG_VERBOSE) {
                            System.out.println("Searching the default build file: " + searchForThis);
                        }
                        buildFile = findBuildFile(System.getProperty("user.dir"), searchForThis);
                    } while (buildFile == null && it.hasNext());
                    if (buildFile == null) {
                        throw new BuildException("Could not locate a build file!");
                    }
                }
            } else {
                Iterator it = ProjectHelperRepository.getInstance().getHelpers();
                do {
                    ProjectHelper helper = (ProjectHelper) it.next();
                    buildFile = new File(helper.getDefaultBuildFile());
                    if (msgOutputLevel >= Project.MSG_VERBOSE) {
                        System.out.println("Trying the default build file: " + buildFile);
                    }
                } while (!buildFile.exists() && it.hasNext());
            }
        }
        if (!buildFile.exists()) {
            System.out.println("Buildfile: " + buildFile + " does not exist!");
            throw new BuildException("Build failed");
        }
        if (buildFile.isDirectory()) {
            System.out.println("What? Buildfile: " + buildFile + " is a dir!");
            throw new BuildException("Build failed");
        }
        buildFile =
            FileUtils.getFileUtils().normalize(buildFile.getAbsolutePath());
        loadPropertyFiles();
        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Buildfile: " + buildFile);
        }
        if (logTo != null) {
            out = logTo;
            err = logTo;
            System.setOut(out);
            System.setErr(err);
        }
        readyToRun = true;
    }
    private int handleArgBuildFile(String[] args, int pos) {
        try {
            buildFile = new File(
                args[++pos].replace('/', File.separatorChar));
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must specify a buildfile when using the -buildfile argument");
        }
        return pos;
    }
    private int handleArgListener(String[] args, int pos) {
        try {
            listeners.addElement(args[pos + 1]);
            pos++;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            String msg = "You must specify a classname when "
                + "using the -listener argument";
            throw new BuildException(msg);
        }
        return pos;
    }
    private int handleArgDefine(String[] args, int argPos) {
        String arg = args[argPos];
        String name = arg.substring(2, arg.length());
        String value = null;
        int posEq = name.indexOf("=");
        if (posEq > 0) {
            value = name.substring(posEq + 1);
            name = name.substring(0, posEq);
        } else if (argPos < args.length - 1) {
            value = args[++argPos];
        } else {
            throw new BuildException("Missing value for property "
                                     + name);
        }
        definedProps.put(name, value);
        return argPos;
    }
    private int handleArgLogger(String[] args, int pos) {
        if (loggerClassname != null) {
            throw new BuildException(
                "Only one logger class may be specified.");
        }
        try {
            loggerClassname = args[++pos];
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must specify a classname when using the -logger argument");
        }
        return pos;
    }
    private int handleArgInputHandler(String[] args, int pos) {
        if (inputHandlerClassname != null) {
            throw new BuildException("Only one input handler class may "
                                     + "be specified.");
        }
        try {
            inputHandlerClassname = args[++pos];
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException("You must specify a classname when"
                                     + " using the -inputhandler"
                                     + " argument");
        }
        return pos;
    }
    private int handleArgPropertyFile(String[] args, int pos) {
        try {
            propertyFiles.addElement(args[++pos]);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            String msg = "You must specify a property filename when "
                + "using the -propertyfile argument";
            throw new BuildException(msg);
        }
        return pos;
    }
    private int handleArgNice(String[] args, int pos) {
        try {
            threadPriority = Integer.decode(args[++pos]);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new BuildException(
                "You must supply a niceness value (1-10)"
                + " after the -nice option");
        } catch (NumberFormatException e) {
            throw new BuildException("Unrecognized niceness value: "
                                     + args[pos]);
        }
        if (threadPriority.intValue() < Thread.MIN_PRIORITY
            || threadPriority.intValue() > Thread.MAX_PRIORITY) {
            throw new BuildException(
                "Niceness value is out of the range 1-10");
        }
        return pos;
    }
    private void loadPropertyFiles() {
        for (int propertyFileIndex = 0;
             propertyFileIndex < propertyFiles.size();
             propertyFileIndex++) {
            String filename
                = (String) propertyFiles.elementAt(propertyFileIndex);
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(filename);
                props.load(fis);
            } catch (IOException e) {
                System.out.println("Could not load property file "
                                   + filename + ": " + e.getMessage());
            } finally {
                FileUtils.close(fis);
            }
            Enumeration propertyNames = props.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String name = (String) propertyNames.nextElement();
                if (definedProps.getProperty(name) == null) {
                    definedProps.put(name, props.getProperty(name));
                }
            }
        }
    }
    private File getParentFile(File file) {
        File parent = file.getParentFile();
        if (parent != null && msgOutputLevel >= Project.MSG_VERBOSE) {
            System.out.println("Searching in " + parent.getAbsolutePath());
        }
        return parent;
    }
    private File findBuildFile(String start, String suffix) {
        if (msgOutputLevel >= Project.MSG_INFO) {
            System.out.println("Searching for " + suffix + " ...");
        }
        File parent = new File(new File(start).getAbsolutePath());
        File file = new File(parent, suffix);
        while (!file.exists()) {
            parent = getParentFile(parent);
            if (parent == null) {
                return null;
            }
            file = new File(parent, suffix);
        }
        return file;
    }
    private void runBuild(ClassLoader coreLoader) throws BuildException {
        if (!readyToRun) {
            return;
        }
        final Project project = new Project();
        project.setCoreLoader(coreLoader);
        Throwable error = null;
        try {
            addBuildListeners(project);
            addInputHandler(project);
            PrintStream savedErr = System.err;
            PrintStream savedOut = System.out;
            InputStream savedIn = System.in;
            SecurityManager oldsm = null;
            oldsm = System.getSecurityManager();
            try {
                if (allowInput) {
                    project.setDefaultInputStream(System.in);
                }
                System.setIn(new DemuxInputStream(project));
                System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
                System.setErr(new PrintStream(new DemuxOutputStream(project, true)));
                if (!projectHelp) {
                    project.fireBuildStarted();
                }
                if (threadPriority != null) {
                    try {
                        project.log("Setting Ant's thread priority to "
                                + threadPriority, Project.MSG_VERBOSE);
                        Thread.currentThread().setPriority(threadPriority.intValue());
                    } catch (SecurityException swallowed) {
                        project.log("A security manager refused to set the -nice value");
                    }
                }
                project.init();
                PropertyHelper propertyHelper
                    = (PropertyHelper) PropertyHelper.getPropertyHelper(project);
                HashMap props = new HashMap(definedProps);
                new ResolvePropertyMap(project, propertyHelper,
                                       propertyHelper.getExpanders())
                    .resolveAllProperties(props, null, false);
                for (Iterator e = props.entrySet().iterator(); e.hasNext(); ) {
                    Map.Entry ent = (Map.Entry) e.next();
                    String arg = (String) ent.getKey();
                    Object value = ent.getValue();
                    project.setUserProperty(arg, String.valueOf(value));
                }
                project.setUserProperty(MagicNames.ANT_FILE,
                                        buildFile.getAbsolutePath());
                project.setUserProperty(MagicNames.ANT_FILE_TYPE,
                                        MagicNames.ANT_FILE_TYPE_FILE);
                project.setKeepGoingMode(keepGoingMode);
                if (proxy) {
                    ProxySetup proxySetup = new ProxySetup(project);
                    proxySetup.enableProxies();
                }
                ProjectHelper.configureProject(project, buildFile);
                if (projectHelp) {
                    printDescription(project);
                    printTargets(project, msgOutputLevel > Project.MSG_INFO,
                            msgOutputLevel > Project.MSG_VERBOSE);
                    return;
                }
                if (targets.size() == 0) {
                    if (project.getDefaultTarget() != null) {
                        targets.addElement(project.getDefaultTarget());
                    }
                }
                project.executeTargets(targets);
            } finally {
                if (oldsm != null) {
                    System.setSecurityManager(oldsm);
                }
                System.setOut(savedOut);
                System.setErr(savedErr);
                System.setIn(savedIn);
            }
        } catch (RuntimeException exc) {
            error = exc;
            throw exc;
        } catch (Error e) {
            error = e;
            throw e;
        } finally {
            if (!projectHelp) {
                try {
                    project.fireBuildFinished(error);
                } catch (Throwable t) {
                    System.err.println("Caught an exception while logging the"
                                       + " end of the build.  Exception was:");
                    t.printStackTrace();
                    if (error != null) {
                        System.err.println("There has been an error prior to"
                                           + " that:");
                        error.printStackTrace();
                    }
                    throw new BuildException(t);
                }
            } else if (error != null) {
                project.log(error.toString(), Project.MSG_ERR);
            }
        }
    }
    protected void addBuildListeners(Project project) {
        project.addBuildListener(createLogger());
        for (int i = 0; i < listeners.size(); i++) {
            String className = (String) listeners.elementAt(i);
            BuildListener listener =
                    (BuildListener) ClasspathUtils.newInstance(className,
                            Main.class.getClassLoader(), BuildListener.class);
            project.setProjectReference(listener);
            project.addBuildListener(listener);
        }
    }
    private void addInputHandler(Project project) throws BuildException {
        InputHandler handler = null;
        if (inputHandlerClassname == null) {
            handler = new DefaultInputHandler();
        } else {
            handler = (InputHandler) ClasspathUtils.newInstance(
                    inputHandlerClassname, Main.class.getClassLoader(),
                    InputHandler.class);
            project.setProjectReference(handler);
        }
        project.setInputHandler(handler);
    }
    private BuildLogger createLogger() {
        BuildLogger logger = null;
        if (loggerClassname != null) {
            try {
                logger = (BuildLogger) ClasspathUtils.newInstance(
                        loggerClassname, Main.class.getClassLoader(),
                        BuildLogger.class);
            } catch (BuildException e) {
                System.err.println("The specified logger class "
                    + loggerClassname
                    + " could not be used because " + e.getMessage());
                throw new RuntimeException();
            }
        } else {
            logger = new DefaultLogger();
        }
        logger.setMessageOutputLevel(msgOutputLevel);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        logger.setEmacsMode(emacsMode);
        return logger;
    }
    private static void printUsage() {
        String lSep = System.getProperty("line.separator");
        StringBuffer msg = new StringBuffer();
        msg.append("ant [options] [target [target2 [target3] ...]]" + lSep);
        msg.append("Options: " + lSep);
        msg.append("  -help, -h              print this message" + lSep);
        msg.append("  -projecthelp, -p       print project help information" + lSep);
        msg.append("  -version               print the version information and exit" + lSep);
        msg.append("  -diagnostics           print information that might be helpful to" + lSep);
        msg.append("                         diagnose or report problems." + lSep);
        msg.append("  -quiet, -q             be extra quiet" + lSep);
        msg.append("  -verbose, -v           be extra verbose" + lSep);
        msg.append("  -debug, -d             print debugging information" + lSep);
        msg.append("  -emacs, -e             produce logging information without adornments"
                   + lSep);
        msg.append("  -lib <path>            specifies a path to search for jars and classes"
                   + lSep);
        msg.append("  -logfile <file>        use given file for log" + lSep);
        msg.append("    -l     <file>                ''" + lSep);
        msg.append("  -logger <classname>    the class which is to perform logging" + lSep);
        msg.append("  -listener <classname>  add an instance of class as a project listener"
                   + lSep);
        msg.append("  -noinput               do not allow interactive input" + lSep);
        msg.append("  -buildfile <file>      use given buildfile" + lSep);
        msg.append("    -file    <file>              ''" + lSep);
        msg.append("    -f       <file>              ''" + lSep);
        msg.append("  -D<property>=<value>   use value for given property" + lSep);
        msg.append("  -keep-going, -k        execute all targets that do not depend" + lSep);
        msg.append("                         on failed target(s)" + lSep);
        msg.append("  -propertyfile <name>   load all properties from file with -D" + lSep);
        msg.append("                         properties taking precedence" + lSep);
        msg.append("  -inputhandler <class>  the class which will handle input requests" + lSep);
        msg.append("  -find <file>           (s)earch for buildfile towards the root of" + lSep);
        msg.append("    -s  <file>           the filesystem and use it" + lSep);
        msg.append("  -nice  number          A niceness value for the main thread:" + lSep
                   + "                         1 (lowest) to 10 (highest); 5 is the default"
                   + lSep);
        msg.append("  -nouserlib             Run ant without using the jar files from" + lSep
                   + "                         ${user.home}/.ant/lib" + lSep);
        msg.append("  -noclasspath           Run ant without using CLASSPATH" + lSep);
        msg.append("  -autoproxy             Java1.5+: use the OS proxy settings"
                + lSep);
        msg.append("  -main <class>          override Ant's normal entry point");
        System.out.println(msg.toString());
    }
    private static void printVersion(int logLevel) throws BuildException {
        System.out.println(getAntVersion());
    }
    private static String antVersion = null;
    public static synchronized String getAntVersion() throws BuildException {
        if (antVersion == null) {
            try {
                Properties props = new Properties();
                InputStream in =
                    Main.class.getResourceAsStream("/org/apache/tools/ant/version.txt");
                props.load(in);
                in.close();
                StringBuffer msg = new StringBuffer();
                msg.append("Apache Ant(TM) version ");
                msg.append(props.getProperty("VERSION"));
                msg.append(" compiled on ");
                msg.append(props.getProperty("DATE"));
                antVersion = msg.toString();
            } catch (IOException ioe) {
                throw new BuildException("Could not load the version information:"
                                         + ioe.getMessage());
            } catch (NullPointerException npe) {
                throw new BuildException("Could not load the version information.");
            }
        }
        return antVersion;
    }
    private static void printDescription(Project project) {
       if (project.getDescription() != null) {
          project.log(project.getDescription());
       }
    }
    private static Map removeDuplicateTargets(Map targets) {
        Map locationMap = new HashMap();
        for (Iterator i = targets.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            Target target = (Target) entry.getValue();
            Target otherTarget =
                (Target) locationMap.get(target.getLocation());
            if (otherTarget == null
                || otherTarget.getName().length() > name.length()) {
                locationMap.put(
                    target.getLocation(), target); 
            }
        }
        Map ret = new HashMap();
        for (Iterator i = locationMap.values().iterator(); i.hasNext();) {
            Target target = (Target) i.next();
            ret.put(target.getName(), target);
        }
        return ret;
    }
    private static void printTargets(Project project, boolean printSubTargets,
            boolean printDependencies) {
        int maxLength = 0;
        Map ptargets = removeDuplicateTargets(project.getTargets());
        String targetName;
        String targetDescription;
        Target currentTarget;
        Vector topNames = new Vector();
        Vector topDescriptions = new Vector();
        Vector topDependencies = new Vector();
        Vector subNames = new Vector();
        Vector subDependencies = new Vector();
        for (Iterator i = ptargets.values().iterator(); i.hasNext();) {
            currentTarget = (Target) i.next();
            targetName = currentTarget.getName();
            if (targetName.equals("")) {
                continue;
            }
            targetDescription = currentTarget.getDescription();
            if (targetDescription == null) {
                int pos = findTargetPosition(subNames, targetName);
                subNames.insertElementAt(targetName, pos);
                if (printDependencies) {
                    subDependencies.insertElementAt(currentTarget.getDependencies(), pos);
                }
            } else {
                int pos = findTargetPosition(topNames, targetName);
                topNames.insertElementAt(targetName, pos);
                topDescriptions.insertElementAt(targetDescription, pos);
                if (targetName.length() > maxLength) {
                    maxLength = targetName.length();
                }
                if (printDependencies) {
                    topDependencies.insertElementAt(currentTarget.getDependencies(), pos);
                }
            }
        }
        printTargets(project, topNames, topDescriptions, topDependencies,
                "Main targets:", maxLength);
        if (topNames.size() == 0) {
            printSubTargets = true;
        }
        if (printSubTargets) {
            printTargets(project, subNames, null, subDependencies, "Other targets:", 0);
        }
        String defaultTarget = project.getDefaultTarget();
        if (defaultTarget != null && !"".equals(defaultTarget)) {
            project.log("Default target: " + defaultTarget);
        }
    }
    private static int findTargetPosition(Vector names, String name) {
        int res = names.size();
        for (int i = 0; i < names.size() && res == names.size(); i++) {
            if (name.compareTo((String) names.elementAt(i)) < 0) {
                res = i;
            }
        }
        return res;
    }
    private static void printTargets(Project project, Vector names,
                                     Vector descriptions, Vector dependencies,
                                     String heading,
                                     int maxlen) {
        String lSep = System.getProperty("line.separator");
        String spaces = "    ";
        while (spaces.length() <= maxlen) {
            spaces += spaces;
        }
        StringBuffer msg = new StringBuffer();
        msg.append(heading + lSep + lSep);
        for (int i = 0; i < names.size(); i++) {
            msg.append(" ");
            msg.append(names.elementAt(i));
            if (descriptions != null) {
                msg.append(
                    spaces.substring(0, maxlen - ((String) names.elementAt(i)).length() + 2));
                msg.append(descriptions.elementAt(i));
            }
            msg.append(lSep);
            if (!dependencies.isEmpty()) {
                Enumeration deps = (Enumeration) dependencies.elementAt(i);
                if (deps.hasMoreElements()) {
                    msg.append("   depends on: ");
                    while (deps.hasMoreElements()) {
                        msg.append(deps.nextElement());
                        if (deps.hasMoreElements()) {
                            msg.append(", ");
                        }
                    }
                    msg.append(lSep);                
                }
            }
        }
        project.log(msg.toString(), Project.MSG_WARN);
    }
}