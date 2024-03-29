package org.apache.solr.analysis;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import java.io.Reader;
public class WhitespaceTokenizerFactory extends BaseTokenizerFactory {
  public WhitespaceTokenizer create(Reader input) {
    assureMatchVersion();
    return new WhitespaceTokenizer(luceneMatchVersion,input);
  }
}
