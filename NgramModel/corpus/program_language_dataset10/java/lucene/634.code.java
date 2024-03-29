package org.apache.lucene.analysis.compound;
import java.util.Set;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter; 
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Version;
public class DictionaryCompoundWordTokenFilter extends CompoundWordTokenFilterBase {
  @Deprecated
  public DictionaryCompoundWordTokenFilter(TokenStream input, String[] dictionary,
      int minWordSize, int minSubwordSize, int maxSubwordSize, boolean onlyLongestMatch) {
    super(Version.LUCENE_30, input, dictionary, minWordSize, minSubwordSize, maxSubwordSize, onlyLongestMatch);
  }
  @Deprecated
  public DictionaryCompoundWordTokenFilter(TokenStream input, String[] dictionary) {
    super(Version.LUCENE_30, input, dictionary);
  }
  @Deprecated
  public DictionaryCompoundWordTokenFilter(TokenStream input, Set dictionary) {
    super(Version.LUCENE_30, input, dictionary);
  }
  @Deprecated
  public DictionaryCompoundWordTokenFilter(TokenStream input, Set dictionary,
      int minWordSize, int minSubwordSize, int maxSubwordSize, boolean onlyLongestMatch) {
    super(Version.LUCENE_30, input, dictionary, minWordSize, minSubwordSize, maxSubwordSize, onlyLongestMatch);
  }
  public DictionaryCompoundWordTokenFilter(Version matchVersion, TokenStream input, String[] dictionary,
      int minWordSize, int minSubwordSize, int maxSubwordSize, boolean onlyLongestMatch) {
    super(matchVersion, input, dictionary, minWordSize, minSubwordSize, maxSubwordSize, onlyLongestMatch);
  }
  public DictionaryCompoundWordTokenFilter(Version matchVersion, TokenStream input, String[] dictionary) {
    super(matchVersion, input, dictionary);
  }
  public DictionaryCompoundWordTokenFilter(Version matchVersion, TokenStream input, Set dictionary) {
    super(matchVersion, input, dictionary);
  }
  public DictionaryCompoundWordTokenFilter(Version matchVersion, TokenStream input, Set dictionary,
      int minWordSize, int minSubwordSize, int maxSubwordSize, boolean onlyLongestMatch) {
    super(matchVersion, input, dictionary, minWordSize, minSubwordSize, maxSubwordSize, onlyLongestMatch);
  }
  @Override
  protected void decomposeInternal(final Token token) {
    if (token.termLength() < this.minWordSize) {
      return;
    }
    char[] lowerCaseTermBuffer=makeLowerCaseCopy(token.termBuffer());
    for (int i=0;i<token.termLength()-this.minSubwordSize;++i) {
        Token longestMatchToken=null;
        for (int j=this.minSubwordSize-1;j<this.maxSubwordSize;++j) {
            if(i+j>token.termLength()) {
                break;
            }
            if(dictionary.contains(lowerCaseTermBuffer, i, j)) {
                if (this.onlyLongestMatch) {
                   if (longestMatchToken!=null) {
                     if (longestMatchToken.termLength()<j) {
                       longestMatchToken=createToken(i,j,token);
                     }
                   } else {
                     longestMatchToken=createToken(i,j,token);
                   }
                } else {
                   tokens.add(createToken(i,j,token));
                }
            } 
        }
        if (this.onlyLongestMatch && longestMatchToken!=null) {
          tokens.add(longestMatchToken);
        }
    }
  }
}
