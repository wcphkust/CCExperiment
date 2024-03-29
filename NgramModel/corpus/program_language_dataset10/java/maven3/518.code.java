package org.apache.maven.repository.legacy.metadata;
import org.apache.maven.artifact.Artifact;
public class ArtifactMetadataRetrievalException
    extends Exception 
{
    private Artifact artifact;
    @Deprecated
    public ArtifactMetadataRetrievalException( String message )
    {
        this( message, null, null );
    }
    @Deprecated
    public ArtifactMetadataRetrievalException( Throwable cause )
    {
        this( null, cause, null );
    }
    @Deprecated
    public ArtifactMetadataRetrievalException( String message,
                                               Throwable cause )
    {
        this( message, cause, null );
    }
    public ArtifactMetadataRetrievalException( String message,
                                               Throwable cause,
                                               Artifact artifact )
    {
        super( message, cause );
        this.artifact = artifact;
    }
    public Artifact getArtifact()
    {
        return artifact;
    }
}
