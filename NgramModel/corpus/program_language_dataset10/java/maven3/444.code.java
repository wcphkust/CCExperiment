package org.apache.maven.plugin.internal;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
class PlexusUtilsInjector
    implements DependencyGraphTransformer
{
    private static final String GID = "org.codehaus.plexus";
    private static final String AID = "plexus-utils";
    private static final String VER = "1.1";
    private static final String EXT = "jar";
    public DependencyNode transformGraph( DependencyNode node, DependencyGraphTransformationContext context )
        throws RepositoryException
    {
        if ( findPlexusUtils( node ) == null )
        {
            Artifact pu = new DefaultArtifact( GID, AID, null, EXT, VER );
            DefaultDependencyNode child = new DefaultDependencyNode( new Dependency( pu, JavaScopes.RUNTIME ) );
            child.setRepositories( node.getRepositories() );
            child.setRequestContext( node.getRequestContext() );
            node.getChildren().add( child );
        }
        return node;
    }
    private DependencyNode findPlexusUtils( DependencyNode node )
    {
        Artifact artifact = node.getDependency().getArtifact();
        if ( AID.equals( artifact.getArtifactId() ) && GID.equals( artifact.getGroupId() )
            && EXT.equals( artifact.getExtension() ) && "".equals( artifact.getClassifier() ) )
        {
            return node;
        }
        for ( DependencyNode child : node.getChildren() )
        {
            DependencyNode result = findPlexusUtils( child );
            if ( result != null )
            {
                return result;
            }
        }
        return null;
    }
}
