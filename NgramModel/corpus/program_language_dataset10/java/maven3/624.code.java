package org.apache.maven.cli;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;
import org.sonatype.aether.transfer.TransferResource;
public class ConsoleMavenTransferListener
    extends AbstractMavenTransferListener
{
    private Map<TransferResource, Long> downloads = new ConcurrentHashMap<TransferResource, Long>();
    private int lastLength;
    public ConsoleMavenTransferListener( PrintStream out )
    {
        super( out );
    }
    @Override
    public void transferProgressed( TransferEvent event )
        throws TransferCancelledException
    {
        TransferResource resource = event.getResource();
        downloads.put( resource, Long.valueOf( event.getTransferredBytes() ) );
        StringBuilder buffer = new StringBuilder( 64 );
        for ( Map.Entry<TransferResource, Long> entry : downloads.entrySet() )
        {
            long total = entry.getKey().getContentLength();
            Long complete = entry.getValue();
            if ( complete != null )
            {
                buffer.append( getStatus( complete.longValue(), total ) ).append( "  " );
            }
        }
        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad( buffer, pad );
        buffer.append( '\r' );
        out.print( buffer );
    }
    private String getStatus( long complete, long total )
    {
        if ( total >= 1024 )
        {
            return toKB( complete ) + "/" + toKB( total ) + " KB ";
        }
        else if ( total >= 0 )
        {
            return complete + "/" + total + " B ";
        }
        else if ( complete >= 1024 )
        {
            return toKB( complete ) + " KB ";
        }
        else
        {
            return complete + " B ";
        }
    }
    private void pad( StringBuilder buffer, int spaces )
    {
        String block = "                                        ";
        while ( spaces > 0 )
        {
            int n = Math.min( spaces, block.length() );
            buffer.append( block, 0, n );
            spaces -= n;
        }
    }
    @Override
    public void transferSucceeded( TransferEvent event )
    {
        transferCompleted( event );
        super.transferSucceeded( event );
    }
    @Override
    public void transferFailed( TransferEvent event )
    {
        transferCompleted( event );
        super.transferFailed( event );
    }
    private void transferCompleted( TransferEvent event )
    {
        downloads.remove( event.getResource() );
        StringBuilder buffer = new StringBuilder( 64 );
        pad( buffer, lastLength );
        buffer.append( '\r' );
        out.print( buffer );
    }
}
