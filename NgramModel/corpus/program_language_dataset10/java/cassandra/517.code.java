package org.apache.cassandra.service;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.cassandra.CleanupHelper;
import org.apache.cassandra.Util;
import org.apache.cassandra.concurrent.Stage;
import org.apache.cassandra.concurrent.StageManager;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.*;
import org.apache.cassandra.db.filter.QueryPath;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.io.PrecompactedRow;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.locator.AbstractReplicationStrategy;
import org.apache.cassandra.locator.TokenMetadata;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.MerkleTree;
import static org.apache.cassandra.service.AntiEntropyService.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.apache.cassandra.utils.ByteBufferUtil;
public abstract class AntiEntropyServiceTestAbstract extends CleanupHelper
{
    public AntiEntropyService aes;
    public String tablename;
    public String cfname;
    public TreeRequest request;
    public ColumnFamilyStore store;
    public InetAddress LOCAL, REMOTE;
    private boolean initialized;
    public abstract void init();
    @Before
    public void prepare() throws Exception
    {
        if (!initialized)
        {
            initialized = true;
            init();
            LOCAL = FBUtilities.getLocalAddress();
            StorageService.instance.initServer();
            REMOTE = InetAddress.getByName("127.0.0.2");
            store = null;
            for (ColumnFamilyStore cfs : Table.open(tablename).getColumnFamilyStores())
            {
                if (cfs.columnFamily.equals(cfname))
                {
                    store = cfs;
                    break;
                }
            }
            assert store != null : "CF not found: " + cfname;
        }
        aes = AntiEntropyService.instance;
        TokenMetadata tmd = StorageService.instance.getTokenMetadata();
        tmd.clearUnsafe();
        StorageService.instance.setToken(StorageService.getPartitioner().getRandomToken());
        tmd.updateNormalToken(StorageService.getPartitioner().getMinimumToken(), REMOTE);
        assert tmd.isMember(REMOTE);
        request = new TreeRequest(UUID.randomUUID().toString(), LOCAL, new CFPair(tablename, cfname));
    }
    @After
    public void teardown() throws Exception
    {
        flushAES();
    }
    @Test
    public void testValidatorPrepare() throws Throwable
    {
        Validator validator;
        List<RowMutation> rms = new LinkedList<RowMutation>();
        RowMutation rm;
        rm = new RowMutation(tablename, ByteBufferUtil.bytes("key1"));
        rm.add(new QueryPath(cfname, null, ByteBufferUtil.bytes("Column1")), ByteBufferUtil.bytes("asdf"), 0);
        rms.add(rm);
        Util.writeColumnFamily(rms);
        validator = new Validator(request);
        validator.prepare(store);
        assertTrue(validator.tree.size() > 1);
    }
    @Test
    public void testValidatorComplete() throws Throwable
    {
        Validator validator = new Validator(request);
        validator.prepare(store);
        validator.complete();
        Token min = validator.tree.partitioner().getMinimumToken();
        assert null != validator.tree.hash(new Range(min, min));
    }
    @Test
    public void testValidatorAdd() throws Throwable
    {
        Validator validator = new Validator(request);
        IPartitioner part = validator.tree.partitioner();
        Token min = part.getMinimumToken();
        Token mid = part.midpoint(min, min);
        validator.prepare(store);
        validator.add(new PrecompactedRow(new DecoratedKey(min, ByteBufferUtil.bytes("nonsense!")),
                                       new DataOutputBuffer()));
        validator.add(new PrecompactedRow(new DecoratedKey(mid, ByteBufferUtil.bytes("inconceivable!")),
                                       new DataOutputBuffer()));
        validator.complete();
        assert null != validator.tree.hash(new Range(min, min));
    }
    @Test
    public void testManualRepair() throws Throwable
    {
        AntiEntropyService.RepairSession sess = AntiEntropyService.instance.getRepairSession(tablename, cfname);
        sess.start();
        sess.blockUntilRunning();
        sess.join(100);
        assert sess.isAlive();
        AntiEntropyService.instance.completedRequest(new TreeRequest(sess.getName(), REMOTE, request.cf));
        sess.join();
    }
    @Test
    public void testGetNeighborsPlusOne() throws Throwable
    {
        Set<InetAddress> expected = addTokens(1 + Table.open(tablename).getReplicationStrategy().getReplicationFactor());
        expected.remove(FBUtilities.getLocalAddress());
        assertEquals(expected, AntiEntropyService.getNeighbors(tablename));
    }
    @Test
    public void testGetNeighborsTimesTwo() throws Throwable
    {
        TokenMetadata tmd = StorageService.instance.getTokenMetadata();
        addTokens(2 * Table.open(tablename).getReplicationStrategy().getReplicationFactor());
        AbstractReplicationStrategy ars = Table.open(tablename).getReplicationStrategy();
        Set<InetAddress> expected = new HashSet<InetAddress>();
        for (Range replicaRange : ars.getAddressRanges().get(FBUtilities.getLocalAddress()))
        {
            expected.addAll(ars.getRangeAddresses(tmd).get(replicaRange));
        }
        expected.remove(FBUtilities.getLocalAddress());
        assertEquals(expected, AntiEntropyService.getNeighbors(tablename));
    }
    @Test
    public void testDifferencer() throws Throwable
    {
        Validator validator = new Validator(request);
        validator.prepare(store);
        validator.complete();
        MerkleTree ltree = validator.tree;
        validator = new Validator(request);
        validator.prepare(store);
        validator.complete();
        MerkleTree rtree = validator.tree;
        Token ltoken = StorageService.instance.getLocalToken();
        ltree.invalidate(ltoken);
        MerkleTree.TreeRange changed = ltree.invalids(StorageService.instance.getLocalPrimaryRange()).next();
        changed.hash("non-empty hash!".getBytes());
        Set<Range> interesting = new HashSet<Range>();
        interesting.add(new Range(changed.left, ltoken));
        interesting.add(new Range(ltoken, changed.right));
        Differencer diff = new Differencer(request, ltree, rtree);
        diff.run();
        assertEquals("Wrong differing ranges", interesting, new HashSet<Range>(diff.differences));
    }
    Set<InetAddress> addTokens(int max) throws Throwable
    {
        TokenMetadata tmd = StorageService.instance.getTokenMetadata();
        Set<InetAddress> endpoints = new HashSet<InetAddress>();
        for (int i = 1; i <= max; i++)
        {
            InetAddress endpoint = InetAddress.getByName("127.0.0." + i);
            tmd.updateNormalToken(StorageService.getPartitioner().getRandomToken(), endpoint);
            endpoints.add(endpoint);
        }
        return endpoints;
    }
    void flushAES() throws Exception
    {
        final ThreadPoolExecutor stage = StageManager.getStage(Stage.ANTI_ENTROPY);
        final Callable noop = new Callable<Object>()
        {
            public Boolean call()
            {
                return true;
            }
        };
        stage.submit(noop).get(5000, TimeUnit.MILLISECONDS);
        stage.submit(noop).get(5000, TimeUnit.MILLISECONDS);
    }
}
