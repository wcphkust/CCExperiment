package org.apache.cassandra.net;
import java.util.Map;
public interface MessagingServiceMBean
{
    public Map<String, Integer> getCommandPendingTasks();
    public Map<String, Long> getCommandCompletedTasks();
    public Map<String, Integer> getResponsePendingTasks();
    public Map<String, Long> getResponseCompletedTasks();
}
