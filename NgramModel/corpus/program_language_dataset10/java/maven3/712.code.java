package org.apache.maven.model.building;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Model;
public class ModelBuildingException
    extends Exception
{
    private final Model model;
    private final String modelId;
    private final List<ModelProblem> problems;
    public ModelBuildingException( Model model, String modelId, List<ModelProblem> problems )
    {
        super( toMessage( modelId, problems ) );
        this.model = model;
        this.modelId = ( modelId != null ) ? modelId : "";
        this.problems = new ArrayList<ModelProblem>();
        if ( problems != null )
        {
            this.problems.addAll( problems );
        }
    }
    public Model getModel()
    {
        return model;
    }
    public String getModelId()
    {
        return modelId;
    }
    public List<ModelProblem> getProblems()
    {
        return problems;
    }
    private static String toMessage( String modelId, List<ModelProblem> problems )
    {
        StringWriter buffer = new StringWriter( 1024 );
        PrintWriter writer = new PrintWriter( buffer );
        writer.print( problems.size() );
        writer.print( ( problems.size() == 1 ) ? " problem was " : " problems were " );
        writer.print( "encountered while building the effective model" );
        if ( modelId != null && modelId.length() > 0 )
        {
            writer.print( " for " );
            writer.print( modelId );
        }
        writer.println();
        for ( ModelProblem problem : problems )
        {
            writer.print( "[" );
            writer.print( problem.getSeverity() );
            writer.print( "] " );
            writer.print( problem.getMessage() );
            writer.print( " @ " );
            writer.println( ModelProblemUtils.formatLocation( problem, modelId ) );
        }
        return buffer.toString();
    }
}
