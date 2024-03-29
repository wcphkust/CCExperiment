package org.apache.cassandra.cql;
import java.nio.ByteBuffer;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
public class Term
{
    private final String text;
    private final TermType type;
    public Term(String text, int type)
    {
        this.text = text;
        this.type = TermType.forInt(type);
    }
    protected Term()
    {
        this.text = "";
        this.type = TermType.STRING;
    }
    public String getText()
    {
        return text;
    }
    public ByteBuffer getByteBuffer()
    {
        switch (type)
        {
            case STRING:
                return ByteBuffer.wrap(text.getBytes());
            case LONG:
                return ByteBufferUtil.bytes(Long.parseLong(text));
        }
        return null;
    }
    public TermType getType()
    {
        return type;
    }
    public String toString()
    {
        return String.format("Term(%s, type=%s)", getText(), type);
    }
}
enum TermType
{
    STRING, LONG;
    static TermType forInt(int type)
    {
        if (type == CqlParser.STRING_LITERAL)
            return STRING;
        else if (type == CqlParser.LONG)
            return LONG;
        return null;
    }
}
