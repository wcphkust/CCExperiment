package org.apache.tools.ant.filters;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.ResourceUtils;
public final class ClassConstants
    extends BaseFilterReader
    implements ChainableReader {
    private String queuedData = null;
    private static final String JAVA_CLASS_HELPER =
        "org.apache.tools.ant.filters.util.JavaClassHelper";
    public ClassConstants() {
        super();
    }
    public ClassConstants(final Reader in) {
        super(in);
    }
    public int read() throws IOException {
        int ch = -1;
        if (queuedData != null && queuedData.length() == 0) {
            queuedData = null;
        }
        if (queuedData != null) {
            ch = queuedData.charAt(0);
            queuedData = queuedData.substring(1);
            if (queuedData.length() == 0) {
                queuedData = null;
            }
        } else {
            final String clazz = readFully();
            if (clazz == null || clazz.length() == 0) {
                ch = -1;
            } else {
                final byte[] bytes = clazz.getBytes(ResourceUtils.ISO_8859_1);
                try {
                    final Class javaClassHelper =
                        Class.forName(JAVA_CLASS_HELPER);
                    if (javaClassHelper != null) {
                        final Class[] params = {
                            byte[].class
                        };
                        final Method getConstants =
                            javaClassHelper.getMethod("getConstants", params);
                        final Object[] args = {
                            bytes
                        };
                        final StringBuffer sb = (StringBuffer)
                                getConstants.invoke(null, args);
                        if (sb.length() > 0) {
                            queuedData = sb.toString();
                            return read();
                        }
                    }
                } catch (NoClassDefFoundError ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (InvocationTargetException ex) {
                    Throwable t = ex.getTargetException();
                    if (t instanceof NoClassDefFoundError) {
                        throw (NoClassDefFoundError) t;
                    }
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new BuildException(t);
                } catch (Exception ex) {
                    throw new BuildException(ex);
                }
            }
        }
        return ch;
    }
    public Reader chain(final Reader rdr) {
        ClassConstants newFilter = new ClassConstants(rdr);
        return newFilter;
    }
}
