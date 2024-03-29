package org.apache.tools.ant.filters;
import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;
public class EscapeUnicodeTest extends BuildFileTest {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    public EscapeUnicodeTest(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/filters/build.xml");
    }
    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testEscapeUnicode() throws IOException {
        executeTarget("testEscapeUnicode");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(), "expected/escapeunicode.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(), "result/escapeunicode.test");
        assertTrue(FILE_UTILS.contentEquals(expected, result));
    }
}
