package org.apache.maven.project.inheritance.t12scm;
import java.io.File;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
public class ProjectInheritanceTest
    extends AbstractProjectInheritanceTestCase
{
    public void testScmInfoCalculatedCorrectlyOnParentAndChildRead()
        throws Exception
    {
        File localRepo = getLocalRepositoryPath();
        File pom0 = new File( localRepo, "p0/pom.xml" );
        File pom0Basedir = pom0.getParentFile();
        File pom1 = new File( pom0Basedir, "modules/p1/pom.xml" );
        MavenProject project0 = getProject( pom0 );
        MavenProject project1 = getProject( pom1 );
        System.out.println( "\n\n" );
        System.out.println( "Parent SCM URL is: " + project0.getScm().getUrl() );
        System.out.println( "Child SCM URL is: " + project1.getScm().getUrl() );
        System.out.println();
        System.out.println( "Parent SCM connection is: " + project0.getScm().getConnection() );
        System.out.println( "Child SCM connection is: " + project1.getScm().getConnection() );
        System.out.println();
        System.out.println( "Parent SCM developer connection is: "
                            + project0.getScm().getDeveloperConnection() );
        System.out.println( "Child SCM developer connection is: "
                            + project1.getScm().getDeveloperConnection() );
        assertEquals( project1.getScm().getUrl(), project0.getScm().getUrl() + "/modules/p1" );
        assertEquals( project1.getScm().getConnection(), project0.getScm().getConnection()
                                                         + "/modules/p1" );
        assertEquals( project1.getScm().getDeveloperConnection(), project0.getScm()
                                                                          .getDeveloperConnection()
                                                                  + "/modules/p1" );
    }
    public void testScmInfoCalculatedCorrectlyOnChildOnlyRead()
        throws Exception
    {
        File localRepo = getLocalRepositoryPath();
        File pom1 = new File( localRepo, "p0/modules/p1/pom.xml" );
        MavenProject project1 = getProject( pom1 );
        System.out.println( "\n\n" );
        System.out.println( "Child SCM URL is: " + project1.getScm().getUrl() );
        System.out.println( "Child SCM connection is: " + project1.getScm().getConnection() );
        System.out.println( "Child SCM developer connection is: "
                            + project1.getScm().getDeveloperConnection() );
        assertEquals( "http://host/viewer?path=/p0/modules/p1", project1.getScm().getUrl() );
        assertEquals( "scm:svn:http://host/p0/modules/p1", project1.getScm().getConnection() );
        assertEquals( "scm:svn:https://host/p0/modules/p1", project1.getScm().getDeveloperConnection() );
    }
}
