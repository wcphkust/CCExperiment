package org.apache.tools.ant.taskdefs.optional.native2ascii;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
public class Native2AsciiAdapterFactory {
    public static String getDefault() {
        if (JavaEnvUtils.isKaffe()) {
            return KaffeNative2Ascii.IMPLEMENTATION_NAME;
        }
        return SunNative2Ascii.IMPLEMENTATION_NAME;
    }
    public static Native2AsciiAdapter getAdapter(String choice,
                                                 ProjectComponent log)
        throws BuildException {
        return getAdapter(choice, log, null);
    }
    public static Native2AsciiAdapter getAdapter(String choice,
                                                 ProjectComponent log,
                                                 Path classpath)
        throws BuildException {
        if ((JavaEnvUtils.isKaffe() && choice == null)
            || KaffeNative2Ascii.IMPLEMENTATION_NAME.equals(choice)) {
            return new KaffeNative2Ascii();
        } else if (SunNative2Ascii.IMPLEMENTATION_NAME.equals(choice)) {
            return new SunNative2Ascii();
        } else if (choice != null) {
            return resolveClassName(choice,
                                    log.getProject()
                                    .createClassLoader(classpath));
        }
        return new SunNative2Ascii();
    }
    private static Native2AsciiAdapter resolveClassName(String className,
                                                        ClassLoader loader)
        throws BuildException {
        return (Native2AsciiAdapter) ClasspathUtils.newInstance(className,
            loader != null ? loader :
            Native2AsciiAdapterFactory.class.getClassLoader(),
            Native2AsciiAdapter.class);
    }
}
