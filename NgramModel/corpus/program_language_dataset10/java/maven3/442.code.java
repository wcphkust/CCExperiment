package org.apache.maven.plugin.internal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.FilterRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.AndDependencyFilter;
import org.sonatype.aether.util.filter.ExclusionsDependencyFilter;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
@Component( role = PluginDependenciesResolver.class )
public class DefaultPluginDependenciesResolver
    implements PluginDependenciesResolver
{
    private static final String REPOSITORY_CONTEXT = "plugin";
    @Requirement
    private Logger logger;
    @Requirement
    private ArtifactFilterManager artifactFilterManager;
    @Requirement
    private RepositorySystem repoSystem;
    private Artifact toArtifact( Plugin plugin, RepositorySystemSession session )
    {
        return new DefaultArtifact( plugin.getGroupId(), plugin.getArtifactId(), null, "jar", plugin.getVersion(),
                                    session.getArtifactTypeRegistry().get( "maven-plugin" ) );
    }
    public Artifact resolve( Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session )
        throws PluginResolutionException
    {
        Artifact pluginArtifact = toArtifact( plugin, session );
        try
        {
            RepositorySystemSession pluginSession = new FilterRepositorySystemSession( session )
            {
                @Override
                public boolean isIgnoreMissingArtifactDescriptor()
                {
                    return false;
                }
            };
            ArtifactDescriptorRequest request =
                new ArtifactDescriptorRequest( pluginArtifact, repositories, REPOSITORY_CONTEXT );
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor( pluginSession, request );
            pluginArtifact = result.getArtifact();
            String requiredMavenVersion = (String) result.getProperties().get( "prerequisites.maven" );
            if ( requiredMavenVersion != null )
            {
                Map<String, String> props = new LinkedHashMap<String, String>( pluginArtifact.getProperties() );
                props.put( "requiredMavenVersion", requiredMavenVersion );
                pluginArtifact = pluginArtifact.setProperties( props );
            }
        }
        catch ( ArtifactDescriptorException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
        try
        {
            ArtifactRequest request = new ArtifactRequest( pluginArtifact, repositories, REPOSITORY_CONTEXT );
            pluginArtifact = repoSystem.resolveArtifact( session, request ).getArtifact();
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
        return pluginArtifact;
    }
    public DependencyNode resolve( Plugin plugin, Artifact pluginArtifact, DependencyFilter dependencyFilter,
                                   List<RemoteRepository> repositories, RepositorySystemSession session )
        throws PluginResolutionException
    {
        if ( pluginArtifact == null )
        {
            pluginArtifact = toArtifact( plugin, session );
        }
        DependencyFilter collectionFilter = new ScopeDependencyFilter( "provided", "test" );
        DependencyFilter resolutionFilter =
            new ExclusionsDependencyFilter( artifactFilterManager.getCoreArtifactExcludes() );
        resolutionFilter = AndDependencyFilter.newInstance( resolutionFilter, dependencyFilter );
        resolutionFilter = new AndDependencyFilter( collectionFilter, resolutionFilter );
        DependencyNode node;
        try
        {
            DependencySelector selector =
                AndDependencySelector.newInstance( session.getDependencySelector(), new WagonExcluder() );
            DependencyGraphTransformer transformer =
                ChainedDependencyGraphTransformer.newInstance( session.getDependencyGraphTransformer(),
                                                               new PlexusUtilsInjector() );
            DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession( session );
            pluginSession.setDependencySelector( selector );
            pluginSession.setDependencyGraphTransformer( transformer );
            CollectRequest request = new CollectRequest();
            request.setRequestContext( REPOSITORY_CONTEXT );
            request.setRepositories( repositories );
            request.setRoot( new org.sonatype.aether.graph.Dependency( pluginArtifact, null ) );
            for ( Dependency dependency : plugin.getDependencies() )
            {
                org.sonatype.aether.graph.Dependency pluginDep =
                    RepositoryUtils.toDependency( dependency, session.getArtifactTypeRegistry() );
                if ( !JavaScopes.SYSTEM.equals( pluginDep.getScope() ) )
                {
                    pluginDep = pluginDep.setScope( JavaScopes.RUNTIME );
                }
                request.addDependency( pluginDep );
            }
            node = repoSystem.collectDependencies( pluginSession, request ).getRoot();
            if ( logger.isDebugEnabled() )
            {
                node.accept( new GraphLogger() );
            }
            repoSystem.resolveDependencies( session, node, resolutionFilter );
        }
        catch ( DependencyCollectionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }
        return node;
    }
    class GraphLogger
        implements DependencyVisitor
    {
        private String indent = "";
        public boolean visitEnter( DependencyNode node )
        {
            StringBuilder buffer = new StringBuilder( 128 );
            buffer.append( indent );
            org.sonatype.aether.graph.Dependency dep = node.getDependency();
            if ( dep != null )
            {
                org.sonatype.aether.artifact.Artifact art = dep.getArtifact();
                buffer.append( art );
                buffer.append( ':' ).append( dep.getScope() );
                if ( node.getPremanagedScope() != null && !node.getPremanagedScope().equals( dep.getScope() ) )
                {
                    buffer.append( " (scope managed from " ).append( node.getPremanagedScope() ).append( ")" );
                }
                if ( node.getPremanagedVersion() != null && !node.getPremanagedVersion().equals( art.getVersion() ) )
                {
                    buffer.append( " (version managed from " ).append( node.getPremanagedVersion() ).append( ")" );
                }
            }
            logger.debug( buffer.toString() );
            indent += "   ";
            return true;
        }
        public boolean visitLeave( DependencyNode node )
        {
            indent = indent.substring( 0, indent.length() - 3 );
            return true;
        }
    }
}