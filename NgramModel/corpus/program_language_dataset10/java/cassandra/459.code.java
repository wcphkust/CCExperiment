package org.apache.cassandra.db;
import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.junit.Test;
import org.apache.cassandra.CleanupHelper;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.commitlog.CommitLogHeader;
import org.apache.cassandra.db.filter.QueryPath;
import org.apache.cassandra.utils.Pair;
public class CommitLogTest extends CleanupHelper
{
    @Test
    public void testRecoveryWithEmptyHeader() throws Exception
    {
        testRecovery(new byte[0], new byte[10]);
    }
    @Test
    public void testRecoveryWithShortHeader() throws Exception
    {
        testRecovery(new byte[2], new byte[10]);
    }
    @Test
    public void testRecoveryWithGarbageHeader() throws Exception
    {
        byte[] garbage = new byte[100];
        (new java.util.Random()).nextBytes(garbage);
        testRecovery(garbage, garbage);
    }
    @Test
    public void testRecoveryWithEmptyLog() throws Exception
    {
        CommitLog.recover(new File[] {tmpFiles().right});
    }
    @Test
    public void testRecoveryWithShortLog() throws Exception
    {
        testRecoveryWithBadSizeArgument(100, 10);
    }
    @Test
    public void testRecoveryWithShortSize() throws Exception
    {
        testRecovery(new byte[0], new byte[2]);
    }
    @Test
    public void testRecoveryWithShortCheckSum() throws Exception
    {
        testRecovery(new byte[0], new byte[6]);
    }
    @Test
    public void testRecoveryWithGarbageLog() throws Exception
    {
        byte[] garbage = new byte[100];
        (new java.util.Random()).nextBytes(garbage);
        testRecovery(new byte[0], garbage);
    }
    @Test
    public void testRecoveryWithBadSizeChecksum() throws Exception
    {
        Checksum checksum = new CRC32();
        checksum.update(100);
        testRecoveryWithBadSizeArgument(100, 100, ~checksum.getValue());
    }
    @Test
    public void testRecoveryWithZeroSegmentSizeArgument() throws Exception
    {
        testRecoveryWithBadSizeArgument(0, 10); 
    }
    @Test
    public void testRecoveryWithNegativeSizeArgument() throws Exception
    {
        testRecoveryWithBadSizeArgument(-10, 10); 
    }
    protected void testRecoveryWithBadSizeArgument(int size, int dataSize) throws Exception
    {
        Checksum checksum = new CRC32();
        checksum.update(size);
        testRecoveryWithBadSizeArgument(size, dataSize, checksum.getValue());
    }
    protected void testRecoveryWithBadSizeArgument(int size, int dataSize, long checksum) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        dout.writeInt(size);
        dout.writeLong(checksum);
        dout.write(new byte[dataSize]);
        dout.close();
        testRecovery(new byte[0], out.toByteArray());
    }
    protected Pair<File, File> tmpFiles() throws IOException
    {
        File logFile = File.createTempFile("testRecoveryWithPartiallyWrittenHeaderTestFile", null);
        File headerFile = new File(CommitLogHeader.getHeaderPathFromSegmentPath(logFile.getAbsolutePath()));
        logFile.deleteOnExit();
        headerFile.deleteOnExit();
        assert logFile.length() == 0;
        assert headerFile.length() == 0;
        return new Pair<File, File>(headerFile, logFile);
    }
    protected void testRecovery(byte[] headerData, byte[] logData) throws Exception
    {
        Pair<File, File> tmpFiles = tmpFiles();
        File logFile = tmpFiles.right;
        File headerFile = tmpFiles.left;
        OutputStream lout = new FileOutputStream(logFile);
        OutputStream hout = new FileOutputStream(headerFile);
        lout.write(logData);
        hout.write(headerData);
        CommitLog.recover(new File[] {logFile}); 
    }
}
