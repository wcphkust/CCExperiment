package org.apache.maven.repository.legacy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Properties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.repository.Proxy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
@Component( role = UpdateCheckManager.class )
public class DefaultUpdateCheckManager
    extends AbstractLogEnabled
    implements UpdateCheckManager
{
    private static final String ERROR_KEY_SUFFIX = ".error";
    public DefaultUpdateCheckManager()
    {
    }
    public DefaultUpdateCheckManager( Logger logger )
    {
        enableLogging( logger );
    }
    public static final String LAST_UPDATE_TAG = ".lastUpdated";
    private static final String TOUCHFILE_NAME = "resolver-status.properties";
    public boolean isUpdateRequired( Artifact artifact, ArtifactRepository repository )
    {
        File file = artifact.getFile();
        ArtifactRepositoryPolicy policy = artifact.isSnapshot() ? repository.getSnapshots() : repository.getReleases();
        if ( !policy.isEnabled() )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                                   "Skipping update check for " + artifact + " (" + file + ") from "
                                       + repository.getId() + " (" + repository.getUrl() + ")" );
            }
            return false;
        }
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug(
                               "Determining update check for " + artifact + " (" + file + ") from "
                                   + repository.getId() + " (" + repository.getUrl() + ")" );
        }
        if ( file == null )
        {
            return true;
        }
        Date lastCheckDate;
        if ( file.exists() )
        {
            lastCheckDate = new Date ( file.lastModified() );
        }
        else
        {
            File touchfile = getTouchfile( artifact );
            lastCheckDate = readLastUpdated( touchfile, getRepositoryKey( repository ) );
        }
        return ( lastCheckDate == null ) || policy.checkOutOfDate( lastCheckDate );
    }
    public boolean isUpdateRequired( RepositoryMetadata metadata, ArtifactRepository repository, File file )
    {
        ArtifactRepositoryPolicy policy = metadata.getPolicy( repository );
        if ( !policy.isEnabled() )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                                   "Skipping update check for " + metadata.getKey() + " (" + file + ") from "
                                       + repository.getId() + " (" + repository.getUrl() + ")" );
            }
            return false;
        }
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug(
                               "Determining update check for " + metadata.getKey() + " (" + file + ") from "
                                   + repository.getId() + " (" + repository.getUrl() + ")" );
        }
        if ( file == null )
        {
            return true;
        }
        Date lastCheckDate = readLastUpdated( metadata, repository, file );
        return ( lastCheckDate == null ) || policy.checkOutOfDate( lastCheckDate );
    }
    private Date readLastUpdated( RepositoryMetadata metadata, ArtifactRepository repository, File file )
    {
        File touchfile = getTouchfile( metadata, file );
        String key = getMetadataKey( repository, file );
        return readLastUpdated( touchfile, key );
    }
    public String getError( Artifact artifact, ArtifactRepository repository )
    {
        File touchFile = getTouchfile( artifact );
        return getError( touchFile, getRepositoryKey( repository ) );
    }
    public void touch( Artifact artifact, ArtifactRepository repository, String error )
    {
        File file = artifact.getFile();
        File touchfile = getTouchfile( artifact );
        if ( file.exists() )
        {
            touchfile.delete();
        }
        else
        {
            writeLastUpdated( touchfile, getRepositoryKey( repository ), error );
        }
    }
    public void touch( RepositoryMetadata metadata, ArtifactRepository repository, File file )
    {
        File touchfile = getTouchfile( metadata, file );
        String key = getMetadataKey( repository, file );
        writeLastUpdated( touchfile, key, null );
    }
    String getMetadataKey( ArtifactRepository repository, File file )
    {
        return repository.getId() + '.' + file.getName() + LAST_UPDATE_TAG;
    }
    String getRepositoryKey( ArtifactRepository repository )
    {
        StringBuilder buffer = new StringBuilder( 256 );
        Proxy proxy = repository.getProxy();
        if ( proxy != null )
        {
            if ( proxy.getUserName() != null )
            {
                int hash = ( proxy.getUserName() + proxy.getPassword() ).hashCode();
                buffer.append( hash ).append( '@' );
            }
            buffer.append( proxy.getHost() ).append( ':' ).append( proxy.getPort() ).append( '>' );
        }
        Authentication auth = repository.getAuthentication();
        if ( auth != null )
        {
            int hash = ( auth.getUsername() + auth.getPassword() ).hashCode();
            buffer.append( hash ).append( '@' );
        }
        buffer.append( repository.getUrl() );
        return buffer.toString();
    }
    private void writeLastUpdated( File touchfile, String key, String error )
    {
        synchronized ( touchfile.getAbsolutePath().intern() )
        {
            if ( !touchfile.getParentFile().exists() && !touchfile.getParentFile().mkdirs() )
            {
                getLogger().debug( "Failed to create directory: " + touchfile.getParent()
                                       + " for tracking artifact metadata resolution." );
                return;
            }
            FileChannel channel = null;
            FileLock lock = null;
            try
            {
                Properties props = new Properties();
                channel = new RandomAccessFile( touchfile, "rw" ).getChannel();
                lock = channel.lock( 0, channel.size(), false );
                if ( touchfile.canRead() )
                {
                    getLogger().debug( "Reading resolution-state from: " + touchfile );
                    ByteBuffer buffer = ByteBuffer.allocate( (int) channel.size() );
                    channel.read( buffer );
                    buffer.flip();
                    ByteArrayInputStream stream = new ByteArrayInputStream( buffer.array() );
                    props.load( stream );
                }
                props.setProperty( key, Long.toString( System.currentTimeMillis() ) );
                if ( error != null )
                {
                    props.setProperty( key + ERROR_KEY_SUFFIX, error );
                }
                else
                {
                    props.remove( key + ERROR_KEY_SUFFIX );
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                getLogger().debug( "Writing resolution-state to: " + touchfile );
                props.store( stream, "Last modified on: " + new Date() );
                byte[] data = stream.toByteArray();
                ByteBuffer buffer = ByteBuffer.allocate( data.length );
                buffer.put( data );
                buffer.flip();
                channel.position( 0 );
                channel.write( buffer );
            }
            catch ( IOException e )
            {
                getLogger().debug( "Failed to record lastUpdated information for resolution.\nFile: "
                                       + touchfile.toString() + "; key: " + key, e );
            }
            finally
            {
                if ( lock != null )
                {
                    try
                    {
                        lock.release();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error releasing exclusive lock for resolution tracking file: "
                                               + touchfile, e );
                    }
                }
                if ( channel != null )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error closing FileChannel for resolution tracking file: "
                                               + touchfile, e );
                    }
                }
            }
        }
    }
    Date readLastUpdated( File touchfile, String key )
    {
        getLogger().debug( "Searching for " + key + " in resolution tracking file." );
        Properties props = read( touchfile );
        if ( props != null )
        {
            String rawVal = props.getProperty( key );
            if ( rawVal != null )
            {
                try
                {
                    return new Date( Long.parseLong( rawVal ) );
                }
                catch ( NumberFormatException e )
                {
                    getLogger().debug( "Cannot parse lastUpdated date: \'" + rawVal + "\'. Ignoring.", e );
                }
            }
        }
        return null;
    }
    private String getError( File touchFile, String key )
    {
        Properties props = read( touchFile );
        if ( props != null )
        {
            return props.getProperty( key + ERROR_KEY_SUFFIX );
        }
        return null;
    }
    private Properties read( File touchfile )
    {
        if ( !touchfile.canRead() )
        {
            getLogger().debug( "Skipped unreadable resolution tracking file " + touchfile );
            return null;
        }
        synchronized ( touchfile.getAbsolutePath().intern() )
        {
            FileInputStream stream = null;
            FileLock lock = null;
            FileChannel channel = null;
            try
            {
                Properties props = new Properties();
                stream = new FileInputStream( touchfile );
                channel = stream.getChannel();
                lock = channel.lock( 0, channel.size(), true );
                getLogger().debug( "Reading resolution-state from: " + touchfile );
                props.load( stream );
                return props;
            }
            catch ( IOException e )
            {
                getLogger().debug( "Failed to read resolution tracking file " + touchfile, e );
                return null;
            }
            finally
            {
                if ( lock != null )
                {
                    try
                    {
                        lock.release();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error releasing shared lock for resolution tracking file: " + touchfile,
                                           e );
                    }
                }
                if ( channel != null )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( IOException e )
                    {
                        getLogger().debug( "Error closing FileChannel for resolution tracking file: " + touchfile, e );
                    }
                }
            }
        }
    }
    File getTouchfile( Artifact artifact )
    {
        StringBuilder sb = new StringBuilder( 128 );
        sb.append( artifact.getArtifactId() );
        sb.append( '-' ).append( artifact.getBaseVersion() );
        if ( artifact.getClassifier() != null )
        {
            sb.append( '-' ).append( artifact.getClassifier() );
        }
        sb.append( '.' ).append( artifact.getType() ).append( LAST_UPDATE_TAG );
        return new File( artifact.getFile().getParentFile(), sb.toString() );
    }
    File getTouchfile( RepositoryMetadata metadata, File file )
    {
        return new File( file.getParent(), TOUCHFILE_NAME );
    }
}
