package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileNameMapper;
public class MultiMapTest extends BuildFileTest {
    public MultiMapTest(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/multimap.xml");
    }
    public void tearDown() {
        executeTarget("cleanup");
    }
    public void testMultiCopy() {
        executeTarget("multicopy");
    }
    public void testMultiMove() {
        executeTarget("multimove");
    }
    public void testSingleCopy() {
        executeTarget("singlecopy");
    }
    public void testSingleMove() {
        executeTarget("singlemove");
    }
    public void testCopyWithEmpty() {
        executeTarget("copywithempty");
    }
    public void testMoveWithEmpty() {
        executeTarget("movewithempty");
    }
    public static class TestMapper implements FileNameMapper {
        public TestMapper() {}
        public void setFrom(String from) {}
        public void setTo(String to) {}
        public String[] mapFileName(final String source_file_name) {
            return new String[] {
                source_file_name, source_file_name+".copy2" };
        }
    }
}
