package org.apache.solr.analysis;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.CharStream;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.Tokenizer;
import java.io.Reader;
import java.io.IOException;
public class TokenizerChain extends SolrAnalyzer {
  final private CharFilterFactory[] charFilters;
  final private TokenizerFactory tokenizer;
  final private TokenFilterFactory[] filters;
  public TokenizerChain(TokenizerFactory tokenizer, TokenFilterFactory[] filters) {
    this(null,tokenizer,filters);
  }
  public TokenizerChain(CharFilterFactory[] charFilters, TokenizerFactory tokenizer, TokenFilterFactory[] filters) {
    this.charFilters = charFilters;
    this.tokenizer = tokenizer;
    this.filters = filters;
  }
  public CharFilterFactory[] getCharFilterFactories() { return charFilters; }
  public TokenizerFactory getTokenizerFactory() { return tokenizer; }
  public TokenFilterFactory[] getTokenFilterFactories() { return filters; }
  @Override
  public Reader charStream(Reader reader){
    if( charFilters != null && charFilters.length > 0 ){
      CharStream cs = CharReader.get( reader );
      for (int i=0; i<charFilters.length; i++) {
        cs = charFilters[i].create(cs);
      }
      reader = cs;
    }
    return reader;
  }
  @Override
  public TokenStreamInfo getStream(String fieldName, Reader reader) {
    Tokenizer tk = (Tokenizer)tokenizer.create(charStream(reader));
    TokenStream ts = tk;
    for (int i=0; i<filters.length; i++) {
      ts = filters[i].create(ts);
    }
    return new TokenStreamInfo(tk,ts);
  }
  public String toString() {
    StringBuilder sb = new StringBuilder("TokenizerChain(");
    for (CharFilterFactory filter: charFilters) {
      sb.append(filter);
      sb.append(", ");
    }
    sb.append(tokenizer);
    for (TokenFilterFactory filter: filters) {
      sb.append(", ");
      sb.append(filter);
    }
    sb.append(')');
    return sb.toString();
  }
}
