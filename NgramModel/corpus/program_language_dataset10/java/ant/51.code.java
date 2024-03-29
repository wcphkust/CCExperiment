package org.apache.tools.ant;
import java.util.EventListener;
public interface BuildListener extends EventListener {
    void buildStarted(BuildEvent event);
    void buildFinished(BuildEvent event);
    void targetStarted(BuildEvent event);
    void targetFinished(BuildEvent event);
    void taskStarted(BuildEvent event);
    void taskFinished(BuildEvent event);
    void messageLogged(BuildEvent event);
}
