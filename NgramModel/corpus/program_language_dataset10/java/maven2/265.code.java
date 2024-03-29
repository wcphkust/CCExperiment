package org.apache.maven.profiles;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import java.io.File;
import java.io.IOException;
public interface MavenProfilesBuilder
{
    String ROLE = MavenProfilesBuilder.class.getName();
    ProfilesRoot buildProfiles( File basedir )
        throws IOException, XmlPullParserException;
}
