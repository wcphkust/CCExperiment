package org.apache.cassandra.db;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.io.util.DataOutputBuffer;
import org.apache.cassandra.utils.ByteBufferUtil;
public class Column implements IColumn
{
    private static Logger logger = LoggerFactory.getLogger(Column.class);
    public static ColumnSerializer serializer()
    {
        return new ColumnSerializer();
    }
    protected final ByteBuffer name;
    protected final ByteBuffer value;
    protected final long timestamp;
    Column(ByteBuffer name)
    {
        this(name, ByteBufferUtil.EMPTY_BYTE_BUFFER);
    }
    public Column(ByteBuffer name, ByteBuffer value)
    {
        this(name, value, 0);
    }
    public Column(ByteBuffer name, ByteBuffer value, long timestamp)
    {
        assert name != null;
        assert value != null;
        assert name.remaining() <= IColumn.MAX_NAME_LENGTH;
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
    }
    public ByteBuffer name()
    {
        return name;
    }
    public Column getSubColumn(ByteBuffer columnName)
    {
        throw new UnsupportedOperationException("This operation is unsupported on simple columns.");
    }
    public ByteBuffer value()
    {
        return value;
    }
    public Collection<IColumn> getSubColumns()
    {
        throw new UnsupportedOperationException("This operation is unsupported on simple columns.");
    }
    public long timestamp()
    {
        return timestamp;
    }
    public boolean isMarkedForDelete()
    {
        return false;
    }
    public long getMarkedForDeleteAt()
    {
        throw new IllegalStateException("column is not marked for delete");
    }
    public long mostRecentLiveChangeAt()
    {
        return timestamp;
    }
    public int size()
    {
        return DBConstants.shortSize_ + name.remaining() + DBConstants.boolSize_ + DBConstants.tsSize_ + DBConstants.intSize_ + value.remaining();
    }
    public int serializedSize()
    {
        return size();
    }
    public void addColumn(IColumn column)
    {
        throw new UnsupportedOperationException("This operation is not supported for simple columns.");
    }
    public IColumn diff(IColumn column)
    {
        if (timestamp() < column.timestamp())
        {
            return column;
        }
        return null;
    }
    public void updateDigest(MessageDigest digest)
    {
        digest.update(name.duplicate());
        digest.update(value.duplicate());
        DataOutputBuffer buffer = new DataOutputBuffer();
        try
        {
            buffer.writeLong(timestamp);
            buffer.writeByte((isMarkedForDelete()) ? ColumnSerializer.DELETION_MASK : 0);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        digest.update(buffer.getData(), 0, buffer.getLength());
    }
    public int getLocalDeletionTime()
    {
        throw new IllegalStateException("column is not marked for delete");
    }
    public IColumn reconcile(IColumn column)
    {
        if (isMarkedForDelete())
            return timestamp() < column.timestamp() ? column : this;
        if (column.isMarkedForDelete())
            return timestamp() > column.timestamp() ? this : column;
        if (timestamp() == column.timestamp())
            return value().compareTo(column.value()) < 0 ? column : this;
        return timestamp() < column.timestamp() ? column : this;
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Column column = (Column)o;
        if (timestamp != column.timestamp)
            return false;
        if (!name.equals(column.name))
            return false;
        return value.equals(column.value);
    }
    @Override
    public int hashCode()
    {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (int)(timestamp ^ (timestamp >>> 32));
        return result;
    }
    public IColumn deepCopy()
    {
        return new Column(ByteBufferUtil.clone(name), ByteBufferUtil.clone(value), timestamp);
    }
    public String getString(AbstractType comparator)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(comparator.getString(name));
        sb.append(":");
        sb.append(isMarkedForDelete());
        sb.append(":");
        sb.append(value.remaining());
        sb.append("@");
        sb.append(timestamp());
        return sb.toString();
    }
    public boolean isLive()
    {
        return !isMarkedForDelete();
    }
}
