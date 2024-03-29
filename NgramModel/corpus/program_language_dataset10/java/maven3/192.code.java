package org.apache.maven.artifact.resolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
public class ArtifactResolverTest
    extends AbstractArtifactComponentTestCase
{
    private DefaultArtifactResolver artifactResolver;
    private Artifact projectArtifact;
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        artifactResolver = (DefaultArtifactResolver) lookup( ArtifactResolver.class );
        projectArtifact = createLocalArtifact( "project", "3.0" );
    }
    @Override
    protected void tearDown()
        throws Exception
    {
        artifactFactory = null;
        projectArtifact = null;
        super.tearDown();
    }
    @Override
    protected String component()
    {
        return "resolver";
    }
    public void testResolutionOfASingleArtifactWhereTheArtifactIsPresentInTheLocalRepository()
        throws Exception
    {
        Artifact a = createLocalArtifact( "a", "1.0" );
        artifactResolver.resolve( a, remoteRepositories(), localRepository() );
        assertLocalArtifactPresent( a );
    }
    public void testResolutionOfASingleArtifactWhereTheArtifactIsNotPresentLocallyAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Artifact b = createRemoteArtifact( "b", "1.0-SNAPSHOT" );
        deleteLocalArtifact( b );
        artifactResolver.resolve( b, remoteRepositories(), localRepository() );
        assertLocalArtifactPresent( b );
    }
    @Override
    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
        throws Exception
    {
        return super.createArtifact( groupId, artifactId, version, type );
    }
    public void testTransitiveResolutionWhereAllArtifactsArePresentInTheLocalRepository()
        throws Exception
    {
        Artifact g = createLocalArtifact( "g", "1.0" );
        Artifact h = createLocalArtifact( "h", "1.0" );
        ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( g ), projectArtifact, remoteRepositories(), localRepository(), null );
        printErrors( result );
        assertEquals( 2, result.getArtifacts().size() );
        assertTrue( result.getArtifacts().contains( g ) );
        assertTrue( result.getArtifacts().contains( h ) );
        assertLocalArtifactPresent( g );
        assertLocalArtifactPresent( h );
    }
    public void testTransitiveResolutionWhereAllArtifactsAreNotPresentInTheLocalRepositoryAndMustBeRetrievedFromTheRemoteRepository()
        throws Exception
    {
        Artifact i = createRemoteArtifact( "i", "1.0-SNAPSHOT" );
        deleteLocalArtifact( i );
        Artifact j = createRemoteArtifact( "j", "1.0-SNAPSHOT" );
        deleteLocalArtifact( j );
        ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( i ), projectArtifact, remoteRepositories(), localRepository(), null );
        printErrors( result );
        assertEquals( 2, result.getArtifacts().size() );
        assertTrue( result.getArtifacts().contains( i ) );
        assertTrue( result.getArtifacts().contains( j ) );
        assertLocalArtifactPresent( i );
        assertLocalArtifactPresent( j );
    }
    public void testResolutionFailureWhenArtifactNotPresentInRemoteRepository()
        throws Exception
    {
        Artifact k = createArtifact( "k", "1.0" );
        try
        {
            artifactResolver.resolve( k, remoteRepositories(), localRepository() );
            fail( "Resolution succeeded when it should have failed" );
        }
        catch ( ArtifactNotFoundException expected )
        {
            assertTrue( true );
        }
    }
    public void testResolutionOfAnArtifactWhereOneRemoteRepositoryIsBadButOneIsGood()
        throws Exception
    {
        Artifact l = createRemoteArtifact( "l", "1.0-SNAPSHOT" );
        deleteLocalArtifact( l );
        List<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();
        repositories.add( remoteRepository() );
        repositories.add( badRemoteRepository() );
        artifactResolver.resolve( l, repositories, localRepository() );
        assertLocalArtifactPresent( l );
    }
    public void testTransitiveResolutionOrder()
        throws Exception
    {
        Artifact m = createLocalArtifact( "m", "1.0" );
        Artifact n = createLocalArtifact( "n", "1.0" );
        ArtifactMetadataSource mds = new ArtifactMetadataSource()
        {
            public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                             List<ArtifactRepository> remoteRepositories )
                throws ArtifactMetadataRetrievalException
            {
                Set dependencies = new HashSet();
                return new ResolutionGroup( artifact, dependencies, remoteRepositories );
            }
            public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact,
                                                                    ArtifactRepository localRepository,
                                                                    List<ArtifactRepository> remoteRepositories )
                throws ArtifactMetadataRetrievalException
            {
                throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
            }
            public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
                                                                                            Artifact artifact,
                                                                                            ArtifactRepository localRepository,
                                                                                            ArtifactRepository remoteRepository )
                throws ArtifactMetadataRetrievalException
            {
                throw new UnsupportedOperationException( "Cannot get available versions in this test case" );
            }
            public ResolutionGroup retrieve( MetadataResolutionRequest request )
                throws ArtifactMetadataRetrievalException
            {
                return retrieve( request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories() );
            }
            public List<ArtifactVersion> retrieveAvailableVersions( MetadataResolutionRequest request )
                throws ArtifactMetadataRetrievalException
            {
                return retrieveAvailableVersions( request.getArtifact(), request.getLocalRepository(), request.getRemoteRepositories() );
            }
        };
        ArtifactResolutionResult result = null;
        Set set = new LinkedHashSet();
        set.add( n );
        set.add( m );
        result =
            artifactResolver.resolveTransitively( set, projectArtifact, remoteRepositories(), localRepository(), mds );
        printErrors( result );
        Iterator i = result.getArtifacts().iterator();
        assertEquals( "n should be first", n, i.next() );
        assertEquals( "m should be second", m, i.next() );
        set = new LinkedHashSet();
        set.add( m );
        set.add( n );
        result =
            artifactResolver.resolveTransitively( set, projectArtifact, remoteRepositories(), localRepository(), mds );
        printErrors( result );
        i = result.getArtifacts().iterator();
        assertEquals( "m should be first", m, i.next() );
        assertEquals( "n should be second", n, i.next() );
    }
    private void printErrors( ArtifactResolutionResult result )
    {
        if ( result.hasMissingArtifacts() )
        {
            for ( Artifact artifact : result.getMissingArtifacts() )
            {
                System.err.println( "Missing: " + artifact );
            }
        }
        if ( result.hasExceptions() )
        {
            for ( Exception e : result.getExceptions() )
            {
                e.printStackTrace();
            }
        }
    }
}
