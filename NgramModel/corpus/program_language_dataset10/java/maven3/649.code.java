package org.plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
public class TestPlugin
    implements Mojo
{
    private Log log;
    private String requiredParam;
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
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
