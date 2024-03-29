package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;
import java.io.File;
import java.io.IOException;
public class MoveTest extends BuildFileTest {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    public MoveTest(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/move.xml");
    }
    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testFilterSet() throws IOException {
        executeTarget("testFilterSet");
        File tmp  = new File(getProjectDir(), "move.filterset.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(FILE_UTILS.contentEquals(tmp, check));
    }
    public void testFilterChain() throws IOException {
        executeTarget("testFilterChain");
        File tmp  = new File(getProjectDir(), "move.filterchain.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(FILE_UTILS.contentEquals(tmp, check));
    }
    public void testDirectoryRemoval() throws IOException {
        executeTarget("testDirectoryRemoval");
        assertTrue(!getProject().resolveFile("E/B/1").exists());
        assertTrue(getProject().resolveFile("E/C/2").exists());
        assertTrue(getProject().resolveFile("E/D/3").exists());
        assertTrue(getProject().resolveFile("A/B/1").exists());
        assertTrue(!getProject().resolveFile("A/C/2").exists());
        assertTrue(!getProject().resolveFile("A/D/3").exists());
        assertTrue(!getProject().resolveFile("A/C").exists());
        assertTrue(!getProject().resolveFile("A/D").exists());
    }
    public void testDirectoryRetaining() throws IOException {
        executeTarget("testDirectoryRetaining");
        assertTrue(getProject().resolveFile("E").exists());
        assertTrue(getProject().resolveFile("E/1").exists());
        assertTrue(!getProject().resolveFile("A/1").exists());
        assertTrue(getProject().resolveFile("A").exists());
    }
    public void testCompleteDirectoryMove() throws IOException {
        testCompleteDirectoryMove("testCompleteDirectoryMove");
    }
    public void testCompleteDirectoryMove2() throws IOException {
        testCompleteDirectoryMove("testCompleteDirectoryMove2");
    }
    private void testCompleteDirectoryMove(String target) throws IOException {
        executeTarget(target);
        assertTrue(getProject().resolveFile("E").exists());
        assertTrue(getProject().resolveFile("E/1").exists());
        assertTrue(!getProject().resolveFile("A/1").exists());
    }
    public void testPathElementMove() throws IOException {
        executeTarget("testPathElementMove");
        assertTrue(getProject().resolveFile("E").exists());
        assertTrue(getProject().resolveFile("E/1").exists());
        assertTrue(!getProject().resolveFile("A/1").exists());
        assertTrue(getProject().resolveFile("A").exists());
    }
    public void testMoveFileAndFileset() {
        executeTarget("testMoveFileAndFileset");
    }
    public void testCompleteDirectoryMoveToExistingDir() {
        executeTarget("testCompleteDirectoryMoveToExistingDir");
    }
    public void testCompleteDirectoryMoveFileToFile() {
        executeTarget("testCompleteDirectoryMoveFileToFile");
    }
    public void testCompleteDirectoryMoveFileToDir() {
        executeTarget("testCompleteDirectoryMoveFileToDir");
    }
    public void testCompleteDirectoryMoveFileAndFileset() {
        executeTarget("testCompleteDirectoryMoveFileAndFileset");
    }
    public void testCompleteDirectoryMoveFileToExistingFile() {
        executeTarget("testCompleteDirectoryMoveFileToExistingFile");
    }
    public void testCompleteDirectoryMoveFileToExistingDir() {
        executeTarget("testCompleteDirectoryMoveFileToExistingDir");
    }
    public void testCompleteDirectoryMoveFileToDirWithExistingFile() {
        executeTarget("testCompleteDirectoryMoveFileToDirWithExistingFile");
    }
    public void testCompleteDirectoryMoveFileToDirWithExistingDir() {
        executeTarget("testCompleteDirectoryMoveFileToDirWithExistingDir");
    }
}
