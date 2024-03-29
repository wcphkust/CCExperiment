package org.apache.maven.model.profile;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
public class DefaultProfileActivationContext
    implements ProfileActivationContext
{
    private List<String> activeProfileIds = Collections.emptyList();
    private List<String> inactiveProfileIds = Collections.emptyList();
    private Map<String, String> systemProperties = Collections.emptyMap();
    private Map<String, String> userProperties = Collections.emptyMap();
    private File projectDirectory;
    public List<String> getActiveProfileIds()
    {
        return activeProfileIds;
    }
    public DefaultProfileActivationContext setActiveProfileIds( List<String> activeProfileIds )
    {
        if ( activeProfileIds != null )
        {
            this.activeProfileIds = Collections.unmodifiableList( activeProfileIds );
        }
        else
        {
            this.activeProfileIds = Collections.emptyList();
        }
        return this;
    }
    public List<String> getInactiveProfileIds()
    {
        return inactiveProfileIds;
    }
    public DefaultProfileActivationContext setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        if ( inactiveProfileIds != null )
        {
            this.inactiveProfileIds = Collections.unmodifiableList( inactiveProfileIds );
        }
        else
        {
            this.inactiveProfileIds = Collections.emptyList();
        }
        return this;
    }
    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }
    @SuppressWarnings( "unchecked" )
    public DefaultProfileActivationContext setSystemProperties( Properties systemProperties )
    {
        if ( systemProperties != null )
        {
            this.systemProperties = Collections.unmodifiableMap( (Map) systemProperties );
        }
        else
        {
            this.systemProperties = Collections.emptyMap();
        }
        return this;
    }
    public DefaultProfileActivationContext setSystemProperties( Map<String, String> systemProperties )
    {
        if ( systemProperties != null )
        {
            this.systemProperties = Collections.unmodifiableMap( systemProperties );
        }
        else
        {
            this.systemProperties = Collections.emptyMap();
        }
        return this;
    }
    public Map<String, String> getUserProperties()
    {
        return userProperties;
    }
    @SuppressWarnings( "unchecked" )
    public DefaultProfileActivationContext setUserProperties( Properties userProperties )
    {
        if ( userProperties != null )
        {
            this.userProperties = Collections.unmodifiableMap( (Map) userProperties );
        }
        else
        {
            this.userProperties = Collections.emptyMap();
        }
        return this;
    }
    public DefaultProfileActivationContext setUserProperties( Map<String, String> userProperties )
    {
        if ( userProperties != null )
        {
            this.userProperties = Collections.unmodifiableMap( userProperties );
        }
        else
        {
            this.userProperties = Collections.emptyMap();
        }
        return this;
    }
    public File getProjectDirectory()
    {
        return projectDirectory;
    }
    public DefaultProfileActivationContext setProjectDirectory( File projectDirectory )
    {
        this.projectDirectory = projectDirectory;
        return this;
    }
}
