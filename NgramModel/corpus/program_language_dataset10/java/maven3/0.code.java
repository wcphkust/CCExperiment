package org.apache.maven.settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import junit.framework.TestCase;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
public class GlobalSettingsTest
    extends TestCase
{
    public void testValidGlobalSettings()
        throws Exception
    {
        String basedir = System.getProperty( "basedir", System.getProperty( "user.dir" ) );
        File globalSettingsFile = new File( basedir, "src/conf/settings.xml" );
        assertTrue( globalSettingsFile.getAbsolutePath(), globalSettingsFile.isFile() );
        Reader reader = new InputStreamReader( new FileInputStream( globalSettingsFile ), "UTF-8" );
        try
        {
            new SettingsXpp3Reader().read( reader );
        }
        finally
        {
            reader.close();
        }
    }
}
