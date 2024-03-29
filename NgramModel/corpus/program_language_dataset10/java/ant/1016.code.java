package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;
public class UntarTest extends BuildFileTest {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    public UntarTest(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/untar.xml");
    }
    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testRealTest() throws java.io.IOException {
        testLogoExtraction("realTest");
    }
    public void testRealGzipTest() throws java.io.IOException {
        testLogoExtraction("realGzipTest");
    }
    public void testRealBzip2Test() throws java.io.IOException {
        testLogoExtraction("realBzip2Test");
    }
    public void testTestTarTask() throws java.io.IOException {
        testLogoExtraction("testTarTask");
    }
    public void testTestGzipTarTask() throws java.io.IOException {
        testLogoExtraction("testGzipTarTask");
    }
    public void testTestBzip2TarTask() throws java.io.IOException {
        testLogoExtraction("testBzip2TarTask");
    }
    public void testSrcDirTest() {
        expectBuildException("srcDirTest", "Src cannot be a directory.");
    }
    public void testEncoding() {
        expectSpecificBuildException("encoding",
                                     "<untar> overrides setEncoding.",
                                     "The untar task doesn't support the "
                                     + "encoding attribute");
    }
    public void testResourceCollection() throws java.io.IOException {
        testLogoExtraction("resourceCollection");
    }
    private void testLogoExtraction(String target) throws java.io.IOException {
        executeTarget(target);
        assertTrue(FILE_UTILS.contentEquals(project.resolveFile("../asf-logo.gif"),
                                           project.resolveFile("asf-logo.gif")));
    }
    public void testDocumentationClaimsOnCopy() {
        executeTarget("testDocumentationClaimsOnCopy");
        assertFalse(getProject().resolveFile("untartestout/1/foo").exists());
        assertTrue(getProject().resolveFile("untartestout/2/bar").exists());
    }
}
