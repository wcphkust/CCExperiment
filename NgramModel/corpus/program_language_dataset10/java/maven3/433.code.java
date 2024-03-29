package org.apache.maven.plugin;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import java.util.Map;
@Deprecated
public interface PluginManager
{
    String ROLE = PluginManager.class.getName();
    void executeMojo( MavenProject project, MojoExecution execution, MavenSession session )
        throws MojoExecutionException, ArtifactResolutionException, MojoFailureException, ArtifactNotFoundException,
        InvalidDependencyVersionException, PluginManagerException, PluginConfigurationException;
    PluginDescriptor getPluginDescriptorForPrefix( String prefix );
    Plugin getPluginDefinitionForPrefix( String prefix, MavenSession session, MavenProject project );
    PluginDescriptor verifyPlugin( Plugin plugin, MavenProject project, Settings settings,
                                   ArtifactRepository localRepository )
        throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
        InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException, PluginNotFoundException,
        PluginVersionNotFoundException;
    Object getPluginComponent( Plugin plugin, String role, String roleHint )
        throws PluginManagerException, ComponentLookupException;
    Map getPluginComponents( Plugin plugin, String role )
        throws ComponentLookupException, PluginManagerException;
    PluginDescriptor loadPluginDescriptor( Plugin plugin, MavenProject project, MavenSession session )
        throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
        InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException, PluginNotFoundException,
        PluginVersionNotFoundException;
    PluginDescriptor loadPluginFully( Plugin plugin, MavenProject project, MavenSession session )
        throws ArtifactResolutionException, PluginVersionResolutionException, ArtifactNotFoundException,
        InvalidVersionSpecificationException, InvalidPluginException, PluginManagerException, PluginNotFoundException,
        PluginVersionNotFoundException;
}
