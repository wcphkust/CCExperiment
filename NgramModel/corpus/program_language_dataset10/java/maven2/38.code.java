package org.apache.maven.artifact.resolver;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
public class ArtifactResolutionResult
{
    private Set<ResolutionNode> resolutionNodes;
    private Set<Artifact> artifacts;
    public ArtifactResolutionResult()
    {
    }
    public Set<Artifact> getArtifacts()
    {
        if ( artifacts == null )
        {
            artifacts = new LinkedHashSet<Artifact>();
            for ( ResolutionNode node : resolutionNodes )
            {
                artifacts.add( node.getArtifact() );
            }
        }
        return artifacts;
    }
    public Set<ResolutionNode> getArtifactResolutionNodes()
    {
        return resolutionNodes;
    }
    public void setArtifactResolutionNodes( Set<ResolutionNode> resolutionNodes )
    {
        this.resolutionNodes = resolutionNodes;
        this.artifacts = null;
    }
    public String toString()
    {
        return "Artifacts: " + this.artifacts + " Nodes: " + this.resolutionNodes;
    }
}
