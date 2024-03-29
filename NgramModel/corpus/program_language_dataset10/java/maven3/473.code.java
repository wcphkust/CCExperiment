package org.apache.maven.project;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.aether.graph.DependencyFilter;
@Component( role = ProjectRealmCache.class )
public class DefaultProjectRealmCache
    implements ProjectRealmCache
{
    private static class CacheKey
    {
        private final List<? extends ClassRealm> extensionRealms;
        private final int hashCode;
        public CacheKey( List<? extends ClassRealm> extensionRealms )
        {
            this.extensionRealms = ( extensionRealms != null ) ? extensionRealms : Collections.<ClassRealm> emptyList();
            this.hashCode = this.extensionRealms.hashCode();
        }
        @Override
        public int hashCode()
        {
            return hashCode;
        }
        @Override
        public boolean equals( Object o )
        {
            if ( o == this )
            {
                return true;
            }
            if ( !( o instanceof CacheKey ) )
            {
                return false;
            }
            CacheKey other = (CacheKey) o;
            return extensionRealms.equals( other.extensionRealms );
        }
    }
    private final Map<CacheKey, CacheRecord> cache = new HashMap<CacheKey, CacheRecord>();
    public CacheRecord get( List<? extends ClassRealm> extensionRealms )
    {
        return cache.get( new CacheKey( extensionRealms ) );
    }
    public CacheRecord put( List<? extends ClassRealm> extensionRealms, ClassRealm projectRealm,
                            DependencyFilter extensionArtifactFilter )
    {
        if ( projectRealm == null )
        {
            throw new NullPointerException();
        }
        CacheKey key = new CacheKey( extensionRealms );
        if ( cache.containsKey( key ) )
        {
            throw new IllegalStateException( "Duplicate project realm for extensions " + extensionRealms );
        }
        CacheRecord record = new CacheRecord( projectRealm, extensionArtifactFilter );
        cache.put( key, record );
        return record;
    }
    public void flush()
    {
        cache.clear();
    }
    public void register( MavenProject project, CacheRecord record )
    {
    }
}
