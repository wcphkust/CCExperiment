package org.apache.maven.lifecycle.mapping;
import java.util.List;
import java.util.Map;
public interface LifecycleMapping
{        
    @Deprecated
    String ROLE = LifecycleMapping.class.getName();
    Map<String, Lifecycle> getLifecycles();
    @Deprecated
    List<String> getOptionalMojos( String lifecycle );
    @Deprecated
    Map<String, String> getPhases( String lifecycle );
}
