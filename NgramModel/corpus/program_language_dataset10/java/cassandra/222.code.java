package org.apache.cassandra.dht;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
public abstract class AbstractByteOrderedPartitioner implements IPartitioner<BytesToken>
{
    public static final BytesToken MINIMUM = new BytesToken(ArrayUtils.EMPTY_BYTE_ARRAY);
    public static final BigInteger BYTE_MASK = new BigInteger("255");
    public DecoratedKey<BytesToken> decorateKey(ByteBuffer key)
    {
        return new DecoratedKey<BytesToken>(getToken(key), key);
    }
    public DecoratedKey<BytesToken> convertFromDiskFormat(ByteBuffer key)
    {
        return new DecoratedKey<BytesToken>(getToken(key), key);
    }
    public BytesToken midpoint(Token ltoken, Token rtoken)
    {
        int ll,rl;
        ByteBuffer lb,rb;
        if(ltoken.token instanceof byte[])
        {
            ll = ((byte[])ltoken.token).length;
            lb = ByteBuffer.wrap(((byte[])ltoken.token));
        }
        else
        {
            ll = ((ByteBuffer)ltoken.token).remaining();
            lb = (ByteBuffer)ltoken.token;
        }
        if(rtoken.token instanceof byte[])
        {
            rl = ((byte[])rtoken.token).length;
            rb = ByteBuffer.wrap(((byte[])rtoken.token));
        }
        else
        {
            rl = ((ByteBuffer)rtoken.token).remaining();
            rb = (ByteBuffer)rtoken.token;
        }
        int sigbytes = Math.max(ll, rl);
        BigInteger left = bigForBytes(lb, sigbytes);
        BigInteger right = bigForBytes(rb, sigbytes);
        Pair<BigInteger,Boolean> midpair = FBUtilities.midpoint(left, right, 8*sigbytes);
        return new BytesToken(bytesForBig(midpair.left, sigbytes, midpair.right));
    }
    private BigInteger bigForBytes(ByteBuffer bytes, int sigbytes)
    {
        byte[] b = new byte[sigbytes];
        Arrays.fill(b, (byte) 0); 
        ByteBufferUtil.arrayCopy(bytes, bytes.position(), b, 0, bytes.remaining());
        return new BigInteger(1, b);
    }
    private byte[] bytesForBig(BigInteger big, int sigbytes, boolean remainder)
    {
        byte[] bytes = new byte[sigbytes + (remainder ? 1 : 0)];
        if (remainder)
        {
            bytes[sigbytes] |= 0x80;
        }
        for (int i = 0; i < sigbytes; i++)
        {
            int maskpos = 8 * (sigbytes - (i + 1));
            bytes[i] = (byte)(big.and(BYTE_MASK.shiftLeft(maskpos)).shiftRight(maskpos).intValue() & 0xFF);
        }
        return bytes;
    }
    public BytesToken getMinimumToken()
    {
        return MINIMUM;
    }
    public BytesToken getRandomToken()
    {
        Random r = new Random();
        byte[] buffer = new byte[16];
        r.nextBytes(buffer);
        return new BytesToken(buffer);
    }
    private final Token.TokenFactory<byte[]> tokenFactory = new Token.TokenFactory<byte[]>() {
        public ByteBuffer toByteArray(Token<byte[]> bytesToken)
        {
            return ByteBuffer.wrap(bytesToken.token);
        }
        public Token<byte[]> fromByteArray(ByteBuffer bytes)
        {
            return new BytesToken(bytes);
        }
        public String toString(Token<byte[]> bytesToken)
        {
            return FBUtilities.bytesToHex(bytesToken.token);
        }
        public Token<byte[]> fromString(String string)
        {
            return new BytesToken(FBUtilities.hexToBytes(string));
        }
    };
    public Token.TokenFactory<byte[]> getTokenFactory()
    {
        return tokenFactory;
    }
    public boolean preservesOrder()
    {
        return true;
    }
    public abstract BytesToken getToken(ByteBuffer key);
    public Map<Token, Float> describeOwnership(List<Token> sortedTokens)
    {
        Map<Token, Float> allTokens = new HashMap<Token, Float>();
        List<Range> sortedRanges = new ArrayList<Range>();
        Token lastToken = sortedTokens.get(sortedTokens.size() - 1);
        for (Token node : sortedTokens)
        {
            allTokens.put(node, new Float(0.0));
            sortedRanges.add(new Range(lastToken, node));
            lastToken = node;
        }
        for (String ks : DatabaseDescriptor.getTables())
        {
            for (CFMetaData cfmd : DatabaseDescriptor.getKSMetaData(ks).cfMetaData().values())
            {
                for (Range r : sortedRanges)
                {
                    allTokens.put(r.right, allTokens.get(r.right) + StorageService.instance.getSplits(ks, cfmd.cfName, r, 1).size());
                }
            }
        }
        Float total = new Float(0.0);
        for (Float f : allTokens.values())
            total += f;
        for (Map.Entry<Token, Float> row : allTokens.entrySet())
            allTokens.put(row.getKey(), row.getValue() / total);
        return allTokens;
    }
}
