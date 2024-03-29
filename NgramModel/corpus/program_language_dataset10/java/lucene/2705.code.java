package org.apache.solr.analysis;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.core.SolrResourceLoader;
public class TestElisionFilterFactory extends BaseTokenTestCase {
  public void testElision() throws Exception {
    Reader reader = new StringReader("l'avion");
    Tokenizer tokenizer = new WhitespaceTokenizer(reader);
    ElisionFilterFactory factory = new ElisionFilterFactory();
    factory.init(DEFAULT_VERSION_PARAM);
    ResourceLoader loader = new SolrResourceLoader(null, null);
    Map<String,String> args = new HashMap<String,String>();
    args.put("articles", "frenchArticles.txt");
    factory.init(args);
    factory.inform(loader);
    TokenStream stream = factory.create(tokenizer);
    assertTokenStreamContents(stream, new String[] { "avion" });
  }
}
