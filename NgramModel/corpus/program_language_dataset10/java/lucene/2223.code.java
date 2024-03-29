package org.apache.solr.analysis;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.compound.*;
import org.apache.solr.util.plugin.ResourceLoaderAware;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.io.IOException;
public class DictionaryCompoundWordTokenFilterFactory extends BaseTokenFilterFactory  implements ResourceLoaderAware {
  private Set dictionary;
  private String dictFile;
  private int minWordSize;
  private int minSubwordSize;
  private int maxSubwordSize;
  private boolean onlyLongestMatch;
  public void init(Map<String, String> args) {
    super.init(args);
    dictFile = args.get("dictionary");
    if (null == dictFile) {
      throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, 
                               "Missing required parameter: dictionary");
    }
    minWordSize= getInt("minWordSize",CompoundWordTokenFilterBase.DEFAULT_MIN_WORD_SIZE);
    minSubwordSize= getInt("minSubwordSize",CompoundWordTokenFilterBase.DEFAULT_MIN_SUBWORD_SIZE);
    maxSubwordSize= getInt("maxSubwordSize",CompoundWordTokenFilterBase.DEFAULT_MAX_SUBWORD_SIZE);
    onlyLongestMatch = getBoolean("onlyLongestMatch",true);
  }
  public void inform(ResourceLoader loader) {
    try {
      List<String> wlist = loader.getLines(dictFile);
      dictionary = StopFilter.makeStopSet((String[])wlist.toArray(new String[0]), false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  public DictionaryCompoundWordTokenFilter create(TokenStream input) {
    return new DictionaryCompoundWordTokenFilter(input,dictionary,minWordSize,minSubwordSize,maxSubwordSize,onlyLongestMatch);
  }
}
