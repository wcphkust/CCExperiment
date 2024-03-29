package org.apache.tools.ant.util;
import java.io.File;
import junit.framework.TestCase;
public class LoaderUtilsTest extends TestCase {
    public LoaderUtilsTest(String name) {
        super(name);
    }
    public void testGetXyzSource() {
        File f1 = LoaderUtils.getClassSource(LoaderUtils.class);
        assertNotNull(f1);
        File f2 = LoaderUtils.getResourceSource(null,
                                                "org/apache/tools/ant/taskdefs/defaults.properties");
        assertNotNull(f2);
        assertEquals(f1.getAbsolutePath(), f2.getAbsolutePath());
    }
}
