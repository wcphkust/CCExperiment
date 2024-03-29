package org.apache.maven.artifact.versioning;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
@Deprecated
public class ManagedVersionMap
    extends HashMap<String, Artifact>
{
    public ManagedVersionMap( Map<String, Artifact> map )
    {
        super();
        if ( map != null )
        {
            putAll( map );
        }
    }
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( "ManagedVersionMap (" + size() + " entries)\n" );
        Iterator<String> iter = keySet().iterator();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            buffer.append( key ).append( "=" ).append( get( key ) );
            if ( iter.hasNext() )
            {
                buffer.append( "\n" );
            }
        }
        return buffer.toString();
    }
}
