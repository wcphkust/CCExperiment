package org.apache.lucene.search.vectorhighlight;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
public class FieldTermStack {
  private final String fieldName;
  LinkedList<TermInfo> termList = new LinkedList<TermInfo>();
  public static void main( String[] args ) throws Exception {
    Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_CURRENT);
    QueryParser parser = new QueryParser(Version.LUCENE_CURRENT,  "f", analyzer );
    Query query = parser.parse( "a x:b" );
    FieldQuery fieldQuery = new FieldQuery( query, true, false );
    Directory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer));
    Document doc = new Document();
    doc.add( new Field( "f", "a a a b b c a b b c d e f", Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS ) );
    doc.add( new Field( "f", "b a b a f", Store.YES, Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS ) );
    writer.addDocument( doc );
    writer.close();
    IndexReader reader = IndexReader.open( dir, true );
    new FieldTermStack( reader, 0, "f", fieldQuery );
    reader.close();
  }
  public FieldTermStack( IndexReader reader, int docId, String fieldName, final FieldQuery fieldQuery ) throws IOException {
    this.fieldName = fieldName;
    TermFreqVector tfv = reader.getTermFreqVector( docId, fieldName );
    if( tfv == null ) return; 
    TermPositionVector tpv = null;
    try{
      tpv = (TermPositionVector)tfv;
    }
    catch( ClassCastException e ){
      return; 
    }
    Set<String> termSet = fieldQuery.getTermSet( fieldName );
    if( termSet == null ) return;
    for( String term : tpv.getTerms() ){
      if( !termSet.contains( term ) ) continue;
      int index = tpv.indexOf( term );
      TermVectorOffsetInfo[] tvois = tpv.getOffsets( index );
      if( tvois == null ) return; 
      int[] poss = tpv.getTermPositions( index );
      if( poss == null ) return; 
      for( int i = 0; i < tvois.length; i++ )
        termList.add( new TermInfo( term, tvois[i].getStartOffset(), tvois[i].getEndOffset(), poss[i] ) );
    }
    Collections.sort( termList );
  }
  public String getFieldName(){
    return fieldName;
  }
  public TermInfo pop(){
    return termList.poll();
  }
  public void push( TermInfo termInfo ){
    termList.addFirst( termInfo );
  }
  public boolean isEmpty(){
    return termList == null || termList.size() == 0;
  }
  public static class TermInfo implements Comparable<TermInfo>{
    final String text;
    final int startOffset;
    final int endOffset;
    final int position;
    TermInfo( String text, int startOffset, int endOffset, int position ){
      this.text = text;
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.position = position;
    }
    public String getText(){ return text; }
    public int getStartOffset(){ return startOffset; }
    public int getEndOffset(){ return endOffset; }
    public int getPosition(){ return position; }
    @Override
    public String toString(){
      StringBuilder sb = new StringBuilder();
      sb.append( text ).append( '(' ).append(startOffset).append( ',' ).append( endOffset ).append( ',' ).append( position ).append( ')' );
      return sb.toString();
    }
    public int compareTo( TermInfo o ) {
      return ( this.position - o.position );
    }
  }
}
