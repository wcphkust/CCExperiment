package org.apache.maven.model.path;
import org.codehaus.plexus.component.annotations.Component;
@Component( role = UrlNormalizer.class )
public class DefaultUrlNormalizer
    implements UrlNormalizer
{
    public String normalize( String url )
    {
        String result = url;
        if ( result != null )
        {
            while ( true )
            {
                int idx = result.indexOf( "/../" );
                if ( idx <= 0 )
                {
                    break;
                }
                int parent = idx - 1;
                while ( parent >= 0 && result.charAt( parent ) == '/' )
                {
                    parent--;
                }
                parent = result.lastIndexOf( '/', parent );
                if ( parent < 0 )
                {
                    break;
                }
                result = result.substring( 0, parent ) + result.substring( idx + 3 );
            }
        }
        return result;
    }
}
