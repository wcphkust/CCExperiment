package org.apache.maven.lifecycle.internal;
import junit.framework.TestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
public class ProjectBuildListTest
    extends TestCase
{
    public void testGetByTaskSegment()
        throws Exception
    {
        final MavenSession session = ProjectDependencyGraphStub.getMavenSession();
        ProjectBuildList projectBuildList = ProjectDependencyGraphStub.getProjectBuildList( session );
        TaskSegment taskSegment = projectBuildList.get( 0 ).getTaskSegment();
        assertTrue( "This test assumes there are at least 6 elements in projectBuilds", projectBuildList.size() >= 6 );
        final ProjectBuildList byTaskSegment = projectBuildList.getByTaskSegment( taskSegment );
        assertEquals( projectBuildList.size(),
                      byTaskSegment.size() ); 
    }
}
