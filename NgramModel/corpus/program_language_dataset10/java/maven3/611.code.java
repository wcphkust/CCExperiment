package org.apache.maven.repository;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.project.artifact.ArtifactWithDependencies;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.aether.RepositorySystemSession;
@Component( role = RepositorySystem.class )
public class TestRepositorySystem
    implements RepositorySystem
{
    @Requirement
    private ModelReader modelReader;
    public ArtifactRepository buildArtifactRepository( Repository repository )
        throws InvalidRepositoryException
    {
        return new MavenArtifactRepository( repository.getId(), repository.getUrl(), new DefaultRepositoryLayout(),
                                            new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );
    }
    public Artifact createArtifact( String groupId, String artifactId, String version, String packaging )
    {
        return createArtifact( groupId, artifactId, version, null, packaging );
    }
    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type )
    {
        return new DefaultArtifact( groupId, artifactId, version, scope, type, null, new TestArtifactHandler( type ) );
    }
    public ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        ArtifactRepositoryPolicy snapshots,
                                                        ArtifactRepositoryPolicy releases )
    {
        return new MavenArtifactRepository( id, url, repositoryLayout, snapshots, releases );
    }
    public Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                                  String classifier )
    {
        return new DefaultArtifact( groupId, artifactId, version, null, type, classifier,
                                    new TestArtifactHandler( type ) );
    }
    public ArtifactRepository createDefaultLocalRepository()
        throws InvalidRepositoryException
    {
        return createLocalRepository( new File( System.getProperty( "basedir", "" ), "target/local-repo" ).getAbsoluteFile() );
    }
    public ArtifactRepository createDefaultRemoteRepository()
        throws InvalidRepositoryException
    {
        return new MavenArtifactRepository( DEFAULT_REMOTE_REPO_ID, "file://"
            + new File( System.getProperty( "basedir", "" ), "src/test/remote-repo" ).toURI().getPath(),
                                            new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(),
                                            new ArtifactRepositoryPolicy() );
    }
    public Artifact createDependencyArtifact( Dependency dependency )
    {
        Artifact artifact =
            new DefaultArtifact( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                                 dependency.getScope(), dependency.getType(), dependency.getClassifier(),
                                 new TestArtifactHandler( dependency.getType() ) );
        if ( Artifact.SCOPE_SYSTEM.equals( dependency.getScope() ) )
        {
            artifact.setFile( new File( dependency.getSystemPath() ) );
            artifact.setResolved( true );
        }
        return artifact;
    }
    public ArtifactRepository createLocalRepository( File localRepository )
        throws InvalidRepositoryException
    {
        return new MavenArtifactRepository( DEFAULT_LOCAL_REPO_ID, "file://" + localRepository.toURI().getPath(),
                                            new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(),
                                            new ArtifactRepositoryPolicy() );
    }
    public Artifact createPluginArtifact( Plugin plugin )
    {
        return new DefaultArtifact( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null,
                                    "maven-plugin", null, new TestArtifactHandler( "maven-plugin", "jar" ) );
    }
    public Artifact createProjectArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, "pom" );
    }
    public List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories )
    {
        return repositories;
    }
    public Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors )
    {
        return null;
    }
    public void injectAuthentication( List<ArtifactRepository> repositories, List<Server> servers )
    {
    }
    public void injectMirror( List<ArtifactRepository> repositories, List<Mirror> mirrors )
    {
    }
    public void injectProxy( List<ArtifactRepository> repositories, List<Proxy> proxies )
    {
    }
    public void publish( ArtifactRepository repository, File source, String remotePath,
                         ArtifactTransferListener transferListener )
        throws ArtifactTransferFailedException
    {
    }
    public ArtifactResolutionResult resolve( ArtifactResolutionRequest request )
    {
        ArtifactResolutionResult result = new ArtifactResolutionResult();
        if ( request.isResolveRoot() )
        {
            try
            {
                resolve( request.getArtifact(), request );
                result.addArtifact( request.getArtifact() );
            }
            catch ( IOException e )
            {
                result.addMissingArtifact( request.getArtifact() );
            }
        }
        if ( request.isResolveTransitively() )
        {
            Map<String, Artifact> artifacts = new LinkedHashMap<String, Artifact>();
            if ( request.getArtifactDependencies() != null )
            {
                for ( Artifact artifact : request.getArtifactDependencies() )
                {
                    artifacts.put( artifact.getDependencyConflictId(), artifact );
                }
            }
            List<Dependency> dependencies = new ArrayList<Dependency>();
            if ( request.getArtifact() instanceof ArtifactWithDependencies )
            {
                dependencies = ( (ArtifactWithDependencies) request.getArtifact() ).getDependencies();
            }
            else
            {
                Artifact pomArtifact =
                    createProjectArtifact( request.getArtifact().getGroupId(), request.getArtifact().getArtifactId(),
                                           request.getArtifact().getVersion() );
                File pomFile =
                    new File( request.getLocalRepository().getBasedir(),
                              request.getLocalRepository().pathOf( pomArtifact ) );
                try
                {
                    Model model = modelReader.read( pomFile, null );
                    dependencies = model.getDependencies();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
            for ( Dependency dependency : dependencies )
            {
                Artifact artifact = createDependencyArtifact( dependency );
                if ( !artifacts.containsKey( artifact.getDependencyConflictId() ) )
                {
                    artifacts.put( artifact.getDependencyConflictId(), artifact );
                }
            }
            for ( Artifact artifact : artifacts.values() )
            {
                try
                {
                    resolve( artifact, request );
                    result.addArtifact( artifact );
                }
                catch ( IOException e )
                {
                    result.addMissingArtifact( artifact );
                }
            }
        }
        return result;
    }
    private void resolve( Artifact artifact, ArtifactResolutionRequest request )
        throws IOException
    {
        if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
        {
            return;
        }
        ArtifactRepository localRepo = request.getLocalRepository();
        File localFile = new File( localRepo.getBasedir(), localRepo.pathOf( artifact ) );
        artifact.setFile( localFile );
        if ( !localFile.exists() )
        {
            if ( request.getRemoteRepositories().isEmpty() )
            {
                throw new IOException( localFile + " does not exist and no remote repositories are configured" );
            }
            ArtifactRepository remoteRepo = request.getRemoteRepositories().get( 0 );
            File remoteFile = new File( remoteRepo.getBasedir(), remoteRepo.pathOf( artifact ) );
            FileUtils.copyFile( remoteFile, localFile );
        }
        artifact.setResolved( true );
    }
    public void retrieve( ArtifactRepository repository, File destination, String remotePath,
                          ArtifactTransferListener transferListener )
        throws ArtifactTransferFailedException, ArtifactDoesNotExistException
    {
    }
    public void injectMirror( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
    }
    public void injectProxy( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
    }
    public void injectAuthentication( RepositorySystemSession session, List<ArtifactRepository> repositories )
    {
    }
}
