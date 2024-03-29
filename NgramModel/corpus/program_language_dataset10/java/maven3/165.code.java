package org.apache.maven.repository.metadata;
public interface GraphConflictResolutionPolicy
{
    String ROLE = GraphConflictResolutionPolicy.class.getName();
    MetadataGraphEdge apply( MetadataGraphEdge e1, MetadataGraphEdge e2 );
}
