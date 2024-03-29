package org.apache.solr.servlet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.core.Config;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.ServletSolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryRequestBase;
public class SolrRequestParsers 
{
  final Logger log = LoggerFactory.getLogger(SolrRequestParsers.class);
  public static final String MULTIPART = "multipart";
  public static final String RAW = "raw";
  public static final String SIMPLE = "simple";
  public static final String STANDARD = "standard";
  private HashMap<String, SolrRequestParser> parsers;
  private boolean enableRemoteStreams = false;
  private boolean handleSelect = true;
  private StandardRequestParser standard;
  public SolrRequestParsers( Config globalConfig )
  {
    long uploadLimitKB = 1048;  
    if( globalConfig == null ) {
      uploadLimitKB = Long.MAX_VALUE; 
      enableRemoteStreams = true;
      handleSelect = true;
    }
    else {
      uploadLimitKB = globalConfig.getInt( 
          "requestDispatcher/requestParsers/@multipartUploadLimitInKB", (int)uploadLimitKB );
      enableRemoteStreams = globalConfig.getBool( 
          "requestDispatcher/requestParsers/@enableRemoteStreaming", false ); 
      handleSelect = globalConfig.getBool( 
          "requestDispatcher/@handleSelect", handleSelect ); 
    }
    MultipartRequestParser multi = new MultipartRequestParser( uploadLimitKB );
    RawRequestParser raw = new RawRequestParser();
    standard = new StandardRequestParser( multi, raw );
    parsers = new HashMap<String, SolrRequestParser>();
    parsers.put( MULTIPART, multi );
    parsers.put( RAW, raw );
    parsers.put( SIMPLE, new SimpleRequestParser() );
    parsers.put( STANDARD, standard );
    parsers.put( "", standard );
  }
  public SolrQueryRequest parse( SolrCore core, String path, HttpServletRequest req ) throws Exception
  {
    SolrRequestParser parser = standard;
    ArrayList<ContentStream> streams = new ArrayList<ContentStream>(1);
    SolrParams params = parser.parseParamsAndFillStreams( req, streams );
    SolrQueryRequest sreq = buildRequestFrom( core, params, streams );
    sreq.getContext().put( "path", path );
    return sreq;
  }
  public SolrQueryRequest buildRequestFrom( SolrCore core, SolrParams params, Collection<ContentStream> streams ) throws Exception
  {
    String contentType = params.get( CommonParams.STREAM_CONTENTTYPE );
    String[] strs = params.getParams( CommonParams.STREAM_URL );
    if( strs != null ) {
      if( !enableRemoteStreams ) {
        throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "Remote Streaming is disabled." );
      }
      for( final String url : strs ) {
        ContentStreamBase stream = new ContentStreamBase.URLStream( new URL(url) );
        if( contentType != null ) {
          stream.setContentType( contentType );
        }
        streams.add( stream );
      }
    }
    strs = params.getParams( CommonParams.STREAM_FILE );
    if( strs != null ) {
      if( !enableRemoteStreams ) {
        throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "Remote Streaming is disabled." );
      }
      for( final String file : strs ) {
        ContentStreamBase stream = new ContentStreamBase.FileStream( new File(file) );
        if( contentType != null ) {
          stream.setContentType( contentType );
        }
        streams.add( stream );
      }
    }
    strs = params.getParams( CommonParams.STREAM_BODY );
    if( strs != null ) {
      for( final String body : strs ) {
        ContentStreamBase stream = new ContentStreamBase.StringStream( body );
        if( contentType != null ) {
          stream.setContentType( contentType );
        }
        streams.add( stream );
      }
    }
    SolrQueryRequestBase q = new SolrQueryRequestBase( core, params ) { };
    if( streams != null && streams.size() > 0 ) {
      q.setContentStreams( streams );
    }
    return q;
  }
  public static MultiMapSolrParams parseQueryString(String queryString) 
  {
    Map<String,String[]> map = new HashMap<String, String[]>();
    if( queryString != null && queryString.length() > 0 ) {
      try {
        for( String kv : queryString.split( "&" ) ) {
          int idx = kv.indexOf( '=' );
          if( idx > 0 ) {
            String name = URLDecoder.decode( kv.substring( 0, idx ), "UTF-8");
            String value = URLDecoder.decode( kv.substring( idx+1 ), "UTF-8");
            MultiMapSolrParams.addParam( name, value, map );
          }
          else {
            String name = URLDecoder.decode( kv, "UTF-8" );
            MultiMapSolrParams.addParam( name, "", map );
          }
        }
      }
      catch( UnsupportedEncodingException uex ) {
        throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, uex );
      }
    }
    return new MultiMapSolrParams( map );
  }
  public boolean isHandleSelect() {
    return handleSelect;
  }
  public void setHandleSelect(boolean handleSelect) {
    this.handleSelect = handleSelect;
  }
}
interface SolrRequestParser 
{
  public SolrParams parseParamsAndFillStreams(
    final HttpServletRequest req, ArrayList<ContentStream> streams ) throws Exception;
}
class SimpleRequestParser implements SolrRequestParser
{
  public SolrParams parseParamsAndFillStreams( 
      final HttpServletRequest req, ArrayList<ContentStream> streams ) throws Exception
  {
    return new ServletSolrParams(req);
  }
}
class HttpRequestContentStream extends ContentStreamBase
{
  private final HttpServletRequest req;
  public HttpRequestContentStream( HttpServletRequest req ) throws IOException {
    this.req = req;
    contentType = req.getContentType();
    String v = req.getHeader( "Content-Length" );
    if( v != null ) {
      size = Long.valueOf( v );
    }
  }
  public InputStream getStream() throws IOException {
    return req.getInputStream();
  }
}
class FileItemContentStream extends ContentStreamBase
{
  private final FileItem item;
  public FileItemContentStream( FileItem f )
  {
    item = f;
    contentType = item.getContentType();
    name = item.getName();
    sourceInfo = item.getFieldName();
    size = item.getSize();
  }
  public InputStream getStream() throws IOException {
    return item.getInputStream();
  }
}
class RawRequestParser implements SolrRequestParser
{
  public SolrParams parseParamsAndFillStreams( 
      final HttpServletRequest req, ArrayList<ContentStream> streams ) throws Exception
  {
    streams.add( new HttpRequestContentStream( req ) );
    return SolrRequestParsers.parseQueryString( req.getQueryString() );
  }
}
class MultipartRequestParser implements SolrRequestParser
{
  private long uploadLimitKB;
  public MultipartRequestParser( long limit )
  {
    uploadLimitKB = limit;
  }
  public SolrParams parseParamsAndFillStreams( 
      final HttpServletRequest req, ArrayList<ContentStream> streams ) throws Exception
  {
    if( !ServletFileUpload.isMultipartContent(req) ) {
      throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "Not multipart content! "+req.getContentType() );
    }
    MultiMapSolrParams params = SolrRequestParsers.parseQueryString( req.getQueryString() );
    DiskFileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    upload.setSizeMax( uploadLimitKB*1024 );
    List items = upload.parseRequest(req);
    Iterator iter = items.iterator();
    while (iter.hasNext()) {
        FileItem item = (FileItem) iter.next();
        if (item.isFormField()) {
          MultiMapSolrParams.addParam( 
            item.getFieldName(), 
            item.getString(), params.getMap() );
        }
        else if( item.getSize() > 0 ) { 
          streams.add( new FileItemContentStream( item ) );
        }
    }
    return params;
  }
}
class StandardRequestParser implements SolrRequestParser
{
  MultipartRequestParser multipart;
  RawRequestParser raw;
  StandardRequestParser( MultipartRequestParser multi, RawRequestParser raw ) 
  {
    this.multipart = multi;
    this.raw = raw;
  }
  public SolrParams parseParamsAndFillStreams( 
      final HttpServletRequest req, ArrayList<ContentStream> streams ) throws Exception
  {
    String method = req.getMethod().toUpperCase();
    if( "GET".equals( method ) || "HEAD".equals( method )) {
      return new ServletSolrParams(req);
    }
    if( "POST".equals( method ) ) {
      String contentType = req.getContentType();
      if( contentType != null ) {
        int idx = contentType.indexOf( ';' );
        if( idx > 0 ) { 
          contentType = contentType.substring( 0, idx );
        }
        if( "application/x-www-form-urlencoded".equals( contentType.toLowerCase() ) ) {
          return new ServletSolrParams(req); 
        }
        if( ServletFileUpload.isMultipartContent(req) ) {
          return multipart.parseParamsAndFillStreams(req, streams);
        }
      }
      return raw.parseParamsAndFillStreams(req, streams);
    }
    throw new SolrException( SolrException.ErrorCode.BAD_REQUEST, "Unsupported method: "+method );
  }
}
