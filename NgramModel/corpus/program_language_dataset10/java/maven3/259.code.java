package org.apache.maven;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
@Component(role = ProjectDependenciesResolver.class)
public class DefaultProjectDependenciesResolver
    implements ProjectDependenciesResolver
{
    @Requirement
    private RepositorySystem repositorySystem;
    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;
    public Set<Artifact> resolve( MavenProject project, Collection<String> scopesToResolve, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolve( Collections.singleton( project ), scopesToResolve, session );
    }
    public Set<Artifact> resolve( MavenProject project, Collection<String> scopesToCollect,
                                  Collection<String> scopesToResolve, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<MavenProject> mavenProjects = Collections.singleton( project );
        return resolveImpl( mavenProjects, scopesToCollect, scopesToResolve, session,
                            getIgnorableArtifacts( mavenProjects ) );
    }
    public Set<Artifact> resolve( Collection<? extends MavenProject> projects, Collection<String> scopesToResolve,
                                  MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveImpl( projects, null, scopesToResolve, session, getIgnorableArtifacts( projects ) );
    }
    public Set<Artifact> resolve( MavenProject project, Collection<String> scopesToCollect,
                                  Collection<String> scopesToResolve, MavenSession session,
                                  Set<Artifact> ignoreableArtifacts )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveImpl( Collections.singleton( project ), scopesToCollect, scopesToResolve, session,
                            getIgnorableArtifacts( ignoreableArtifacts ) );
    }
    private Set<Artifact> resolveImpl( Collection<? extends MavenProject> projects, Collection<String> scopesToCollect,
                                       Collection<String> scopesToResolve, MavenSession session,
                                       Set<String> projectIds )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<Artifact> resolved = new LinkedHashSet<Artifact>();
        if ( projects == null || projects.isEmpty() )
        {
            return resolved;
        }
        if ( ( scopesToCollect == null || scopesToCollect.isEmpty() ) &&
            ( scopesToResolve == null || scopesToResolve.isEmpty() ) )
        {
            return resolved;
        }
        CumulativeScopeArtifactFilter resolutionScopeFilter = new CumulativeScopeArtifactFilter( scopesToResolve );
        CumulativeScopeArtifactFilter collectionScopeFilter = new CumulativeScopeArtifactFilter( scopesToCollect );
        collectionScopeFilter = new CumulativeScopeArtifactFilter( collectionScopeFilter, resolutionScopeFilter );
        ArtifactResolutionRequest request =
            new ArtifactResolutionRequest().setResolveRoot( false ).setResolveTransitively( true ).setCollectionFilter(
                collectionScopeFilter ).setResolutionFilter( resolutionScopeFilter ).setLocalRepository(
                session.getLocalRepository() ).setOffline( session.isOffline() ).setForceUpdate(
                session.getRequest().isUpdateSnapshots() );
        request.setServers( session.getRequest().getServers() );
        request.setMirrors( session.getRequest().getMirrors() );
        request.setProxies( session.getRequest().getProxies() );
        for ( MavenProject project : projects )
        {
            request.setArtifact( new ProjectArtifact( project ) );
            request.setArtifactDependencies( project.getDependencyArtifacts() );
            request.setManagedVersionMap( project.getManagedVersionMap() );
            request.setRemoteRepositories( project.getRemoteArtifactRepositories() );
            ArtifactResolutionResult result = repositorySystem.resolve( request );
            try
            {
                resolutionErrorHandler.throwErrors( request, result );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                Collection<Artifact> missing = new HashSet<Artifact>( e.getMissingArtifacts() );
                for ( Iterator<Artifact> it = missing.iterator(); it.hasNext(); )
                {
                    String key = ArtifactUtils.key( it.next() );
                    if ( projectIds.contains( key ) )
                    {
                        it.remove();
                    }
                }
                if ( !missing.isEmpty() )
                {
                    throw e;
                }
            }
            resolved.addAll( result.getArtifacts() );
        }
        return resolved;
    }
    private Set<String> getIgnorableArtifacts( Collection<? extends MavenProject> projects )
    {
        Set<String> projectIds = new HashSet<String>( projects.size() * 2 );
        for ( MavenProject p : projects )
        {
            String key = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );
            projectIds.add( key );
        }
        return projectIds;
    }
    private Set<String> getIgnorableArtifacts( Iterable<Artifact> artifactIterable )
    {
        Set<String> projectIds = new HashSet<String>();
        for ( Artifact artifact : artifactIterable )
        {
            String key = ArtifactUtils.key( artifact );
            projectIds.add( key );
        }
        return projectIds;
    }
}
