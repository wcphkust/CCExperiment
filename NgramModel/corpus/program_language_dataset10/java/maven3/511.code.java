package org.apache.maven.repository;
public interface ArtifactTransferListener
{
    boolean isShowChecksumEvents();
    void setShowChecksumEvents( boolean showChecksumEvents );
    void transferInitiated( ArtifactTransferEvent transferEvent );
    void transferStarted( ArtifactTransferEvent transferEvent );
    void transferProgress( ArtifactTransferEvent transferEvent );
    void transferCompleted( ArtifactTransferEvent transferEvent );
}
