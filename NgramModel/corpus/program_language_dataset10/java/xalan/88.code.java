package org.apache.xalan.lib.sql;
import java.util.Vector;
import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xpath.XPathContext;
import org.apache.xml.dtm.DTMManager;
import org.apache.xml.dtm.DTM;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.*;
import org.apache.xml.dtm.ref.*;
public class SQLDocument extends DTMDocument
{
  private boolean DEBUG = false;
  private static final String S_NAMESPACE = "http://xml.apache.org/xalan/SQLExtension";
  private static final String S_SQL = "sql";
  private static final String S_ROW_SET = "row-set";
  private static final String S_METADATA = "metadata";
  private static final String S_COLUMN_HEADER = "column-header";
  private static final String S_ROW = "row";
  private static final String S_COL = "col";
  private static final String S_OUT_PARAMETERS = "out-parameters";
  private static final String S_CATALOGUE_NAME = "catalogue-name";
  private static final String S_DISPLAY_SIZE = "column-display-size";
  private static final String S_COLUMN_LABEL = "column-label";
  private static final String S_COLUMN_NAME = "column-name";
  private static final String S_COLUMN_TYPE = "column-type";
  private static final String S_COLUMN_TYPENAME = "column-typename";
  private static final String S_PRECISION = "precision";
  private static final String S_SCALE = "scale";
  private static final String S_SCHEMA_NAME = "schema-name";
  private static final String S_TABLE_NAME = "table-name";
  private static final String S_CASESENSITIVE = "case-sensitive";
  private static final String S_DEFINITELYWRITABLE = "definitely-writable";
  private static final String S_ISNULLABLE = "nullable";
  private static final String S_ISSIGNED = "signed";
  private static final String S_ISWRITEABLE = "writable";
  private static final String S_ISSEARCHABLE = "searchable";
  private int m_SQL_TypeID = 0;
  private int m_MetaData_TypeID = 0;
  private int m_ColumnHeader_TypeID = 0;
  private int m_RowSet_TypeID = 0;
  private int m_Row_TypeID = 0;
  private int m_Col_TypeID = 0;
  private int m_OutParameter_TypeID = 0;
  private int m_ColAttrib_CATALOGUE_NAME_TypeID = 0;
  private int m_ColAttrib_DISPLAY_SIZE_TypeID = 0;
  private int m_ColAttrib_COLUMN_LABEL_TypeID = 0;
  private int m_ColAttrib_COLUMN_NAME_TypeID = 0;
  private int m_ColAttrib_COLUMN_TYPE_TypeID = 0;
  private int m_ColAttrib_COLUMN_TYPENAME_TypeID = 0;
  private int m_ColAttrib_PRECISION_TypeID = 0;
  private int m_ColAttrib_SCALE_TypeID = 0;
  private int m_ColAttrib_SCHEMA_NAME_TypeID = 0;
  private int m_ColAttrib_TABLE_NAME_TypeID = 0;
  private int m_ColAttrib_CASESENSITIVE_TypeID = 0;
  private int m_ColAttrib_DEFINITELYWRITEABLE_TypeID = 0;
  private int m_ColAttrib_ISNULLABLE_TypeID = 0;
  private int m_ColAttrib_ISSIGNED_TypeID = 0;
  private int m_ColAttrib_ISWRITEABLE_TypeID = 0;
  private int m_ColAttrib_ISSEARCHABLE_TypeID = 0;
  private Statement m_Statement = null;
  private ExpressionContext m_ExpressionContext = null;
  private ConnectionPool m_ConnectionPool = null;
  private ResultSet m_ResultSet = null;
  private SQLQueryParser m_QueryParser = null;
  private int[] m_ColHeadersIdx;
  private int m_ColCount;
  private int m_MetaDataIdx = DTM.NULL;
  private int m_RowSetIdx = DTM.NULL;
  private int m_SQLIdx = DTM.NULL;
  private int m_FirstRowIdx = DTM.NULL;
  private int m_LastRowIdx = DTM.NULL;
  private boolean m_StreamingMode = true;
  private boolean m_MultipleResults = false;
  private boolean m_HasErrors = false;
  private boolean m_IsStatementCachingEnabled = false;
  private XConnection m_XConnection = null;
  public SQLDocument(DTMManager mgr, int ident)
  {
    super(mgr, ident);
  }
  public static SQLDocument getNewDocument(ExpressionContext exprContext)
  {
    DTMManager mgr =
      ((XPathContext.XPathExpressionContext)exprContext).getDTMManager();
    DTMManagerDefault  mgrDefault = (DTMManagerDefault) mgr;
    int dtmIdent = mgrDefault.getFirstFreeDTMID();
    SQLDocument doc =
      new SQLDocument(mgr, dtmIdent << DTMManager.IDENT_DTM_NODE_BITS);
    mgrDefault.addDTM(doc, dtmIdent);
    doc.setExpressionContext(exprContext);
    return doc;
  }
  protected void setExpressionContext(ExpressionContext expr)
  {
    m_ExpressionContext = expr;
  }
  public ExpressionContext getExpressionContext()
  {
    return m_ExpressionContext;
  }
  public void execute(XConnection xconn, SQLQueryParser query)
    throws SQLException
  {
    try
    {
      m_StreamingMode = "true".equals(xconn.getFeature("streaming"));
      m_MultipleResults = "true".equals(xconn.getFeature("multiple-results"));
      m_IsStatementCachingEnabled = "true".equals(xconn.getFeature("cache-statements"));
      m_XConnection = xconn;
      m_QueryParser = query;
      executeSQLStatement();
      createExpandedNameTable();
      m_DocumentIdx = addElement(0, m_Document_TypeID, DTM.NULL, DTM.NULL);
      m_SQLIdx = addElement(1, m_SQL_TypeID,  m_DocumentIdx, DTM.NULL);
      if ( ! m_MultipleResults )
        extractSQLMetaData(m_ResultSet.getMetaData());
    }
    catch(SQLException e)
    {
      m_HasErrors = true;
      throw e;
    }
  }
  private void executeSQLStatement() throws SQLException
  {
    m_ConnectionPool = m_XConnection.getConnectionPool();
    Connection conn = m_ConnectionPool.getConnection();
    if (! m_QueryParser.hasParameters() )
    {
      m_Statement = conn.createStatement();
      m_ResultSet = m_Statement.executeQuery(m_QueryParser.getSQLQuery());
    }
    else if (m_QueryParser.isCallable())
    {
      CallableStatement cstmt =
        conn.prepareCall(m_QueryParser.getSQLQuery());
      m_QueryParser.registerOutputParameters(cstmt);
      m_QueryParser.populateStatement(cstmt, m_ExpressionContext);
      m_Statement = cstmt;
      if (! cstmt.execute()) throw new SQLException("Error in Callable Statement");
      m_ResultSet = m_Statement.getResultSet();
    }
    else
    {
      PreparedStatement stmt =
        conn.prepareStatement(m_QueryParser.getSQLQuery());
      m_QueryParser.populateStatement(stmt, m_ExpressionContext);
      m_Statement = stmt;
      m_ResultSet = stmt.executeQuery();
    }
  }
  public void skip( int value )
  {
    try
    {
      if (m_ResultSet != null) m_ResultSet.relative(value);
    }
    catch(Exception origEx)
    {
      try
      {
        for (int x=0; x<value; x++)
        {
          if (! m_ResultSet.next()) break;
        }
      }
      catch(Exception e)
      {
        m_XConnection.setError(origEx, this, checkWarnings());
        m_XConnection.setError(e, this, checkWarnings());
      }
    }
  }
  private void extractSQLMetaData( ResultSetMetaData meta )
  {
    m_MetaDataIdx = addElement(1, m_MetaData_TypeID,  m_MultipleResults ? m_RowSetIdx : m_SQLIdx, DTM.NULL);
    try
    {
      m_ColCount = meta.getColumnCount();
      m_ColHeadersIdx = new int[m_ColCount];
    }
    catch(Exception e)
    {
      m_XConnection.setError(e, this, checkWarnings());
    }
    int lastColHeaderIdx = DTM.NULL;
    int i = 1;
    for (i=1; i<= m_ColCount; i++)
    {
      m_ColHeadersIdx[i-1] =
        addElement(2,m_ColumnHeader_TypeID, m_MetaDataIdx, lastColHeaderIdx);
      lastColHeaderIdx = m_ColHeadersIdx[i-1];
      try
      {
        addAttributeToNode(
          meta.getColumnName(i),
          m_ColAttrib_COLUMN_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_NAME_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.getColumnLabel(i),
          m_ColAttrib_COLUMN_LABEL_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_LABEL_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.getCatalogName(i),
          m_ColAttrib_CATALOGUE_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_CATALOGUE_NAME_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          new Integer(meta.getColumnDisplaySize(i)),
          m_ColAttrib_DISPLAY_SIZE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_DISPLAY_SIZE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          new Integer(meta.getColumnType(i)),
          m_ColAttrib_COLUMN_TYPE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_TYPE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.getColumnTypeName(i),
          m_ColAttrib_COLUMN_TYPENAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_COLUMN_TYPENAME_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          new Integer(meta.getPrecision(i)),
          m_ColAttrib_PRECISION_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_PRECISION_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          new Integer(meta.getScale(i)),
          m_ColAttrib_SCALE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_SCALE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.getSchemaName(i),
          m_ColAttrib_SCHEMA_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_SCHEMA_NAME_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.getTableName(i),
          m_ColAttrib_TABLE_NAME_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_TABLE_NAME_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.isCaseSensitive(i) ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_CASESENSITIVE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_CASESENSITIVE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.isDefinitelyWritable(i) ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_DEFINITELYWRITEABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_DEFINITELYWRITEABLE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.isNullable(i) != 0 ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISNULLABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISNULLABLE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.isSigned(i) ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISSIGNED_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISSIGNED_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.isWritable(i) == true ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISWRITEABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISWRITEABLE_TypeID, lastColHeaderIdx);
      }
      try
      {
        addAttributeToNode(
          meta.isSearchable(i) == true ? S_ISTRUE : S_ISFALSE,
          m_ColAttrib_ISSEARCHABLE_TypeID, lastColHeaderIdx);
      }
      catch(Exception e)
      {
        addAttributeToNode(
          S_ATTRIB_NOT_SUPPORTED,
          m_ColAttrib_ISSEARCHABLE_TypeID, lastColHeaderIdx);
      }
    }
  }
  protected void createExpandedNameTable( )
  {
    super.createExpandedNameTable();
    m_SQL_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SQL, DTM.ELEMENT_NODE);
    m_MetaData_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_METADATA, DTM.ELEMENT_NODE);
    m_ColumnHeader_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_HEADER, DTM.ELEMENT_NODE);
    m_RowSet_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ROW_SET, DTM.ELEMENT_NODE);
    m_Row_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ROW, DTM.ELEMENT_NODE);
    m_Col_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COL, DTM.ELEMENT_NODE);
    m_OutParameter_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_OUT_PARAMETERS, DTM.ELEMENT_NODE);
    m_ColAttrib_CATALOGUE_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_CATALOGUE_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_DISPLAY_SIZE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_DISPLAY_SIZE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_LABEL_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_LABEL, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_TYPE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_TYPE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_COLUMN_TYPENAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_COLUMN_TYPENAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_PRECISION_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_PRECISION, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_SCALE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SCALE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_SCHEMA_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_SCHEMA_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_TABLE_NAME_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_TABLE_NAME, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_CASESENSITIVE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_CASESENSITIVE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_DEFINITELYWRITEABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_DEFINITELYWRITABLE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISNULLABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISNULLABLE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISSIGNED_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISSIGNED, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISWRITEABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISWRITEABLE, DTM.ATTRIBUTE_NODE);
    m_ColAttrib_ISSEARCHABLE_TypeID =
      m_expandedNameTable.getExpandedTypeID(S_NAMESPACE, S_ISSEARCHABLE, DTM.ATTRIBUTE_NODE);
  }
  private boolean addRowToDTMFromResultSet( )
  {
    try
    {
      if (m_FirstRowIdx == DTM.NULL)
      {
        m_RowSetIdx =
          addElement(1, m_RowSet_TypeID,  m_SQLIdx, m_MultipleResults ? m_RowSetIdx : m_MetaDataIdx);
        if ( m_MultipleResults ) extractSQLMetaData(m_ResultSet.getMetaData());
      }
      if ( ! m_ResultSet.next())
      {
        if (m_StreamingMode && (m_LastRowIdx != DTM.NULL))
        {
          m_nextsib.setElementAt(DTM.NULL, m_LastRowIdx);
        }
        m_ResultSet.close();
        if ( m_MultipleResults )
        {
          while ( !m_Statement.getMoreResults() && m_Statement.getUpdateCount() >= 0 ) ;
          m_ResultSet = m_Statement.getResultSet();
        }
        else
          m_ResultSet = null;
        if ( m_ResultSet != null )
        {
          m_FirstRowIdx = DTM.NULL;
          addRowToDTMFromResultSet();
        }
        else
        {
          Vector parameters = m_QueryParser.getParameters();
          if ( parameters != null )
          {
            int outParamIdx = addElement(1, m_OutParameter_TypeID,  m_SQLIdx, m_RowSetIdx);
            int lastColID = DTM.NULL;
            for ( int indx = 0 ; indx < parameters.size() ; indx++ )
            {
              QueryParameter parm = (QueryParameter)parameters.elementAt(indx);
              if ( parm.isOutput() )
              {
                Object rawobj = ((CallableStatement)m_Statement).getObject(indx + 1);
                lastColID = addElementWithData(rawobj, 2, m_Col_TypeID, outParamIdx, lastColID);
                addAttributeToNode(parm.getName(), m_ColAttrib_COLUMN_NAME_TypeID, lastColID);
                addAttributeToNode(parm.getName(), m_ColAttrib_COLUMN_LABEL_TypeID, lastColID);
                addAttributeToNode(new Integer(parm.getType()), m_ColAttrib_COLUMN_TYPE_TypeID, lastColID);
                addAttributeToNode(parm.getTypeName(), m_ColAttrib_COLUMN_TYPENAME_TypeID, lastColID);
              }
            }
          }
          SQLWarning warn = checkWarnings();
          if ( warn != null )	m_XConnection.setError(null, null, warn);
        }
        return false;
      }
      if (m_FirstRowIdx == DTM.NULL)
      {
        m_FirstRowIdx =
          addElement(2, m_Row_TypeID, m_RowSetIdx, m_MultipleResults ? m_MetaDataIdx : DTM.NULL);
        m_LastRowIdx = m_FirstRowIdx;
        if (m_StreamingMode)
        {
          m_nextsib.setElementAt(m_LastRowIdx, m_LastRowIdx);
        }
      }
      else
      {
        if (! m_StreamingMode)
        {
          m_LastRowIdx = addElement(2, m_Row_TypeID, m_RowSetIdx, m_LastRowIdx);
        }
      }
      int colID = _firstch(m_LastRowIdx);
      int pcolID = DTM.NULL;
      for (int i=1; i<= m_ColCount; i++)
      {
        Object o = m_ResultSet.getObject(i);
        if (colID == DTM.NULL)
        {
          pcolID = addElementWithData(o,3,m_Col_TypeID, m_LastRowIdx, pcolID);
          cloneAttributeFromNode(pcolID, m_ColHeadersIdx[i-1]);
        }
        else
        {
          int dataIdent = _firstch(colID);
          if (dataIdent == DTM.NULL)
          {
            error("Streaming Mode, Data Error");
          }
          else
          {
            m_ObjectArray.setAt(dataIdent, o);
          }
        } 
        if (colID != DTM.NULL)
        {
          colID = _nextsib(colID);
        }
      } 
    }
    catch(Exception e)
    {
      if (DEBUG)
      {
        System.out.println(
          "SQL Error Fetching next row [" + e.getLocalizedMessage() + "]");
      }
      m_XConnection.setError(e, this, checkWarnings());
      m_HasErrors = true;
    }
    return true;
  }
  public boolean hasErrors()
  {
    return m_HasErrors;
  }
  public void close(boolean flushConnPool )
  {
    try
    {
      SQLWarning warn = checkWarnings();
      if ( warn != null ) m_XConnection.setError(null, null, warn);
    }
    catch(Exception e) {}
    try
    {
      if (null != m_ResultSet)
      {
        m_ResultSet.close();
        m_ResultSet = null;
      }
    }
    catch(Exception e) {}
    Connection conn = null;
    try
    {
      if (null != m_Statement)
      {
        conn = m_Statement.getConnection();
        m_Statement.close();
        m_Statement = null;
      }
    }
    catch(Exception e) {}
    try
    {
      if (conn != null)
      {
        if (m_HasErrors)  m_ConnectionPool.releaseConnectionOnError(conn);
        else m_ConnectionPool.releaseConnection(conn);
      }
    }
    catch(Exception e) {}
    getManager().release(this, true);
  }
  protected boolean nextNode( )
  {
    if (DEBUG) System.out.println("nextNode()");
    try
    {
      return false;
    }
    catch(Exception e)
    {
      return false;
    }
  }
  protected int _nextsib( int identity )
  {
  if ( m_ResultSet != null )
  {
      int id = _exptype(identity);
      if (m_FirstRowIdx == DTM.NULL)
      {
        addRowToDTMFromResultSet();
      }
      if (
        ( id == m_Row_TypeID) &&
        (identity >= m_LastRowIdx) )
      {
        if (DEBUG) System.out.println("reading from the ResultSet");
        addRowToDTMFromResultSet();
      }
      else if ( m_MultipleResults && identity == m_RowSetIdx )
      {
        if (DEBUG) System.out.println("reading for next ResultSet");
      int startIdx = m_RowSetIdx;
      while ( startIdx == m_RowSetIdx && m_ResultSet != null )
            addRowToDTMFromResultSet();
      }
  }
    return super._nextsib(identity);
  }
  public void documentRegistration()
  {
    if (DEBUG) System.out.println("Document Registration");
  }
  public void documentRelease()
  {
    if (DEBUG) System.out.println("Document Release");
  }
  public SQLWarning checkWarnings()
  {
    SQLWarning warn = null;
    if ( m_Statement != null )
    {
      try
      {
        warn = m_Statement.getWarnings();
        m_Statement.clearWarnings();
      }
      catch (SQLException se) {}
    }
    return(warn);
  }
}
