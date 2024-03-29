package org.apache.maven.plugin;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import java.util.List;
public interface PluginMappingManager
{
    Plugin getByPrefix( String pluginPrefix, List groupIds, List pluginRepositories,
                        ArtifactRepository localRepository );
}
