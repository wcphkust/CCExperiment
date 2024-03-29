package org.apache.cassandra.contrib.stress.operations;
import org.apache.cassandra.contrib.stress.util.OperationThread;
import org.apache.cassandra.db.ColumnFamilyType;
import org.apache.cassandra.thrift.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class MultiGetter extends OperationThread
{
    public MultiGetter(int index)
    {
        super(index);
    }
    public void run()
    {
        SlicePredicate predicate = new SlicePredicate().setSlice_range(new SliceRange(ByteBuffer.wrap(new byte[]{}),
                                                                                      ByteBuffer.wrap(new byte[] {}),
                                                                                      false, session.getColumnsPerKey()));
        int offset = index * session.getKeysPerThread();
        Map<ByteBuffer,List<ColumnOrSuperColumn>> results = null;
        int count  = (((index + 1) * session.getKeysPerThread()) - offset) / session.getKeysPerCall();
        if (session.getColumnFamilyType() == ColumnFamilyType.Super)
        {
            for (int i = 0; i < count; i++)
            {
                List<ByteBuffer> keys = generateKeys(offset, offset + session.getKeysPerCall());
                for (int j = 0; j < session.getSuperColumns(); j++)
                {
                    ColumnParent parent = new ColumnParent("Super1").setSuper_column(("S" + j).getBytes());
                    long start = System.currentTimeMillis();
                    try
                    {
                        results = client.multiget_slice(keys, parent, predicate, session.getConsistencyLevel());
                        if (results.size() == 0)
                        {
                            System.err.printf("Keys %s were not found.%n", keys);
                            if (!session.ignoreErrors())
                                break;
                        }
                    }
                    catch (Exception e)
                    {
                        System.err.printf("Error on multiget_slice call - %s%n", getExceptionMessage(e));
                        if (!session.ignoreErrors())
                            return;
                    }
                    session.operationCount.getAndIncrement(index);
                    session.keyCount.getAndAdd(index, keys.size());
                    session.latencies.getAndAdd(index, System.currentTimeMillis() - start);
                    offset += session.getKeysPerCall();
                }
            }
        }
        else
        {
            ColumnParent parent = new ColumnParent("Standard1");
            for (int i = 0; i < count; i++)
            {
                List<ByteBuffer> keys = generateKeys(offset, offset + session.getKeysPerCall());
                long start = System.currentTimeMillis();
                try
                {
                    results = client.multiget_slice(keys, parent, predicate, session.getConsistencyLevel());
                    if (results.size() == 0)
                    {
                        System.err.printf("Keys %s were not found.%n", keys);
                        if (!session.ignoreErrors())
                            break;
                    }
                }
                catch (Exception e)
                {
                    System.err.printf("Error on multiget_slice call - %s%n", getExceptionMessage(e));
                    if (!session.ignoreErrors())
                        return;
                }
                session.operationCount.getAndIncrement(index);
                session.keyCount.getAndAdd(index, keys.size());
                session.latencies.getAndAdd(index, System.currentTimeMillis() - start);
                offset += session.getKeysPerCall();
            }
        }
    }
    private List<ByteBuffer> generateKeys(int start, int limit)
    {
        List<ByteBuffer> keys = new ArrayList<ByteBuffer>();
        for (int i = start; i < limit; i++)
        {
            keys.add(ByteBuffer.wrap(generateKey()));
        }
        return keys;
    }
}
