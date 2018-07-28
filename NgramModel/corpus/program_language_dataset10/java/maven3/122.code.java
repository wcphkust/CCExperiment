package org.apache.maven.repository;
import java.util.ArrayList;
import java.util.Collection;
public class MetadataGraph
{
    Collection<MetadataGraphNode> nodes;
    MetadataGraphNode entry;
    public MetadataGraph( MetadataGraphNode entry )
    {
        this();
        this.entry = entry;
    }
    public MetadataGraph()
    {
        nodes = new ArrayList<MetadataGraphNode>( 64 );
    }
    public void addNode( MetadataGraphNode node )
    {
        nodes.add( node );
    }
    public MetadataGraphNode findNode( MavenArtifactMetadata md )
    {
        for ( MetadataGraphNode mgn : nodes )
        {
            if ( mgn.metadata.equals( md ) )
            {
                return mgn;
            }
        }
        MetadataGraphNode node = new MetadataGraphNode( md );
        addNode( node );
        return node;
    }
    public MetadataGraphNode getEntry()
    {
        return entry;
    }
    public Collection<MetadataGraphNode> getNodes()
    {
        return nodes;
    }
}