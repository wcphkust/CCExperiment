package org.apache.cassandra.io;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
public interface ICompactSerializer<T>
{
    public void serialize(T t, DataOutputStream dos) throws IOException;
    public T deserialize(DataInputStream dis) throws IOException;
}
