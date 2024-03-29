package org.apache.xalan.lib.sql;
import java.util.Hashtable;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.Statement;
public class QueryParameter
{
  private int     m_type;
  private String  m_name;
  private String  m_value;
  private boolean m_output;
  private String  m_typeName;
  private static  Hashtable m_Typetable = null;
  public QueryParameter()
  {
    m_type = -1;
    m_name = null;
    m_value = null;
    m_output = false;
    m_typeName = null;
  }
  public QueryParameter( String v, String t )
  {
    m_name = null;
    m_value = v;
    m_output = false;
    setTypeName(t);
  }
  public QueryParameter( String name, String value, String type, boolean out_flag )
  {
    m_name = name;
    m_value = value;
    m_output = out_flag;
    setTypeName(type);
  }
  public String getValue( ) {
    return m_value;
  }
  public void setValue( String newValue ) {
    m_value = newValue;
  }
  public void setTypeName( String newType )
  {
    m_type = map_type(newType);
    m_typeName = newType;
  }
  public String getTypeName( )
  {
    return m_typeName;
  }
  public int getType( )
  {
    return m_type;
  }
  public String getName()
  {
    return m_name;
  }
  public void setName(String n)
  {
    m_name = n;
  }
  public boolean isOutput()
  {
    return m_output;
  }
  public void setIsOutput(boolean flag)
  {
    m_output = flag;
  }
  private static int map_type(String typename)
  {
    if ( m_Typetable == null )
    {
      m_Typetable = new Hashtable();
      m_Typetable.put("BIGINT", new Integer(java.sql.Types.BIGINT));
      m_Typetable.put("BINARY", new Integer(java.sql.Types.BINARY));
      m_Typetable.put("BIT", new Integer(java.sql.Types.BIT));
      m_Typetable.put("CHAR", new Integer(java.sql.Types.CHAR));
      m_Typetable.put("DATE", new Integer(java.sql.Types.DATE));
      m_Typetable.put("DECIMAL", new Integer(java.sql.Types.DECIMAL));
      m_Typetable.put("DOUBLE", new Integer(java.sql.Types.DOUBLE));
      m_Typetable.put("FLOAT", new Integer(java.sql.Types.FLOAT));
      m_Typetable.put("INTEGER", new Integer(java.sql.Types.INTEGER));
      m_Typetable.put("LONGVARBINARY", new Integer(java.sql.Types.LONGVARBINARY));
      m_Typetable.put("LONGVARCHAR", new Integer(java.sql.Types.LONGVARCHAR));
      m_Typetable.put("NULL", new Integer(java.sql.Types.NULL));
      m_Typetable.put("NUMERIC", new Integer(java.sql.Types.NUMERIC));
      m_Typetable.put("OTHER", new Integer(java.sql.Types.OTHER));
      m_Typetable.put("REAL", new Integer(java.sql.Types.REAL));
      m_Typetable.put("SMALLINT", new Integer(java.sql.Types.SMALLINT));
      m_Typetable.put("TIME", new Integer(java.sql.Types.TIME));
      m_Typetable.put("TIMESTAMP", new Integer(java.sql.Types.TIMESTAMP));
      m_Typetable.put("TINYINT", new Integer(java.sql.Types.TINYINT));
      m_Typetable.put("VARBINARY", new Integer(java.sql.Types.VARBINARY));
      m_Typetable.put("VARCHAR", new Integer(java.sql.Types.VARCHAR));
      m_Typetable.put("STRING", new Integer(java.sql.Types.VARCHAR));
      m_Typetable.put("BIGDECIMAL", new Integer(java.sql.Types.NUMERIC));
      m_Typetable.put("BOOLEAN", new Integer(java.sql.Types.BIT));
      m_Typetable.put("BYTES", new Integer(java.sql.Types.LONGVARBINARY));
      m_Typetable.put("LONG", new Integer(java.sql.Types.BIGINT));
      m_Typetable.put("SHORT", new Integer(java.sql.Types.SMALLINT));
    }
    Integer type = (Integer) m_Typetable.get(typename.toUpperCase());
    int rtype;
    if ( type == null )
      rtype = java.sql.Types.OTHER;
    else
      rtype = type.intValue();
    return(rtype);
  }
}
