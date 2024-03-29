package org.apache.cassandra.streaming;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.utils.Pair;
public class IncomingStreamReader
{
    private static final Logger logger = LoggerFactory.getLogger(IncomingStreamReader.class);
    protected final PendingFile localFile;
    protected final PendingFile remoteFile;
    private final SocketChannel socketChannel;
    protected final StreamInSession session;
    public IncomingStreamReader(StreamHeader header, Socket socket) throws IOException
    {
        this.socketChannel = socket.getChannel();
        InetSocketAddress remoteAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
        session = StreamInSession.get(remoteAddress.getAddress(), header.sessionId);
        session.addFiles(header.pendingFiles);
        session.setCurrentFile(header.file);
        session.setTable(header.table);
        remoteFile = header.file;
        localFile = remoteFile != null ? StreamIn.getContextMapping(remoteFile) : null;
    }
    public void read() throws IOException
    {
        if (remoteFile != null)
            readFile();
        session.closeIfFinished();
    }
    protected void readFile() throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Receiving stream");
            logger.debug("Creating file for {}", localFile.getFilename());
        }
        FileOutputStream fos = new FileOutputStream(localFile.getFilename(), true);
        FileChannel fc = fos.getChannel();
        long offset = 0;
        try
        {
            for (Pair<Long, Long> section : localFile.sections)
            {
                long length = section.right - section.left;
                long bytesRead = 0;
                while (bytesRead < length)
                {
                    bytesRead = readnwrite(length, bytesRead, offset, fc);
                }
                offset += length;
            }
        }
        catch (IOException ex)
        {
            session.retry(remoteFile);
            FileUtils.deleteWithConfirm(new File(localFile.getFilename()));
            throw ex;
        }
        finally
        {
            fc.close();
        }
        session.finished(remoteFile, localFile);
    }
    protected long readnwrite(long length, long bytesRead, long offset, FileChannel fc) throws IOException
    {
        long toRead = Math.min(FileStreamTask.CHUNK_SIZE, length - bytesRead);
        long lastRead = fc.transferFrom(socketChannel, offset + bytesRead, toRead);
        bytesRead += lastRead;
        remoteFile.progress += lastRead;
        return bytesRead;
    }
}
