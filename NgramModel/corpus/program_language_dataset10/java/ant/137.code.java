package org.apache.tools.ant.launch;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;
import java.io.FilenameFilter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;
public final class Locator {
    private static final int NIBBLE = 4;
    private static final int NIBBLE_MASK   = 0xF;
    private static final int ASCII_SIZE = 128;
    private static final int BYTE_SIZE = 256;
    private static final int WORD = 16;
    private static final int SPACE = 0x20;
    private static final int DEL = 0x7F;
    public static final String URI_ENCODING = "UTF-8";
    private static boolean[] gNeedEscaping = new boolean[ASCII_SIZE];
    private static char[] gAfterEscaping1 = new char[ASCII_SIZE];
    private static char[] gAfterEscaping2 = new char[ASCII_SIZE];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static final String ERROR_NOT_FILE_URI
        = "Can only handle valid file: URIs, not ";
    static {
        for (int i = 0; i < SPACE; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> NIBBLE];
            gAfterEscaping2[i] = gHexChs[i & NIBBLE_MASK];
        }
        gNeedEscaping[DEL] = true;
        gAfterEscaping1[DEL] = '7';
        gAfterEscaping2[DEL] = 'F';
        char[] escChs = {' ', '<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> NIBBLE];
            gAfterEscaping2[ch] = gHexChs[ch & NIBBLE_MASK];
        }
    }
    private Locator() {
    }
    public static File getClassSource(Class c) {
        String classResource = c.getName().replace('.', '/') + ".class";
        return getResourceSource(c.getClassLoader(), classResource);
    }
    public static File getResourceSource(ClassLoader c, String resource) {
        if (c == null) {
            c = Locator.class.getClassLoader();
        }
        URL url = null;
        if (c == null) {
            url = ClassLoader.getSystemResource(resource);
        } else {
            url = c.getResource(resource);
        }
        if (url != null) {
            String u = url.toString();
            try {
                if (u.startsWith("jar:file:")) {
                    return new File(fromJarURI(u));
                } else if (u.startsWith("file:")) {
                    int tail = u.indexOf(resource);
                    String dirName = u.substring(0, tail);
                    return new File(fromURI(dirName));
                }
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
    public static String fromURI(String uri) {
        return fromURIJava13(uri);
    }
    private static String fromURIJava13(String uri) {
        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException emYouEarlEx) {
        }
        if (url == null || !("file".equals(url.getProtocol()))) {
            throw new IllegalArgumentException(ERROR_NOT_FILE_URI + uri);
        }
        StringBuffer buf = new StringBuffer(url.getHost());
        if (buf.length() > 0) {
            buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
        }
        String file = url.getFile();
        int queryPos = file.indexOf('?');
        buf.append((queryPos < 0) ? file : file.substring(0, queryPos));
        uri = buf.toString().replace('/', File.separatorChar);
        if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2
            && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(':') > -1) {
            uri = uri.substring(1);
        }
        String path = null;
        try {
            path = decodeUri(uri);
            String cwd = System.getProperty("user.dir");
            int posi = cwd.indexOf(':');
            boolean pathStartsWithFileSeparator = path.startsWith(File.separator);
            boolean pathStartsWithUNC = path.startsWith("" + File.separator + File.separator);
            if ((posi > 0) && pathStartsWithFileSeparator && !pathStartsWithUNC) {
                path = cwd.substring(0, posi + 1) + path;
            }
        } catch (UnsupportedEncodingException exc) {
            throw new IllegalStateException(
                "Could not convert URI " + uri + " to path: "
                + exc.getMessage());
        }
        return path;
    }
    public static String fromJarURI(String uri) {
        int pling = uri.indexOf("!/");
        String jarName = uri.substring("jar:".length(), pling);
        return fromURI(jarName);
    }
    public static String decodeUri(String uri) throws UnsupportedEncodingException {
        if (uri.indexOf('%') == -1) {
            return uri;
        }
        ByteArrayOutputStream sb = new ByteArrayOutputStream(uri.length());
        CharacterIterator iter = new StringCharacterIterator(uri);
        for (char c = iter.first(); c != CharacterIterator.DONE;
             c = iter.next()) {
            if (c == '%') {
                char c1 = iter.next();
                if (c1 != CharacterIterator.DONE) {
                    int i1 = Character.digit(c1, WORD);
                    char c2 = iter.next();
                    if (c2 != CharacterIterator.DONE) {
                        int i2 = Character.digit(c2, WORD);
                        sb.write((char) ((i1 << NIBBLE) + i2));
                    }
                }
            } else if (c >= 0x0000 && c < 0x0080) {
                sb.write(c);
            } else { 
                byte[] bytes = String.valueOf(c).getBytes(URI_ENCODING);
                sb.write(bytes, 0, bytes.length);
            }
        }
        return sb.toString(URI_ENCODING);
    }
    public static String encodeURI(String path) throws UnsupportedEncodingException {
        int i = 0;
        int len = path.length();
        int ch = 0;
        StringBuffer sb = null;
        for (; i < len; i++) {
            ch = path.charAt(i);
            if (ch >= ASCII_SIZE) {
                break;
            }
            if (gNeedEscaping[ch]) {
                if (sb == null) {
                    sb = new StringBuffer(path.substring(0, i));
                }
                sb.append('%');
                sb.append(gAfterEscaping1[ch]);
                sb.append(gAfterEscaping2[ch]);
            } else if (sb != null) {
                sb.append((char) ch);
            }
        }
        if (i < len) {
            if (sb == null) {
                sb = new StringBuffer(path.substring(0, i));
            }
            byte[] bytes = null;
            byte b;
            bytes = path.substring(i).getBytes(URI_ENCODING);
            len = bytes.length;
            for (i = 0; i < len; i++) {
                b = bytes[i];
                if (b < 0) {
                    ch = b + BYTE_SIZE;
                    sb.append('%');
                    sb.append(gHexChs[ch >> NIBBLE]);
                    sb.append(gHexChs[ch & NIBBLE_MASK]);
                } else if (gNeedEscaping[b]) {
                    sb.append('%');
                    sb.append(gAfterEscaping1[b]);
                    sb.append(gAfterEscaping2[b]);
                } else {
                    sb.append((char) b);
                }
            }
        }
        return sb == null ? path : sb.toString();
    }
    public static URL fileToURL(File file)
        throws MalformedURLException {
        try {
            return new URL(encodeURI(file.toURL().toString()));
        } catch (UnsupportedEncodingException ex) {
            throw new MalformedURLException(ex.toString());
        }
    }
    public static File getToolsJar() {
        boolean toolsJarAvailable = false;
        try {
            Class.forName("com.sun.tools.javac.Main");
            toolsJarAvailable = true;
        } catch (Exception e) {
            try {
                Class.forName("sun.tools.javac.Main");
                toolsJarAvailable = true;
            } catch (Exception e2) {
            }
        }
        if (toolsJarAvailable) {
            return null;
        }
        String libToolsJar
            = File.separator + "lib" + File.separator + "tools.jar";
        String javaHome = System.getProperty("java.home");
        File toolsJar = new File(javaHome + libToolsJar);
        if (toolsJar.exists()) {
            return toolsJar;
        }
        if (javaHome.toLowerCase(Locale.ENGLISH).endsWith(File.separator + "jre")) {
            javaHome = javaHome.substring(
                0, javaHome.length() - "/jre".length());
            toolsJar = new File(javaHome + libToolsJar);
        }
        if (!toolsJar.exists()) {
            System.out.println("Unable to locate tools.jar. "
                 + "Expected to find it in " + toolsJar.getPath());
            return null;
        }
        return toolsJar;
    }
    public static URL[] getLocationURLs(File location)
         throws MalformedURLException {
        return getLocationURLs(location, new String[]{".jar"});
    }
    public static URL[] getLocationURLs(File location,
                                        final String[] extensions)
         throws MalformedURLException {
        URL[] urls = new URL[0];
        if (!location.exists()) {
            return urls;
        }
        if (!location.isDirectory()) {
            urls = new URL[1];
            String path = location.getPath();
            String littlePath = path.toLowerCase(Locale.ENGLISH);
            for (int i = 0; i < extensions.length; ++i) {
                if (littlePath.endsWith(extensions[i])) {
                    urls[0] = fileToURL(location);
                    break;
                }
            }
            return urls;
        }
        File[] matches = location.listFiles(
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    String littleName = name.toLowerCase(Locale.ENGLISH);
                    for (int i = 0; i < extensions.length; ++i) {
                        if (littleName.endsWith(extensions[i])) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        urls = new URL[matches.length];
        for (int i = 0; i < matches.length; ++i) {
            urls[i] = fileToURL(matches[i]);
        }
        return urls;
    }
}
