package org.apache.solr.analysis;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.language.Metaphone;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
public class TestPhoneticFilter extends BaseTokenTestCase {
  public void testFactory()
  {
    Map<String,String> args = new HashMap<String, String>();
    PhoneticFilterFactory ff = new PhoneticFilterFactory();
    try {
      ff.init( args );
      fail( "missing encoder parameter" );
    }
    catch( Exception ex ) {}
    args.put( PhoneticFilterFactory.ENCODER, "XXX" );
    try {
      ff.init( args );
      fail( "unknown encoder parameter" );
    }
    catch( Exception ex ) {}
    args.put( PhoneticFilterFactory.ENCODER, "Metaphone" );
    ff.init( args );
    assertTrue( ff.encoder instanceof Metaphone );
    assertTrue( ff.inject ); 
    args.put( PhoneticFilterFactory.INJECT, "false" );
    ff.init( args );
    assertFalse( ff.inject );
  }
  public void testAlgorithms() throws Exception {
    assertAlgorithm("Metaphone", "true", "aaa bbb ccc easgasg",
        new String[] { "A", "aaa", "B", "bbb", "KKK", "ccc", "ESKS", "easgasg" });
    assertAlgorithm("Metaphone", "false", "aaa bbb ccc easgasg",
        new String[] { "A", "B", "KKK", "ESKS" });
    assertAlgorithm("DoubleMetaphone", "true", "aaa bbb ccc easgasg",
        new String[] { "A", "aaa", "PP", "bbb", "KK", "ccc", "ASKS", "easgasg" });
    assertAlgorithm("DoubleMetaphone", "false", "aaa bbb ccc easgasg",
        new String[] { "A", "PP", "KK", "ASKS" });
    assertAlgorithm("Soundex", "true", "aaa bbb ccc easgasg",
        new String[] { "A000", "aaa", "B000", "bbb", "C000", "ccc", "E220", "easgasg" });
    assertAlgorithm("Soundex", "false", "aaa bbb ccc easgasg",
        new String[] { "A000", "B000", "C000", "E220" });
    assertAlgorithm("RefinedSoundex", "true", "aaa bbb ccc easgasg",
        new String[] { "A0", "aaa", "B1", "bbb", "C3", "ccc", "E034034", "easgasg" });
    assertAlgorithm("RefinedSoundex", "false", "aaa bbb ccc easgasg",
        new String[] { "A0", "B1", "C3", "E034034" });
  }
  static void assertAlgorithm(String algName, String inject, String input,
      String[] expected) throws Exception {
    Tokenizer tokenizer = new WhitespaceTokenizer(
        new StringReader(input));
    Map<String,String> args = new HashMap<String,String>();
    args.put("encoder", algName);
    args.put("inject", inject);
    PhoneticFilterFactory factory = new PhoneticFilterFactory();
    factory.init(args);
    TokenStream stream = factory.create(tokenizer);
    assertTokenStreamContents(stream, expected);
  }
}
