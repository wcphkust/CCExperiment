package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.bzip2.CBZip2InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
public class BZip2Test extends BuildFileTest {
    public BZip2Test(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/bzip2.xml");
        executeTarget("prepare");
    }
    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testRealTest() throws IOException {
        executeTarget("realTest");
        File originalFile =
            project.resolveFile("expected/asf-logo-huge.tar.bz2");
        File actualFile   = project.resolveFile("asf-logo-huge.tar.bz2");
        InputStream originalIn =
            new BufferedInputStream(new FileInputStream(originalFile));
        assertEquals((byte) 'B', originalIn.read());
        assertEquals((byte) 'Z', originalIn.read());
        InputStream actualIn =
            new BufferedInputStream(new FileInputStream(actualFile));
        assertEquals((byte) 'B', actualIn.read());
        assertEquals((byte) 'Z', actualIn.read());
        originalIn = new CBZip2InputStream(originalIn);
        actualIn   = new CBZip2InputStream(actualIn);
        while (true) {
            int expected = originalIn.read();
            int actual   = actualIn.read();
            if (expected >= 0) {
                if (expected != actual) {
                    fail("File content mismatch");
                }
            } else {
                if (actual >= 0) {
                    fail("File content mismatch");
                }
                break;
            }
        }
        originalIn.close();
        actualIn.close();
    }
    public void testResource(){
        executeTarget("realTestWithResource");
    }
    public void testDateCheck(){
        executeTarget("testDateCheck");
        String log = getLog();
        assertTrue(
            "Expecting message ending with 'asf-logo.gif.bz2 is up to date.' but got '" + log + "'",
            log.endsWith("asf-logo.gif.bz2 is up to date."));
    }
}
