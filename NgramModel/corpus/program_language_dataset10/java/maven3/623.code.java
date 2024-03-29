package org.apache.maven.cli;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
public final class CLIReportingUtils
{
    public static final long MB = 1024 * 1024;
    public static final int MS_PER_SEC = 1000;
    public static final int SEC_PER_MIN = 60;
    public static void showVersion( PrintStream stdout )
    {
        Properties properties = getBuildProperties();
        String timestamp = reduce( properties.getProperty( "timestamp" ) );
        String version = reduce( properties.getProperty( "version" ) );
        String rev = reduce( properties.getProperty( "buildNumber" ) );
        String msg = "Apache Maven ";
        msg += ( version != null ? version : "<version unknown>" );
        if ( rev != null || timestamp != null )
        {
            msg += " (";
            msg += ( rev != null ? "r" + rev : "" );
            if ( timestamp != null )
            {
                SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" );
                String ts = fmt.format( new Date( Long.valueOf( timestamp ).longValue() ) );
                msg += ( rev != null ? "; " : "" ) + ts;
            }
            msg += ")";
        }
        stdout.println( msg );
        stdout.println( "Maven home: " + System.getProperty( "maven.home", "<unknown maven home>" ) );
        stdout.println( "Java version: " + System.getProperty( "java.version", "<unknown java version>" )
            + ", vendor: " + System.getProperty( "java.vendor", "<unknown vendor>" ) );
        stdout.println( "Java home: " + System.getProperty( "java.home", "<unknown java home>" ) );
        stdout.println( "Default locale: " + Locale.getDefault() + ", platform encoding: "
            + System.getProperty( "file.encoding", "<unknown encoding>" ) );
        stdout.println( "OS name: \"" + Os.OS_NAME + "\", version: \"" + Os.OS_VERSION + "\", arch: \"" + Os.OS_ARCH
            + "\", family: \"" + Os.OS_FAMILY + "\"" );
    }
    private static String reduce( String s )
    {
        return ( s != null ? ( s.startsWith( "${" ) && s.endsWith( "}" ) ? null : s ) : null );
    }
    private static void stats( Date start, Logger logger )
    {
        Date finish = new Date();
        long time = finish.getTime() - start.getTime();
        logger.info( "Total time: " + formatTime( time ) );
        logger.info( "Finished at: " + finish );
        System.gc();
        Runtime r = Runtime.getRuntime();
        logger.info( "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / MB + "M/" + r.totalMemory() / MB + "M" );
    }
    private static String formatTime( long ms )
    {
        long secs = ms / MS_PER_SEC;
        long min = secs / SEC_PER_MIN;
        secs = secs % SEC_PER_MIN;
        String msg = "";
        if ( min > 1 )
        {
            msg = min + " minutes ";
        }
        else if ( min == 1 )
        {
            msg = "1 minute ";
        }
        if ( secs > 1 )
        {
            msg += secs + " seconds";
        }
        else if ( secs == 1 )
        {
            msg += "1 second";
        }
        else if ( min == 0 )
        {
            msg += "< 1 second";
        }
        return msg;
    }
    private static String getFormattedTime( long time )
    {
        String pattern = "s.SSS's'";
        if ( time / 60000L > 0 )
        {
            pattern = "m:s" + pattern;
            if ( time / 3600000L > 0 )
            {
                pattern = "H:m" + pattern;
            }
        }
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.format( new Date( time ) );
    }
    static Properties getBuildProperties()
    {
        Properties properties = new Properties();
        InputStream resourceAsStream = null;
        try
        {
            resourceAsStream = MavenCli.class.getResourceAsStream( "/org/apache/maven/messages/build.properties" );
            if ( resourceAsStream != null )
            {
                properties.load( resourceAsStream );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }
        return properties;
    }
    public static void showError( Logger logger, String message, Throwable e, boolean showStackTrace )
    {
        if ( logger == null )
        {
            logger = new PrintStreamLogger( System.out );
        }
        if ( showStackTrace )
        {
            logger.error( message, e );
        }
        else
        {
            logger.error( message );
            if ( e != null )
            {
                logger.error( e.getMessage() );
                for ( Throwable cause = e.getCause(); cause != null; cause = cause.getCause() )
                {
                    logger.error( "Caused by: " + cause.getMessage() );
                }
            }
        }
    }
}
