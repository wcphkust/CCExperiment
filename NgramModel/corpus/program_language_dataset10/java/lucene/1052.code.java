package org.apache.lucene.queryParser.analyzing;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
public class AnalyzingQueryParser extends org.apache.lucene.queryParser.QueryParser {
  public AnalyzingQueryParser(Version matchVersion, String field, Analyzer analyzer) {
    super(matchVersion, field, analyzer);
  }
  @Override
  protected Query getWildcardQuery(String field, String termStr) throws ParseException {
    List<String> tlist = new ArrayList<String>();
    List<String> wlist = new ArrayList<String>();
    boolean isWithinToken = (!termStr.startsWith("?") && !termStr.startsWith("*"));
    StringBuilder tmpBuffer = new StringBuilder();
    char[] chars = termStr.toCharArray();
    for (int i = 0; i < termStr.length(); i++) {
      if (chars[i] == '?' || chars[i] == '*') {
        if (isWithinToken) {
          tlist.add(tmpBuffer.toString());
          tmpBuffer.setLength(0);
        }
        isWithinToken = false;
      } else {
        if (!isWithinToken) {
          wlist.add(tmpBuffer.toString());
          tmpBuffer.setLength(0);
        }
        isWithinToken = true;
      }
      tmpBuffer.append(chars[i]);
    }
    if (isWithinToken) {
      tlist.add(tmpBuffer.toString());
    } else {
      wlist.add(tmpBuffer.toString());
    }
    TokenStream source = getAnalyzer().tokenStream(field, new StringReader(termStr));
    TermAttribute termAtt = source.addAttribute(TermAttribute.class);
    int countTokens = 0;
    while (true) {
      try {
        if (!source.incrementToken()) break;
      } catch (IOException e) {
        break;
      }
      String term = termAtt.term();
      if (!"".equals(term)) {
        try {
          tlist.set(countTokens++, term);
        } catch (IndexOutOfBoundsException ioobe) {
          countTokens = -1;
        }
      }
    }
    try {
      source.close();
    } catch (IOException e) {
    }
    if (countTokens != tlist.size()) {
      throw new ParseException("Cannot build WildcardQuery with analyzer "
          + getAnalyzer().getClass() + " - tokens added or lost");
    }
    if (tlist.size() == 0) {
      return null;
    } else if (tlist.size() == 1) {
      if (wlist != null && wlist.size() == 1) {
        return super.getWildcardQuery(field, tlist.get(0)
            + wlist.get(0).toString());
      } else {
        throw new IllegalArgumentException("getWildcardQuery called without wildcard");
      }
    } else {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < tlist.size(); i++) {
        sb.append( tlist.get(i));
        if (wlist != null && wlist.size() > i) {
          sb.append(wlist.get(i));
        }
      }
      return super.getWildcardQuery(field, sb.toString());
    }
  }
  @Override
  protected Query getPrefixQuery(String field, String termStr) throws ParseException {
    TokenStream source = getAnalyzer().tokenStream(field, new StringReader(termStr));
    List<String> tlist = new ArrayList<String>();
    TermAttribute termAtt = source.addAttribute(TermAttribute.class);
    while (true) {
      try {
        if (!source.incrementToken()) break;
      } catch (IOException e) {
        break;
      }
      tlist.add(termAtt.term());
    }
    try {
      source.close();
    } catch (IOException e) {
    }
    if (tlist.size() == 1) {
      return super.getPrefixQuery(field, tlist.get(0));
    } else {
      throw new ParseException("Cannot build PrefixQuery with analyzer "
          + getAnalyzer().getClass()
          + (tlist.size() > 1 ? " - token(s) added" : " - token consumed"));
    }
  }
  @Override
  protected Query getFuzzyQuery(String field, String termStr, float minSimilarity)
      throws ParseException {
    TokenStream source = getAnalyzer().tokenStream(field, new StringReader(termStr));
    TermAttribute termAtt = source.addAttribute(TermAttribute.class);
    String nextToken = null;
    boolean multipleTokens = false;
    try {
      if (source.incrementToken()) {
        nextToken = termAtt.term();
      }
      multipleTokens = source.incrementToken();
    } catch (IOException e) {
      nextToken = null;
    }
    try {
      source.close();
    } catch (IOException e) {
    }
    if (multipleTokens) {
      throw new ParseException("Cannot build FuzzyQuery with analyzer " + getAnalyzer().getClass()
          + " - tokens were added");
    }
    return (nextToken == null) ? null : super.getFuzzyQuery(field, nextToken, minSimilarity);
  }
  @Override
  protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive)
      throws ParseException {
    TokenStream source = getAnalyzer().tokenStream(field, new StringReader(part1));
    TermAttribute termAtt = source.addAttribute(TermAttribute.class);
    boolean multipleTokens = false;
    try {
      if (source.incrementToken()) {
        part1 = termAtt.term();
      }
      multipleTokens = source.incrementToken();
    } catch (IOException e) {
    }
    try {
      source.close();
    } catch (IOException e) {
    }
    if (multipleTokens) {
      throw new ParseException("Cannot build RangeQuery with analyzer " + getAnalyzer().getClass()
          + " - tokens were added to part1");
    }
    source = getAnalyzer().tokenStream(field, new StringReader(part2));
    termAtt = source.addAttribute(TermAttribute.class);
    try {
      if (source.incrementToken()) {
        part2 = termAtt.term();
      }
      multipleTokens = source.incrementToken();
    } catch (IOException e) {
    }
    try {
      source.close();
    } catch (IOException e) {
    }
    if (multipleTokens) {
      throw new ParseException("Cannot build RangeQuery with analyzer " + getAnalyzer().getClass()
          + " - tokens were added to part2");
    }
    return super.getRangeQuery(field, part1, part2, inclusive);
  }
}
