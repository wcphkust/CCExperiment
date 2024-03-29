package org.apache.cassandra.streaming;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOError;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cassandra.net.IVerbHandler;
import org.apache.cassandra.net.Message;
public class StreamReplyVerbHandler implements IVerbHandler
{
    private static Logger logger = LoggerFactory.getLogger(StreamReplyVerbHandler.class);
    public void doVerb(Message message)
    {
        byte[] body = message.getMessageBody();
        ByteArrayInputStream bufIn = new ByteArrayInputStream(body);
        try
        {
            StreamReply reply = StreamReply.serializer.deserialize(new DataInputStream(bufIn));
            logger.debug("Received StreamReply {}", reply);
            StreamOutSession session = StreamOutSession.get(message.getFrom(), reply.sessionId);
            switch (reply.action)
            {
                case FILE_FINISHED:
                    session.validateCurrentFile(reply.file);
                    session.startNext();
                    break;
                case FILE_RETRY:
                    session.validateCurrentFile(reply.file);
                    logger.info("Need to re-stream file {} to {}", reply.file, message.getFrom());
                    session.retry();
                    break;
                case SESSION_FINISHED:
                    session.close();
                    break;
                default:
                    throw new RuntimeException("Cannot handle FileStatus.Action: " + reply.action);
            }
        }
        catch (IOException ex)
        {
            throw new IOError(ex);
        }
    }
}
