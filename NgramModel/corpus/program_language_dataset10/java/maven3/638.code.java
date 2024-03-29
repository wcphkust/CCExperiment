package org.plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.MavenProject;
public class TestPlugin
    implements Mojo
{
    private Log log;
    private MavenProjectHelper mavenProjectHelper;
    private MavenProject project;
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        mavenProjectHelper.attachArtifact( project, "pom", "classifier", project.getFile() );
        mavenProjectHelper.attachArtifact( project, "pom", "classifier", project.getFile() );
    }
    public Log getLog()
    {
        return log;
    }
    public void setLog( Log log )
    {
        this.log = log;
    }
}
