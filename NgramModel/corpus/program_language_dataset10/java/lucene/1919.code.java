package org.apache.lucene.queryParser;
import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.util.Version;
public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
  private static int multiToken = 0;
  public void testMultiAnalyzer() throws ParseException {
    QueryParser qp = new QueryParser(TEST_VERSION_CURRENT, "", new MultiAnalyzer());
    assertEquals("foo", qp.parse("foo").toString());
    assertEquals("foo", qp.parse("\"foo\"").toString());
    assertEquals("foo foobar", qp.parse("foo foobar").toString());
    assertEquals("\"foo foobar\"", qp.parse("\"foo foobar\"").toString());
    assertEquals("\"foo foobar blah\"", qp.parse("\"foo foobar blah\"").toString());
    assertEquals("(multi multi2) foo", qp.parse("multi foo").toString());
    assertEquals("foo (multi multi2)", qp.parse("foo multi").toString());
    assertEquals("(multi multi2) (multi multi2)", qp.parse("multi multi").toString());
    assertEquals("+(foo (multi multi2)) +(bar (multi multi2))",
        qp.parse("+(foo multi) +(bar multi)").toString());
    assertEquals("+(foo (multi multi2)) field:\"bar (multi multi2)\"",
        qp.parse("+(foo multi) field:\"bar multi\"").toString());
    assertEquals("\"(multi multi2) foo\"", qp.parse("\"multi foo\"").toString());
    assertEquals("\"foo (multi multi2)\"", qp.parse("\"foo multi\"").toString());
    assertEquals("\"foo (multi multi2) foobar (multi multi2)\"",
        qp.parse("\"foo multi foobar multi\"").toString());
    assertEquals("(field:multi field:multi2) field:foo", qp.parse("field:multi field:foo").toString());
    assertEquals("field:\"(multi multi2) foo\"", qp.parse("field:\"multi foo\"").toString());
    assertEquals("triplemulti multi3 multi2", qp.parse("triplemulti").toString());
    assertEquals("foo (triplemulti multi3 multi2) foobar",
        qp.parse("foo triplemulti foobar").toString());
    assertEquals("\"(multi multi2) foo\"~10", qp.parse("\"multi foo\"~10").toString());
    assertEquals("\"(multi multi2) foo\"^2.0", qp.parse("\"multi foo\"^2").toString());
    qp.setPhraseSlop(99);
    assertEquals("\"(multi multi2) foo\"~99 bar",
                 qp.parse("\"multi foo\" bar").toString());
    assertEquals("\"(multi multi2) foo\"~99 \"foo bar\"~2",
                 qp.parse("\"multi foo\" \"foo bar\"~2").toString());
    qp.setPhraseSlop(0);
    qp.setDefaultOperator(QueryParser.AND_OPERATOR);
    assertEquals("+(multi multi2) +foo", qp.parse("multi foo").toString());
  }
  public void testMultiAnalyzerWithSubclassOfQueryParser() throws ParseException {
    DumbQueryParser qp = new DumbQueryParser("", new MultiAnalyzer());
    qp.setPhraseSlop(99); 
    assertEquals("\"foo bar\"~99",
                 qp.getSuperFieldQuery("","foo bar").toString());
    assertEquals("\"(multi multi2) bar\"~99",
                 qp.getSuperFieldQuery("","multi bar").toString());
    assertEquals("\"(multi multi2) foo\"~99 bar",
                 qp.parse("\"multi foo\" bar").toString());
  }
  public void testPosIncrementAnalyzer() throws ParseException {
    QueryParser qp = new QueryParser(Version.LUCENE_24, "", new PosIncrementAnalyzer());
    assertEquals("quick brown", qp.parse("the quick brown").toString());
    assertEquals("\"quick brown\"", qp.parse("\"the quick brown\"").toString());
    assertEquals("quick brown fox", qp.parse("the quick brown fox").toString());
    assertEquals("\"quick brown fox\"", qp.parse("\"the quick brown fox\"").toString());
  }
  private class MultiAnalyzer extends Analyzer {
    public MultiAnalyzer() {
    }
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new StandardTokenizer(TEST_VERSION_CURRENT, reader);
      result = new TestFilter(result);
      result = new LowerCaseFilter(TEST_VERSION_CURRENT, result);
      return result;
    }
  }
  private final class TestFilter extends TokenFilter {
    private String prevType;
    private int prevStartOffset;
    private int prevEndOffset;
    TermAttribute termAtt;
    PositionIncrementAttribute posIncrAtt;
    OffsetAttribute offsetAtt;
    TypeAttribute typeAtt;
    public TestFilter(TokenStream in) {
      super(in);
      termAtt = addAttribute(TermAttribute.class);
      posIncrAtt = addAttribute(PositionIncrementAttribute.class);
      offsetAtt = addAttribute(OffsetAttribute.class);
      typeAtt = addAttribute(TypeAttribute.class);
    }
    @Override
    public final boolean incrementToken() throws java.io.IOException {
      if (multiToken > 0) {
        termAtt.setTermBuffer("multi"+(multiToken+1));
        offsetAtt.setOffset(prevStartOffset, prevEndOffset);
        typeAtt.setType(prevType);
        posIncrAtt.setPositionIncrement(0);
        multiToken--;
        return true;
      } else {
        boolean next = input.incrementToken();
        if (next == false) {
          return false;
        }
        prevType = typeAtt.type();
        prevStartOffset = offsetAtt.startOffset();
        prevEndOffset = offsetAtt.endOffset();
        String text = termAtt.term();
        if (text.equals("triplemulti")) {
          multiToken = 2;
          return true;
        } else if (text.equals("multi")) {
          multiToken = 1;
          return true;
        } else {
          return true;
        }
      }
    }
  }
  private class PosIncrementAnalyzer extends Analyzer {
    public PosIncrementAnalyzer() {
    }
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new StandardTokenizer(TEST_VERSION_CURRENT, reader);
      result = new TestPosIncrementFilter(result);
      result = new LowerCaseFilter(TEST_VERSION_CURRENT, result);
      return result;
    }
  }
  private final class TestPosIncrementFilter extends TokenFilter {
    TermAttribute termAtt;
    PositionIncrementAttribute posIncrAtt;
    public TestPosIncrementFilter(TokenStream in) {
      super(in);
      termAtt = addAttribute(TermAttribute.class);
      posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    }
    @Override
    public final boolean incrementToken () throws java.io.IOException {
      while(input.incrementToken()) {
        if (termAtt.term().equals("the")) {
        } else if (termAtt.term().equals("quick")) {
          posIncrAtt.setPositionIncrement(2);
          return true;
        } else {
          posIncrAtt.setPositionIncrement(1);
          return true;
        }
      }
      return false;
    }
  }
    private final static class DumbQueryParser extends QueryParser {
        public DumbQueryParser(String f, Analyzer a) {
            super(TEST_VERSION_CURRENT, f, a);
        }
        public Query getSuperFieldQuery(String f, String t) 
            throws ParseException {
            return super.getFieldQuery(f,t);
        }
        @Override
        protected Query getFieldQuery(String f, String t)
            throws ParseException {
            return new DumbQueryWrapper(getSuperFieldQuery(f,t));
        }
    }
    private final static class DumbQueryWrapper extends Query {
        private Query q;
        public DumbQueryWrapper(Query q) {
            super();
            this.q = q;
        }
        @Override
        public String toString(String f) {
            return q.toString(f);
        }
    }
}
