package org.apache.maven.settings.building;
import java.io.File;
import java.util.Properties;
public class DefaultSettingsBuildingRequest
    implements SettingsBuildingRequest
{
    private File globalSettingsFile;
    private File userSettingsFile;
    private SettingsSource globalSettingsSource;
    private SettingsSource userSettingsSource;
    private Properties systemProperties;
    private Properties userProperties;
    public File getGlobalSettingsFile()
    {
        return globalSettingsFile;
    }
    public DefaultSettingsBuildingRequest setGlobalSettingsFile( File globalSettingsFile )
    {
        this.globalSettingsFile = globalSettingsFile;
        return this;
    }
    public SettingsSource getGlobalSettingsSource()
    {
        return globalSettingsSource;
    }
    public DefaultSettingsBuildingRequest setGlobalSettingsSource( SettingsSource globalSettingsSource )
    {
        this.globalSettingsSource = globalSettingsSource;
        return this;
    }
    public File getUserSettingsFile()
    {
        return userSettingsFile;
    }
    public DefaultSettingsBuildingRequest setUserSettingsFile( File userSettingsFile )
    {
        this.userSettingsFile = userSettingsFile;
        return this;
    }
    public SettingsSource getUserSettingsSource()
    {
        return userSettingsSource;
    }
    public DefaultSettingsBuildingRequest setUserSettingsSource( SettingsSource userSettingsSource )
    {
        this.userSettingsSource = userSettingsSource;
        return this;
    }
    public Properties getSystemProperties()
    {
        if ( systemProperties == null )
        {
            systemProperties = new Properties();
        }
        return systemProperties;
    }
    public DefaultSettingsBuildingRequest setSystemProperties( Properties systemProperties )
    {
        if ( systemProperties != null )
        {
            this.systemProperties = new Properties();
            this.systemProperties.putAll( systemProperties );
        }
        else
        {
            this.systemProperties = null;
        }
        return this;
    }
    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }
        return userProperties;
    }
    public DefaultSettingsBuildingRequest setUserProperties( Properties userProperties )
    {
        if ( userProperties != null )
        {
            this.userProperties = new Properties();
            this.userProperties.putAll( userProperties );
        }
        else
        {
            this.userProperties = null;
        }
        return this;
    }
}
