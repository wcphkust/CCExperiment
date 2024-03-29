package org.apache.maven.repository.internal;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.aether.RepositoryEvent.EventType;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.util.listener.DefaultRepositoryEvent;
import org.sonatype.aether.util.metadata.DefaultMetadata;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.aether.impl.MetadataResolver;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;
@Component( role = VersionRangeResolver.class )
public class DefaultVersionRangeResolver
    implements VersionRangeResolver, Service
{
    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";
    @Requirement
    private Logger logger = NullLogger.INSTANCE;
    @Requirement
    private MetadataResolver metadataResolver;
    public void initService( ServiceLocator locator )
    {
        setLogger( locator.getService( Logger.class ) );
        setMetadataResolver( locator.getService( MetadataResolver.class ) );
    }
    public DefaultVersionRangeResolver setLogger( Logger logger )
    {
        this.logger = ( logger != null ) ? logger : NullLogger.INSTANCE;
        return this;
    }
    public DefaultVersionRangeResolver setMetadataResolver( MetadataResolver metadataResolver )
    {
        if ( metadataResolver == null )
        {
            throw new IllegalArgumentException( "metadata resolver has not been specified" );
        }
        this.metadataResolver = metadataResolver;
        return this;
    }
    public VersionRangeResult resolveVersionRange( RepositorySystemSession session, VersionRangeRequest request )
        throws VersionRangeResolutionException
    {
        VersionRangeResult result = new VersionRangeResult( request );
        VersionScheme versionScheme = new GenericVersionScheme();
        VersionConstraint versionConstraint;
        try
        {
            versionConstraint = versionScheme.parseVersionConstraint( request.getArtifact().getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            result.addException( e );
            throw new VersionRangeResolutionException( result );
        }
        result.setVersionConstraint( versionConstraint );
        if ( versionConstraint.getRanges().isEmpty() )
        {
            result.addVersion( versionConstraint.getVersion() );
        }
        else
        {
            Map<String, ArtifactRepository> versionIndex = getVersions( session, result, request );
            List<Version> versions = new ArrayList<Version>();
            for ( Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet() )
            {
                try
                {
                    Version ver = versionScheme.parseVersion( v.getKey() );
                    if ( versionConstraint.containsVersion( ver ) )
                    {
                        versions.add( ver );
                        result.setRepository( ver, v.getValue() );
                    }
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    result.addException( e );
                }
            }
            Collections.sort( versions );
            result.setVersions( versions );
        }
        return result;
    }
    private Map<String, ArtifactRepository> getVersions( RepositorySystemSession session, VersionRangeResult result,
                                                         VersionRangeRequest request )
    {
        Map<String, ArtifactRepository> versionIndex = new HashMap<String, ArtifactRepository>();
        Metadata metadata =
            new DefaultMetadata( request.getArtifact().getGroupId(), request.getArtifact().getArtifactId(),
                                 MAVEN_METADATA_XML, Metadata.Nature.RELEASE_OR_SNAPSHOT );
        List<MetadataRequest> metadataRequests = new ArrayList<MetadataRequest>( request.getRepositories().size() );
        metadataRequests.add( new MetadataRequest( metadata, null, request.getRequestContext() ) );
        for ( RemoteRepository repository : request.getRepositories() )
        {
            MetadataRequest metadataRequest = new MetadataRequest( metadata, repository, request.getRequestContext() );
            metadataRequest.setDeleteLocalCopyIfMissing( true );
            metadataRequests.add( metadataRequest );
        }
        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata( session, metadataRequests );
        WorkspaceReader workspace = session.getWorkspaceReader();
        if ( workspace != null )
        {
            List<String> versions = workspace.findVersions( request.getArtifact() );
            for ( String version : versions )
            {
                versionIndex.put( version, workspace.getRepository() );
            }
        }
        for ( MetadataResult metadataResult : metadataResults )
        {
            result.addException( metadataResult.getException() );
            ArtifactRepository repository = metadataResult.getRequest().getRepository();
            if ( repository == null )
            {
                repository = session.getLocalRepository();
            }
            Versioning versioning = readVersions( session, metadataResult.getMetadata(), repository, result );
            for ( String version : versioning.getVersions() )
            {
                if ( !versionIndex.containsKey( version ) )
                {
                    versionIndex.put( version, repository );
                }
            }
        }
        return versionIndex;
    }
    private Versioning readVersions( RepositorySystemSession session, Metadata metadata, ArtifactRepository repository,
                                     VersionRangeResult result )
    {
        Versioning versioning = null;
        FileInputStream fis = null;
        try
        {
            if ( metadata != null && metadata.getFile() != null && metadata.getFile().exists() )
            {
                fis = new FileInputStream( metadata.getFile() );
                org.apache.maven.artifact.repository.metadata.Metadata m = new MetadataXpp3Reader().read( fis, false );
                versioning = m.getVersioning();
            }
        }
        catch ( Exception e )
        {
            invalidMetadata( session, metadata, repository, e );
            result.addException( e );
        }
        finally
        {
            IOUtil.close( fis );
        }
        return ( versioning != null ) ? versioning : new Versioning();
    }
    private void invalidMetadata( RepositorySystemSession session, Metadata metadata, ArtifactRepository repository,
                                  Exception exception )
    {
        RepositoryListener listener = session.getRepositoryListener();
        if ( listener != null )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_INVALID, session );
            event.setMetadata( metadata );
            event.setException( exception );
            event.setRepository( repository );
            listener.metadataInvalid( event );
        }
    }
}
