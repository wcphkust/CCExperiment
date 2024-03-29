package org.apache.maven.project.interpolation;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.util.ValueSourceUtils;
import java.io.File;
import java.util.List;
@Deprecated
public class PathTranslatingPostProcessor
    implements InterpolationPostProcessor
{
    private final List<String> unprefixedPathKeys;
    private final File projectDir;
    private final PathTranslator pathTranslator;
    private final List<String> expressionPrefixes;
    public PathTranslatingPostProcessor( List<String> expressionPrefixes, List<String> unprefixedPathKeys,
                                         File projectDir, PathTranslator pathTranslator )
    {
        this.expressionPrefixes = expressionPrefixes;
        this.unprefixedPathKeys = unprefixedPathKeys;
        this.projectDir = projectDir;
        this.pathTranslator = pathTranslator;
    }
    public Object execute( String expression,
                                      Object value )
    {
        expression = ValueSourceUtils.trimPrefix( expression, expressionPrefixes, true );
        if ( projectDir != null && value != null && unprefixedPathKeys.contains( expression ) )
        {
            return pathTranslator.alignToBaseDirectory( String.valueOf( value ), projectDir );
        }
        return value;
    }
}
