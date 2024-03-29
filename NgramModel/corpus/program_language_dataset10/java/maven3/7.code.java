package org.apache.maven.repository.internal;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.sonatype.aether.artifact.Artifact;
final class LocalSnapshotMetadata
    extends MavenMetadata
{
    private final Collection<Artifact> artifacts = new ArrayList<Artifact>();
    private final boolean legacyFormat;
    public LocalSnapshotMetadata( Artifact artifact, boolean legacyFormat )
    {
        super( createMetadata( artifact, legacyFormat ), null );
        this.legacyFormat = legacyFormat;
    }
    public LocalSnapshotMetadata( Metadata metadata, File file, boolean legacyFormat )
    {
        super( metadata, file );
        this.legacyFormat = legacyFormat;
    }
    private static Metadata createMetadata( Artifact artifact, boolean legacyFormat )
    {
        Snapshot snapshot = new Snapshot();
        snapshot.setLocalCopy( true );
        Versioning versioning = new Versioning();
        versioning.setSnapshot( snapshot );
        Metadata metadata = new Metadata();
        metadata.setVersioning( versioning );
        metadata.setGroupId( artifact.getGroupId() );
        metadata.setArtifactId( artifact.getArtifactId() );
        metadata.setVersion( artifact.getBaseVersion() );
        if ( !legacyFormat )
        {
            metadata.setModelVersion( "1.1.0" );
        }
        return metadata;
    }
    public void bind( Artifact artifact )
    {
        artifacts.add( artifact );
    }
    public MavenMetadata setFile( File file )
    {
        return new LocalSnapshotMetadata( metadata, file, legacyFormat );
    }
    public Object getKey()
    {
        return getGroupId() + ':' + getArtifactId() + ':' + getVersion();
    }
    public static Object getKey( Artifact artifact )
    {
        return artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getBaseVersion();
    }
    @Override
    protected void merge( Metadata recessive )
    {
        metadata.getVersioning().updateTimestamp();
        if ( !legacyFormat )
        {
            String lastUpdated = metadata.getVersioning().getLastUpdated();
            Map<String, SnapshotVersion> versions = new LinkedHashMap<String, SnapshotVersion>();
            for ( Artifact artifact : artifacts )
            {
                SnapshotVersion sv = new SnapshotVersion();
                sv.setClassifier( artifact.getClassifier() );
                sv.setExtension( artifact.getExtension() );
                sv.setVersion( getVersion() );
                sv.setUpdated( lastUpdated );
                versions.put( getKey( sv.getClassifier(), sv.getExtension() ), sv );
            }
            Versioning versioning = recessive.getVersioning();
            if ( versioning != null )
            {
                for ( SnapshotVersion sv : versioning.getSnapshotVersions() )
                {
                    String key = getKey( sv.getClassifier(), sv.getExtension() );
                    if ( !versions.containsKey( key ) )
                    {
                        versions.put( key, sv );
                    }
                }
            }
            metadata.getVersioning().setSnapshotVersions( new ArrayList<SnapshotVersion>( versions.values() ) );
        }
        artifacts.clear();
    }
    private String getKey( String classifier, String extension )
    {
        return classifier + ':' + extension;
    }
    public String getGroupId()
    {
        return metadata.getGroupId();
    }
    public String getArtifactId()
    {
        return metadata.getArtifactId();
    }
    public String getVersion()
    {
        return metadata.getVersion();
    }
    public Nature getNature()
    {
        return Nature.SNAPSHOT;
    }
}
