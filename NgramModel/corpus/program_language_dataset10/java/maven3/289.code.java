package org.apache.maven.artifact.repository.metadata;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
public abstract class AbstractRepositoryMetadata
    implements RepositoryMetadata
{
    private Metadata metadata;
    protected AbstractRepositoryMetadata( Metadata metadata )
    {
        this.metadata = metadata;
    }
    public String getRemoteFilename()
    {
        return "maven-metadata.xml";
    }
    public String getLocalFilename( ArtifactRepository repository )
    {        
        return "maven-metadata-" + repository.getKey() + ".xml";
    }
    public void storeInLocalRepository( ArtifactRepository localRepository,
                                        ArtifactRepository remoteRepository )
        throws RepositoryMetadataStoreException
    {
        try
        {
            updateRepositoryMetadata( localRepository, remoteRepository );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataStoreException( "Error updating group repository metadata", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataStoreException( "Error updating group repository metadata", e );
        }
    }
    protected void updateRepositoryMetadata( ArtifactRepository localRepository,
                                             ArtifactRepository remoteRepository )
        throws IOException, XmlPullParserException
    {
        MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();
        Metadata metadata = null;
        File metadataFile = new File( localRepository.getBasedir(),
            localRepository.pathOfLocalRepositoryMetadata( this, remoteRepository ) );
        if ( metadataFile.length() == 0 )
        {
            metadataFile.delete();
        }
        else if ( metadataFile.exists() )
        {
            Reader reader = null;
            try
            {
                reader = ReaderFactory.newXmlReader( metadataFile );
                metadata = mappingReader.read( reader, false );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }
        boolean changed;
        if ( metadata == null )
        {
            metadata = this.metadata;
            changed = true;
        }
        else
        {
            changed = metadata.merge( this.metadata );
        }
        String version = metadata.getVersion();
        if ( version != null && ( Artifact.LATEST_VERSION.equals( version ) || Artifact.RELEASE_VERSION.equals(
            version ) ) )
        {
            metadata.setVersion( null );
        }
        if ( changed || !metadataFile.exists() )
        {
            Writer writer = null;
            try
            {
                metadataFile.getParentFile().mkdirs();
                writer = WriterFactory.newXmlWriter( metadataFile );
                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();
                mappingWriter.write( writer, metadata );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        else
        {
            metadataFile.setLastModified( System.currentTimeMillis() );
        }
    }
    public String toString()
    {
        return "repository metadata for: \'" + getKey() + "\'";
    }
    protected static Metadata createMetadata( Artifact artifact,
                                              Versioning versioning )
    {
        Metadata metadata = new Metadata();
        metadata.setGroupId( artifact.getGroupId() );
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setVersion( artifact.getVersion() );
        metadata.setVersioning( versioning );
        return metadata;
    }
    protected static Versioning createVersioning( Snapshot snapshot )
    {
        Versioning versioning = new Versioning();
        versioning.setSnapshot( snapshot );
        versioning.updateTimestamp();
        return versioning;
    }
    public void setMetadata( Metadata metadata )
    {
        this.metadata = metadata;
    }
    public Metadata getMetadata()
    {
        return metadata;
    }
    public void merge( org.apache.maven.repository.legacy.metadata.ArtifactMetadata metadata )
    {
        AbstractRepositoryMetadata repoMetadata = (AbstractRepositoryMetadata) metadata;
        this.metadata.merge( repoMetadata.getMetadata() );
    }
    public void merge( ArtifactMetadata metadata )
    {
        AbstractRepositoryMetadata repoMetadata = (AbstractRepositoryMetadata) metadata;
        this.metadata.merge( repoMetadata.getMetadata() );
    }
    public String extendedToString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "\nRepository Metadata\n--------------------------" );
        buffer.append( "\nGroupId: " ).append( getGroupId() );
        buffer.append( "\nArtifactId: " ).append( getArtifactId() );
        buffer.append( "\nMetadata Type: " ).append( getClass().getName() );
        return buffer.toString();
    }
    public int getNature()
    {
        return RELEASE;
    }
    public ArtifactRepositoryPolicy getPolicy( ArtifactRepository repository )
    {
        int nature = getNature();
        if ( ( nature & RepositoryMetadata.RELEASE_OR_SNAPSHOT ) == RepositoryMetadata.RELEASE_OR_SNAPSHOT )
        {
            ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy( repository.getReleases() );
            policy.merge( repository.getSnapshots() );
            return policy;
        }
        else if ( ( nature & RepositoryMetadata.SNAPSHOT ) != 0 )
        {
            return repository.getSnapshots();
        }
        else
        {
            return repository.getReleases();
        }
    }
}
