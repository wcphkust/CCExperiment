package org.apache.tools.ant.taskdefs.optional.depend;
public class ClassFileUtils {
    public static String convertSlashName(String name) {
        return name.replace('\\', '.').replace('/', '.');
    }
    public static String convertDotName(String dotName) {
        return dotName.replace('.', '/');
    }
}