package org.apache.lucene.queryParser.standard;
import java.io.IOException;
import java.io.Reader;
import java.text.Collator;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.messages.MessageImpl;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.messages.QueryParserMessages;
import org.apache.lucene.queryParser.core.nodes.FuzzyQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorPipeline;
import org.apache.lucene.queryParser.standard.nodes.WildcardQueryNode;
import org.apache.lucene.queryParser.standard.processors.WildcardQueryNodeProcessor;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LocalizedTestCase;
public class TestQueryParserWrapper extends LocalizedTestCase {
  public TestQueryParserWrapper(String name) {
    super(name, new HashSet<String>(Arrays.asList(new String[]{
      "testLegacyDateRange", "testDateRange",
      "testCJK", "testNumber", "testFarsiRangeCollating",
      "testLocalDateFormat"
    })));
  }
  public static Analyzer qpAnalyzer = new QPTestAnalyzer();
  public static class QPTestFilter extends TokenFilter {
    TermAttribute termAtt;
    OffsetAttribute offsetAtt;
    public QPTestFilter(TokenStream in) {
      super(in);
      termAtt = addAttribute(TermAttribute.class);
      offsetAtt = addAttribute(OffsetAttribute.class);
    }
    boolean inPhrase = false;
    int savedStart = 0, savedEnd = 0;
    @Override
    public boolean incrementToken() throws IOException {
      if (inPhrase) {
        inPhrase = false;
        clearAttributes();
        termAtt.setTermBuffer("phrase2");
        offsetAtt.setOffset(savedStart, savedEnd);
        return true;
      } else
        while (input.incrementToken()) {
          if (termAtt.term().equals("phrase")) {
            inPhrase = true;
            savedStart = offsetAtt.startOffset();
            savedEnd = offsetAtt.endOffset();
            termAtt.setTermBuffer("phrase1");
            offsetAtt.setOffset(savedStart, savedEnd);
            return true;
          } else if (!termAtt.term().equals("stop"))
            return true;
        }
      return false;
    }
  }
  public static class QPTestAnalyzer extends Analyzer {
    @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
      return new QPTestFilter(new LowerCaseTokenizer(TEST_VERSION_CURRENT, reader));
    }
  }
  public static class QPTestParser extends QueryParserWrapper {
    public QPTestParser(String f, Analyzer a) {
      super(f, a);
      QueryNodeProcessorPipeline newProcessorPipeline = new QueryNodeProcessorPipeline(
          getQueryProcessor().getQueryConfigHandler());
      newProcessorPipeline.addProcessor(new WildcardQueryNodeProcessor());
      newProcessorPipeline.addProcessor(new QPTestParserQueryNodeProcessor());
      newProcessorPipeline.addProcessor(getQueryProcessor());
      setQueryProcessor(newProcessorPipeline);
    }
    @Override
    protected Query getFuzzyQuery(String field, String termStr,
        float minSimilarity) throws ParseException {
      throw new ParseException("Fuzzy queries not allowed");
    }
    @Override
    protected Query getWildcardQuery(String field, String termStr)
        throws ParseException {
      throw new ParseException("Wildcard queries not allowed");
    }
    private static class QPTestParserQueryNodeProcessor extends
        QueryNodeProcessorImpl {
      @Override
      protected QueryNode postProcessNode(QueryNode node)
          throws QueryNodeException {
        return node;
      }
      @Override
      protected QueryNode preProcessNode(QueryNode node)
          throws QueryNodeException {
        if (node instanceof WildcardQueryNode || node instanceof FuzzyQueryNode) {
          throw new QueryNodeException(new MessageImpl(
              QueryParserMessages.EMPTY_MESSAGE));
        }
        return node;
      }
      @Override
      protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
          throws QueryNodeException {
        return children;
      }
    }
  }
  private int originalMaxClauses;
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    originalMaxClauses = BooleanQuery.getMaxClauseCount();
  }
  public QueryParserWrapper getParser(Analyzer a) throws Exception {
    if (a == null)
      a = new SimpleAnalyzer(TEST_VERSION_CURRENT);
    QueryParserWrapper qp = new QueryParserWrapper("field", a);
    qp.setDefaultOperator(QueryParserWrapper.OR_OPERATOR);
    return qp;
  }
  public Query getQuery(String query, Analyzer a) throws Exception {
    return getParser(a).parse(query);
  }
  public Query getQueryAllowLeadingWildcard(String query, Analyzer a) throws Exception {
    QueryParserWrapper parser = getParser(a);
    parser.setAllowLeadingWildcard(true);
    return parser.parse(query);
  }
  public void assertQueryEquals(String query, Analyzer a, String result)
      throws Exception {
    Query q = getQuery(query, a);
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("Query /" + query + "/ yielded /" + s + "/, expecting /" + result
          + "/");
    }
  }
  public void assertQueryEqualsAllowLeadingWildcard(String query, Analyzer a, String result)
      throws Exception {
    Query q = getQueryAllowLeadingWildcard(query, a);
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("Query /" + query + "/ yielded /" + s + "/, expecting /" + result
          + "/");
    }
  }
  public void assertQueryEquals(QueryParserWrapper qp, String field,
      String query, String result) throws Exception {
    Query q = qp.parse(query);
    String s = q.toString(field);
    if (!s.equals(result)) {
      fail("Query /" + query + "/ yielded /" + s + "/, expecting /" + result
          + "/");
    }
  }
  public void assertEscapedQueryEquals(String query, Analyzer a, String result)
      throws Exception {
    String escapedQuery = QueryParserWrapper.escape(query);
    if (!escapedQuery.equals(result)) {
      fail("Query /" + query + "/ yielded /" + escapedQuery + "/, expecting /"
          + result + "/");
    }
  }
  public void assertWildcardQueryEquals(String query, boolean lowercase,
      String result, boolean allowLeadingWildcard) throws Exception {
    QueryParserWrapper qp = getParser(null);
    qp.setLowercaseExpandedTerms(lowercase);
    qp.setAllowLeadingWildcard(allowLeadingWildcard);
    Query q = qp.parse(query);
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("WildcardQuery /" + query + "/ yielded /" + s + "/, expecting /"
          + result + "/");
    }
  }
  public void assertWildcardQueryEquals(String query, boolean lowercase,
      String result) throws Exception {
    assertWildcardQueryEquals(query, lowercase, result, false);
  }
  public void assertWildcardQueryEquals(String query, String result)
      throws Exception {
    QueryParserWrapper qp = getParser(null);
    Query q = qp.parse(query);
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("WildcardQuery /" + query + "/ yielded /" + s + "/, expecting /"
          + result + "/");
    }
  }
  public Query getQueryDOA(String query, Analyzer a) throws Exception {
    if (a == null)
      a = new SimpleAnalyzer(TEST_VERSION_CURRENT);
    QueryParserWrapper qp = new QueryParserWrapper("field", a);
    qp.setDefaultOperator(QueryParserWrapper.AND_OPERATOR);
    return qp.parse(query);
  }
  public void assertQueryEqualsDOA(String query, Analyzer a, String result)
      throws Exception {
    Query q = getQueryDOA(query, a);
    String s = q.toString("field");
    if (!s.equals(result)) {
      fail("Query /" + query + "/ yielded /" + s + "/, expecting /" + result
          + "/");
    }
  }
  public void testCJK() throws Exception {
    assertQueryEquals("term\u3000term\u3000term", null,
        "term\u0020term\u0020term");
    assertQueryEqualsAllowLeadingWildcard("??\u3000??\u3000??", null, "??\u0020??\u0020??");
  }
  public void testSimple() throws Exception {
    assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
    assertQueryEquals("term term term", null, "term term term");
    assertQueryEquals("t�rm term term", new WhitespaceAnalyzer(TEST_VERSION_CURRENT),
        "t�rm term term");
    assertQueryEquals("�mlaut", new WhitespaceAnalyzer(TEST_VERSION_CURRENT), "�mlaut");
    assertQueryEquals("\"\"", new KeywordAnalyzer(), "");
    assertQueryEquals("foo:\"\"", new KeywordAnalyzer(), "foo:");
    assertQueryEquals("a AND b", null, "+a +b");
    assertQueryEquals("(a AND b)", null, "+a +b");
    assertQueryEquals("c OR (a AND b)", null, "c (+a +b)");
    assertQueryEquals("a AND NOT b", null, "+a -b");
    assertQueryEquals("a AND -b", null, "+a -b");
    assertQueryEquals("a AND !b", null, "+a -b");
    assertQueryEquals("a && b", null, "+a +b");
    assertQueryEquals("a && ! b", null, "+a -b");
    assertQueryEquals("a OR b", null, "a b");
    assertQueryEquals("a || b", null, "a b");
    assertQueryEquals("a OR !b", null, "a -b");
    assertQueryEquals("a OR ! b", null, "a -b");
    assertQueryEquals("a OR -b", null, "a -b");
    assertQueryEquals("+term -term term", null, "+term -term term");
    assertQueryEquals("foo:term AND field:anotherTerm", null,
        "+foo:term +anotherterm");
    assertQueryEquals("term AND \"phrase phrase\"", null,
        "+term +\"phrase phrase\"");
    assertQueryEquals("\"hello there\"", null, "\"hello there\"");
    assertTrue(getQuery("a AND b", null) instanceof BooleanQuery);
    assertTrue(getQuery("hello", null) instanceof TermQuery);
    assertTrue(getQuery("\"hello there\"", null) instanceof PhraseQuery);
    assertQueryEquals("germ term^2.0", null, "germ term^2.0");
    assertQueryEquals("(term)^2.0", null, "term^2.0");
    assertQueryEquals("(germ term)^2.0", null, "(germ term)^2.0");
    assertQueryEquals("term^2.0", null, "term^2.0");
    assertQueryEquals("term^2", null, "term^2.0");
    assertQueryEquals("\"germ term\"^2.0", null, "\"germ term\"^2.0");
    assertQueryEquals("\"term germ\"^2", null, "\"term germ\"^2.0");
    assertQueryEquals("(foo OR bar) AND (baz OR boo)", null,
        "+(foo bar) +(baz boo)");
    assertQueryEquals("((a OR b) AND NOT c) OR d", null, "(+(a b) -c) d");
    assertQueryEquals("+(apple \"steve jobs\") -(foo bar baz)", null,
        "+(apple \"steve jobs\") -(foo bar baz)");
    assertQueryEquals("+title:(dog OR cat) -author:\"bob dole\"", null,
        "+(title:dog title:cat) -author:\"bob dole\"");
    QueryParserWrapper qp = new QueryParserWrapper("field",
        new StandardAnalyzer(TEST_VERSION_CURRENT));
    assertEquals(QueryParserWrapper.OR_OPERATOR, qp.getDefaultOperator());
    qp.setDefaultOperator(QueryParserWrapper.AND_OPERATOR);
    assertEquals(QueryParserWrapper.AND_OPERATOR, qp.getDefaultOperator());
    qp.setDefaultOperator(QueryParserWrapper.OR_OPERATOR);
    assertEquals(QueryParserWrapper.OR_OPERATOR, qp.getDefaultOperator());
  }
  public void testPunct() throws Exception {
    Analyzer a = new WhitespaceAnalyzer(TEST_VERSION_CURRENT);
    assertQueryEquals("a&b", a, "a&b");
    assertQueryEquals("a&&b", a, "a&&b");
    assertQueryEquals(".NET", a, ".NET");
  }
  public void testSlop() throws Exception {
    assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
    assertQueryEquals("\"term germ\"~2 flork", null, "\"term germ\"~2 flork");
    assertQueryEquals("\"term\"~2", null, "term");
    assertQueryEquals("\" \"~2 germ", null, "germ");
    assertQueryEquals("\"term germ\"~2^2", null, "\"term germ\"~2^2.0");
  }
  public void testNumber() throws Exception {
    assertQueryEquals("3", null, "");
    assertQueryEquals("term 1.0 1 2", null, "term");
    assertQueryEquals("term term1 term2", null, "term term term");
    Analyzer a = new StandardAnalyzer(TEST_VERSION_CURRENT);
    assertQueryEquals("3", a, "3");
    assertQueryEquals("term 1.0 1 2", a, "term 1.0 1 2");
    assertQueryEquals("term term1 term2", a, "term term1 term2");
  }
  public void testWildcard() throws Exception {
    assertQueryEquals("term*", null, "term*");
    assertQueryEquals("term*^2", null, "term*^2.0");
    assertQueryEquals("term~", null, "term~0.5");
    assertQueryEquals("term~0.7", null, "term~0.7");
    assertQueryEquals("term~^2", null, "term~0.5^2.0");
    assertQueryEquals("term^2~", null, "term~0.5^2.0");
    assertQueryEquals("term*germ", null, "term*germ");
    assertQueryEquals("term*germ^3", null, "term*germ^3.0");
    assertTrue(getQuery("term*", null) instanceof PrefixQuery);
    assertTrue(getQuery("term*^2", null) instanceof PrefixQuery);
    assertTrue(getQuery("term~", null) instanceof FuzzyQuery);
    assertTrue(getQuery("term~0.7", null) instanceof FuzzyQuery);
    FuzzyQuery fq = (FuzzyQuery) getQuery("term~0.7", null);
    assertEquals(0.7f, fq.getMinSimilarity(), 0.1f);
    assertEquals(FuzzyQuery.defaultPrefixLength, fq.getPrefixLength());
    fq = (FuzzyQuery) getQuery("term~", null);
    assertEquals(0.5f, fq.getMinSimilarity(), 0.1f);
    assertEquals(FuzzyQuery.defaultPrefixLength, fq.getPrefixLength());
    assertParseException("term~1.1"); 
    assertTrue(getQuery("term*germ", null) instanceof WildcardQuery);
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("term*", true, "term*");
    assertWildcardQueryEquals("Term*", true, "term*");
    assertWildcardQueryEquals("TERM*", true, "term*");
    assertWildcardQueryEquals("term*", false, "term*");
    assertWildcardQueryEquals("Term*", false, "Term*");
    assertWildcardQueryEquals("TERM*", false, "TERM*");
    assertWildcardQueryEquals("Te?m", "te?m");
    assertWildcardQueryEquals("te?m", true, "te?m");
    assertWildcardQueryEquals("Te?m", true, "te?m");
    assertWildcardQueryEquals("TE?M", true, "te?m");
    assertWildcardQueryEquals("Te?m*gerM", true, "te?m*germ");
    assertWildcardQueryEquals("te?m", false, "te?m");
    assertWildcardQueryEquals("Te?m", false, "Te?m");
    assertWildcardQueryEquals("TE?M", false, "TE?M");
    assertWildcardQueryEquals("Te?m*gerM", false, "Te?m*gerM");
    assertWildcardQueryEquals("Term~", "term~0.5");
    assertWildcardQueryEquals("Term~", true, "term~0.5");
    assertWildcardQueryEquals("Term~", false, "Term~0.5");
    assertWildcardQueryEquals("[A TO C]", "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", true, "[a TO c]");
    assertWildcardQueryEquals("[A TO C]", false, "[A TO C]");
    try {
      assertWildcardQueryEquals("*Term", true, "*term");
      fail();
    } catch (ParseException pe) {
    }
    try {
      assertWildcardQueryEquals("?Term", true, "?term");
      fail();
    } catch (ParseException pe) {
    }
    assertWildcardQueryEquals("*Term", true, "*term", true);
    assertWildcardQueryEquals("?Term", true, "?term", true);
  }
  public void testLeadingWildcardType() throws Exception {
    QueryParserWrapper qp = getParser(null);
    qp.setAllowLeadingWildcard(true);
    assertEquals(WildcardQuery.class, qp.parse("t*erm*").getClass());
    assertEquals(WildcardQuery.class, qp.parse("?term*").getClass());
    assertEquals(WildcardQuery.class, qp.parse("*term*").getClass());
  }
  public void testQPA() throws Exception {
    assertQueryEquals("term term^3.0 term", qpAnalyzer, "term term^3.0 term");
    assertQueryEquals("term stop^3.0 term", qpAnalyzer, "term term");
    assertQueryEquals("term term term", qpAnalyzer, "term term term");
    assertQueryEquals("term +stop term", qpAnalyzer, "term term");
    assertQueryEquals("term -stop term", qpAnalyzer, "term term");
    assertQueryEquals("drop AND (stop) AND roll", qpAnalyzer, "+drop +roll");
    assertQueryEquals("term +(stop) term", qpAnalyzer, "term term");
    assertQueryEquals("term -(stop) term", qpAnalyzer, "term term");
    assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
    assertQueryEquals("term phrase term", qpAnalyzer,
        "term \"phrase1 phrase2\" term");
    assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
        "+term -\"phrase1 phrase2\" term");
    assertQueryEquals("stop^3", qpAnalyzer, "");
    assertQueryEquals("stop", qpAnalyzer, "");
    assertQueryEquals("(stop)^3", qpAnalyzer, "");
    assertQueryEquals("((stop))^3", qpAnalyzer, "");
    assertQueryEquals("(stop^3)", qpAnalyzer, "");
    assertQueryEquals("((stop)^3)", qpAnalyzer, "");
    assertQueryEquals("(stop)", qpAnalyzer, "");
    assertQueryEquals("((stop))", qpAnalyzer, "");
    assertTrue(getQuery("term term term", qpAnalyzer) instanceof BooleanQuery);
    assertTrue(getQuery("term +stop", qpAnalyzer) instanceof TermQuery);
  }
  public void testRange() throws Exception {
    assertQueryEquals("[ a TO z]", null, "[a TO z]");
    assertEquals(MultiTermQuery.CONSTANT_SCORE_AUTO_REWRITE_DEFAULT, ((TermRangeQuery)getQuery("[ a TO z]", null)).getRewriteMethod());
    QueryParserWrapper qp = new QueryParserWrapper("field",
        new SimpleAnalyzer(TEST_VERSION_CURRENT));
    qp.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
    assertEquals(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE,((TermRangeQuery)qp.parse("[ a TO z]")).getRewriteMethod());
    assertQueryEquals("[ a TO z ]", null, "[a TO z]");
    assertQueryEquals("{ a TO z}", null, "{a TO z}");
    assertQueryEquals("{ a TO z }", null, "{a TO z}");
    assertQueryEquals("{ a TO z }^2.0", null, "{a TO z}^2.0");
    assertQueryEquals("[ a TO z] OR bar", null, "[a TO z] bar");
    assertQueryEquals("[ a TO z] AND bar", null, "+[a TO z] +bar");
    assertQueryEquals("( bar blar { a TO z}) ", null, "bar blar {a TO z}");
    assertQueryEquals("gack ( bar blar { a TO z}) ", null,
        "gack (bar blar {a TO z})");
  }
  public void testFarsiRangeCollating() throws Exception {
    RAMDirectory ramDir = new RAMDirectory();
    IndexWriter iw = new IndexWriter(ramDir, new WhitespaceAnalyzer(TEST_VERSION_CURRENT), true,
        IndexWriter.MaxFieldLength.LIMITED);
    Document doc = new Document();
    doc.add(new Field("content", "\u0633\u0627\u0628", Field.Store.YES,
        Field.Index.NOT_ANALYZED));
    iw.addDocument(doc);
    iw.close();
    IndexSearcher is = new IndexSearcher(ramDir, true);
    QueryParserWrapper qp = new QueryParserWrapper("content",
        new WhitespaceAnalyzer(TEST_VERSION_CURRENT));
    Collator c = Collator.getInstance(new Locale("ar"));
    qp.setRangeCollator(c);
    qp.setMultiTermRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
    ScoreDoc[] result = is.search(qp.parse("[ \u062F TO \u0698 ]"), null, 1000).scoreDocs;
    assertEquals("The index Term should not be included.", 0, result.length);
    result = is.search(qp.parse("[ \u0633 TO \u0638 ]"), null, 1000).scoreDocs;
    assertEquals("The index Term should be included.", 1, result.length);
    qp.setMultiTermRewriteMethod(MultiTermQuery.SCORING_BOOLEAN_QUERY_REWRITE);
    result = is.search(qp.parse("[ \u062F TO \u0698 ]"), null, 1000).scoreDocs;
    assertEquals("The index Term should not be included.", 0, result.length);
    result = is.search(qp.parse("[ \u0633 TO \u0638 ]"), null, 1000).scoreDocs;
    assertEquals("The index Term should be included.", 1, result.length);
    is.close();
  }
  private String escapeDateString(String s) {
    if (s.contains(" ")) {
      return "\"" + s + "\"";
    } else {
      return s;
    }
  }
  private String getLegacyDate(String s) throws Exception {
    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
    return DateField.dateToString(df.parse(s));
  }
  private String getDate(String s, DateTools.Resolution resolution)
      throws Exception {
    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
    return getDate(df.parse(s), resolution);
  }
  private String getDate(Date d, DateTools.Resolution resolution)
      throws Exception {
    if (resolution == null) {
      return DateField.dateToString(d);
    } else {
      return DateTools.dateToString(d, resolution);
    }
  }
  private String getLocalizedDate(int year, int month, int day) {
    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
    Calendar calendar = new GregorianCalendar();
    calendar.clear();
    calendar.set(year, month, day);
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return df.format(calendar.getTime());
  }
  public void testLegacyDateRange() throws Exception {
    String startDate = getLocalizedDate(2002, 1, 1);
    String endDate = getLocalizedDate(2002, 1, 4);
    Calendar endDateExpected = new GregorianCalendar();
    endDateExpected.clear();
    endDateExpected.set(2002, 1, 4, 23, 59, 59);
    endDateExpected.set(Calendar.MILLISECOND, 999);
    assertQueryEquals("[ " + escapeDateString(startDate) + " TO " + escapeDateString(endDate) + "]", null, "["
        + getLegacyDate(startDate) + " TO "
        + DateField.dateToString(endDateExpected.getTime()) + "]");
    assertQueryEquals("{  " + escapeDateString(startDate) + "    " + escapeDateString(endDate) + "   }", null, "{"
        + getLegacyDate(startDate) + " TO " + getLegacyDate(endDate) + "}");
  }
  public void testDateRange() throws Exception {
    String startDate = getLocalizedDate(2002, 1, 1);
    String endDate = getLocalizedDate(2002, 1, 4);
    Calendar endDateExpected = new GregorianCalendar();
    endDateExpected.clear();
    endDateExpected.set(2002, 1, 4, 23, 59, 59);
    endDateExpected.set(Calendar.MILLISECOND, 999);
    final String defaultField = "default";
    final String monthField = "month";
    final String hourField = "hour";
    QueryParserWrapper qp = new QueryParserWrapper("field",
        new SimpleAnalyzer(TEST_VERSION_CURRENT));
    assertDateRangeQueryEquals(qp, defaultField, startDate, endDate,
        endDateExpected.getTime(), null);
    qp.setDateResolution(monthField, DateTools.Resolution.MONTH);
    assertDateRangeQueryEquals(qp, defaultField, startDate, endDate,
        endDateExpected.getTime(), null);
    qp.setDateResolution(DateTools.Resolution.MILLISECOND);
    qp.setDateResolution(hourField, DateTools.Resolution.HOUR);
    assertDateRangeQueryEquals(qp, defaultField, startDate, endDate,
        endDateExpected.getTime(), DateTools.Resolution.MILLISECOND);
    assertDateRangeQueryEquals(qp, monthField, startDate, endDate,
        endDateExpected.getTime(), DateTools.Resolution.MONTH);
    assertDateRangeQueryEquals(qp, hourField, startDate, endDate,
        endDateExpected.getTime(), DateTools.Resolution.HOUR);
  }
  public void assertDateRangeQueryEquals(QueryParserWrapper qp, String field,
      String startDate, String endDate, Date endDateInclusive,
      DateTools.Resolution resolution) throws Exception {
    assertQueryEquals(qp, field, field + ":[" + escapeDateString(startDate) + " TO " + escapeDateString(endDate)
        + "]", "[" + getDate(startDate, resolution) + " TO "
        + getDate(endDateInclusive, resolution) + "]");
    assertQueryEquals(qp, field, field + ":{" + escapeDateString(startDate) + " TO " + escapeDateString(endDate)
        + "}", "{" + getDate(startDate, resolution) + " TO "
        + getDate(endDate, resolution) + "}");
  }
  public void testEscaped() throws Exception {
    Analyzer a = new WhitespaceAnalyzer(TEST_VERSION_CURRENT);
    assertQueryEquals("\\a", a, "a");
    assertQueryEquals("a\\-b:c", a, "a-b:c");
    assertQueryEquals("a\\+b:c", a, "a+b:c");
    assertQueryEquals("a\\:b:c", a, "a:b:c");
    assertQueryEquals("a\\\\b:c", a, "a\\b:c");
    assertQueryEquals("a:b\\-c", a, "a:b-c");
    assertQueryEquals("a:b\\+c", a, "a:b+c");
    assertQueryEquals("a:b\\:c", a, "a:b:c");
    assertQueryEquals("a:b\\\\c", a, "a:b\\c");
    assertQueryEquals("a:b\\-c*", a, "a:b-c*");
    assertQueryEquals("a:b\\+c*", a, "a:b+c*");
    assertQueryEquals("a:b\\:c*", a, "a:b:c*");
    assertQueryEquals("a:b\\\\c*", a, "a:b\\c*");
    assertQueryEquals("a:b\\-?c", a, "a:b-?c");
    assertQueryEquals("a:b\\+?c", a, "a:b+?c");
    assertQueryEquals("a:b\\:?c", a, "a:b:?c");
    assertQueryEquals("a:b\\\\?c", a, "a:b\\?c");
    assertQueryEquals("a:b\\-c~", a, "a:b-c~0.5");
    assertQueryEquals("a:b\\+c~", a, "a:b+c~0.5");
    assertQueryEquals("a:b\\:c~", a, "a:b:c~0.5");
    assertQueryEquals("a:b\\\\c~", a, "a:b\\c~0.5");
    assertQueryEquals("[ a\\- TO a\\+ ]", null, "[a- TO a+]");
    assertQueryEquals("[ a\\: TO a\\~ ]", null, "[a: TO a~]");
    assertQueryEquals("[ a\\\\ TO a\\* ]", null, "[a\\ TO a*]");
    assertQueryEquals(
        "[\"c\\:\\\\temp\\\\\\~foo0.txt\" TO \"c\\:\\\\temp\\\\\\~foo9.txt\"]",
        a, "[c:\\temp\\~foo0.txt TO c:\\temp\\~foo9.txt]");
    assertQueryEquals("a\\\\\\+b", a, "a\\+b");
    assertQueryEquals("a \\\"b c\\\" d", a, "a \"b c\" d");
    assertQueryEquals("\"a \\\"b c\\\" d\"", a, "\"a \"b c\" d\"");
    assertQueryEquals("\"a \\+b c d\"", a, "\"a +b c d\"");
    assertQueryEquals("c\\:\\\\temp\\\\\\~foo.txt", a, "c:\\temp\\~foo.txt");
    assertParseException("XY\\"); 
    assertQueryEquals("a\\u0062c", a, "abc");
    assertQueryEquals("XY\\u005a", a, "XYZ");
    assertQueryEquals("XY\\u005A", a, "XYZ");
    assertQueryEquals("\"a \\\\\\u0028\\u0062\\\" c\"", a, "\"a \\(b\" c\"");
    assertParseException("XY\\u005G"); 
    assertParseException("XY\\u005"); 
    assertQueryEquals("(item:\\\\ item:ABCD\\\\)", a, "item:\\ item:ABCD\\");
    assertParseException("(item:\\\\ item:ABCD\\\\))"); 
    assertQueryEquals("\\*", a, "*");
    assertQueryEquals("\\\\", a, "\\"); 
    assertParseException("\\"); 
    assertQueryEquals("(\"a\\\\\") or (\"b\")", a, "a\\ or b");
  }
  public void testQueryStringEscaping() throws Exception {
    Analyzer a = new WhitespaceAnalyzer(TEST_VERSION_CURRENT);
    assertEscapedQueryEquals("a-b:c", a, "a\\-b\\:c");
    assertEscapedQueryEquals("a+b:c", a, "a\\+b\\:c");
    assertEscapedQueryEquals("a:b:c", a, "a\\:b\\:c");
    assertEscapedQueryEquals("a\\b:c", a, "a\\\\b\\:c");
    assertEscapedQueryEquals("a:b-c", a, "a\\:b\\-c");
    assertEscapedQueryEquals("a:b+c", a, "a\\:b\\+c");
    assertEscapedQueryEquals("a:b:c", a, "a\\:b\\:c");
    assertEscapedQueryEquals("a:b\\c", a, "a\\:b\\\\c");
    assertEscapedQueryEquals("a:b-c*", a, "a\\:b\\-c\\*");
    assertEscapedQueryEquals("a:b+c*", a, "a\\:b\\+c\\*");
    assertEscapedQueryEquals("a:b:c*", a, "a\\:b\\:c\\*");
    assertEscapedQueryEquals("a:b\\\\c*", a, "a\\:b\\\\\\\\c\\*");
    assertEscapedQueryEquals("a:b-?c", a, "a\\:b\\-\\?c");
    assertEscapedQueryEquals("a:b+?c", a, "a\\:b\\+\\?c");
    assertEscapedQueryEquals("a:b:?c", a, "a\\:b\\:\\?c");
    assertEscapedQueryEquals("a:b?c", a, "a\\:b\\?c");
    assertEscapedQueryEquals("a:b-c~", a, "a\\:b\\-c\\~");
    assertEscapedQueryEquals("a:b+c~", a, "a\\:b\\+c\\~");
    assertEscapedQueryEquals("a:b:c~", a, "a\\:b\\:c\\~");
    assertEscapedQueryEquals("a:b\\c~", a, "a\\:b\\\\c\\~");
    assertEscapedQueryEquals("[ a - TO a+ ]", null, "\\[ a \\- TO a\\+ \\]");
    assertEscapedQueryEquals("[ a : TO a~ ]", null, "\\[ a \\: TO a\\~ \\]");
    assertEscapedQueryEquals("[ a\\ TO a* ]", null, "\\[ a\\\\ TO a\\* \\]");
    assertEscapedQueryEquals("|| abc ||", a, "\\|\\| abc \\|\\|");
    assertEscapedQueryEquals("&& abc &&", a, "\\&\\& abc \\&\\&");
  }
  public void testTabNewlineCarriageReturn() throws Exception {
    assertQueryEqualsDOA("+weltbank +worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("+weltbank\n+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \n+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \n +worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("+weltbank\r+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \r+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \r +worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("+weltbank\r\n+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \r\n+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \r\n +worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \r \n +worlbank", null,
        "+weltbank +worlbank");
    assertQueryEqualsDOA("+weltbank\t+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \t+worlbank", null, "+weltbank +worlbank");
    assertQueryEqualsDOA("weltbank \t +worlbank", null, "+weltbank +worlbank");
  }
  public void testSimpleDAO() throws Exception {
    assertQueryEqualsDOA("term term term", null, "+term +term +term");
    assertQueryEqualsDOA("term +term term", null, "+term +term +term");
    assertQueryEqualsDOA("term term +term", null, "+term +term +term");
    assertQueryEqualsDOA("term +term +term", null, "+term +term +term");
    assertQueryEqualsDOA("-term term term", null, "-term +term +term");
  }
  public void testBoost() throws Exception {
    StandardAnalyzer oneStopAnalyzer = new StandardAnalyzer(TEST_VERSION_CURRENT, Collections.singleton("on"));
    QueryParserWrapper qp = new QueryParserWrapper("field", oneStopAnalyzer);
    Query q = qp.parse("on^1.0");
    assertNotNull(q);
    q = qp.parse("\"hello\"^2.0");
    assertNotNull(q);
    assertEquals(q.getBoost(), (float) 2.0, (float) 0.5);
    q = qp.parse("hello^2.0");
    assertNotNull(q);
    assertEquals(q.getBoost(), (float) 2.0, (float) 0.5);
    q = qp.parse("\"on\"^1.0");
    assertNotNull(q);
    QueryParserWrapper qp2 = new QueryParserWrapper("field",
        new StandardAnalyzer(TEST_VERSION_CURRENT));
    q = qp2.parse("the^3");
    assertNotNull(q);
    assertEquals("", q.toString());
    assertEquals(1.0f, q.getBoost(), 0.01f);
  }
  public void assertParseException(String queryString) throws Exception {
    try {
      getQuery(queryString, null);
    } catch (ParseException expected) {
      return;
    }
    fail("ParseException expected, not thrown");
  }
  public void testException() throws Exception {
    assertParseException("\"some phrase");
    assertParseException("(foo bar");
    assertParseException("foo bar))");
    assertParseException("field:term:with:colon some more terms");
    assertParseException("(sub query)^5.0^2.0 plus more");
    assertParseException("secret AND illegal) AND access:confidential");
  }
  public void testCustomQueryParserWildcard() {
    try {
      new QPTestParser("contents", new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).parse("a?t");
      fail("Wildcard queries should not be allowed");
    } catch (ParseException expected) {
    }
  }
  public void testCustomQueryParserFuzzy() throws Exception {
    try {
      new QPTestParser("contents", new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).parse("xunit~");
      fail("Fuzzy queries should not be allowed");
    } catch (ParseException expected) {
    }
  }
  public void testBooleanQuery() throws Exception {
    BooleanQuery.setMaxClauseCount(2);
    try {
      QueryParserWrapper qp = new QueryParserWrapper("field",
          new WhitespaceAnalyzer(TEST_VERSION_CURRENT));
      qp.parse("one two three");
      fail("ParseException expected due to too many boolean clauses");
    } catch (ParseException expected) {
    }
  }
  public void testPrecedence() throws Exception {
    QueryParserWrapper qp = new QueryParserWrapper("field",
        new WhitespaceAnalyzer(TEST_VERSION_CURRENT));
    Query query1 = qp.parse("A AND B OR C AND D");
    Query query2 = qp.parse("+A +B +C +D");
    assertEquals(query1, query2);
  }
  public void testLocalDateFormat() throws IOException, ParseException {
    RAMDirectory ramDir = new RAMDirectory();
    IndexWriter iw = new IndexWriter(ramDir, new WhitespaceAnalyzer(TEST_VERSION_CURRENT), true,
        IndexWriter.MaxFieldLength.LIMITED);
    addDateDoc("a", 2005, 12, 2, 10, 15, 33, iw);
    addDateDoc("b", 2005, 12, 4, 22, 15, 00, iw);
    iw.close();
    IndexSearcher is = new IndexSearcher(ramDir, true);
    assertHits(1, "[12/1/2005 TO 12/3/2005]", is);
    assertHits(2, "[12/1/2005 TO 12/4/2005]", is);
    assertHits(1, "[12/3/2005 TO 12/4/2005]", is);
    assertHits(1, "{12/1/2005 TO 12/3/2005}", is);
    assertHits(1, "{12/1/2005 TO 12/4/2005}", is);
    assertHits(0, "{12/3/2005 TO 12/4/2005}", is);
    is.close();
  }
  public void testStarParsing() throws Exception {
  }
  public void testStopwords() throws Exception {
    QueryParserWrapper qp = new QueryParserWrapper("a", new StopAnalyzer(TEST_VERSION_CURRENT, StopFilter.makeStopSet(TEST_VERSION_CURRENT, "the", "foo")));
    Query result = qp.parse("a:the OR a:foo");
    assertNotNull("result is null and it shouldn't be", result);
    assertTrue("result is not a BooleanQuery", result instanceof BooleanQuery);
    assertTrue(((BooleanQuery) result).clauses().size() + " does not equal: "
        + 0, ((BooleanQuery) result).clauses().size() == 0);
    result = qp.parse("a:woo OR a:the");
    assertNotNull("result is null and it shouldn't be", result);
    assertTrue("result is not a TermQuery", result instanceof TermQuery);
    result = qp
        .parse("(fieldX:xxxxx OR fieldy:xxxxxxxx)^2 AND (fieldx:the OR fieldy:foo)");
    assertNotNull("result is null and it shouldn't be", result);
    assertTrue("result is not a BooleanQuery", result instanceof BooleanQuery);
    if (VERBOSE) System.out.println("Result: " + result);
    assertTrue(((BooleanQuery) result).clauses().size() + " does not equal: "
        + 2, ((BooleanQuery) result).clauses().size() == 2);
  }
  public void testPositionIncrement() throws Exception {
    QueryParserWrapper qp = new QueryParserWrapper("a", new StopAnalyzer(TEST_VERSION_CURRENT, StopFilter.makeStopSet(TEST_VERSION_CURRENT, "the", "in", "are", "this")));
    qp.setEnablePositionIncrements(true);
    String qtxt = "\"the words in poisitions pos02578 are stopped in this phrasequery\"";
    int expectedPositions[] = { 1, 3, 4, 6, 9 };
    PhraseQuery pq = (PhraseQuery) qp.parse(qtxt);
    Term t[] = pq.getTerms();
    int pos[] = pq.getPositions();
    for (int i = 0; i < t.length; i++) {
      assertEquals("term " + i + " = " + t[i] + " has wrong term-position!",
          expectedPositions[i], pos[i]);
    }
  }
  public void testMatchAllDocs() throws Exception {
    QueryParserWrapper qp = new QueryParserWrapper("field",
        new WhitespaceAnalyzer(TEST_VERSION_CURRENT));
    assertEquals(new MatchAllDocsQuery(), qp.parse("*:*"));
    assertEquals(new MatchAllDocsQuery(), qp.parse("(*:*)"));
    BooleanQuery bq = (BooleanQuery) qp.parse("+*:* -*:*");
    assertTrue(bq.getClauses()[0].getQuery() instanceof MatchAllDocsQuery);
    assertTrue(bq.getClauses()[1].getQuery() instanceof MatchAllDocsQuery);
  }
  private void assertHits(int expected, String query, IndexSearcher is)
      throws ParseException, IOException {
    QueryParserWrapper qp = new QueryParserWrapper("date",
        new WhitespaceAnalyzer(TEST_VERSION_CURRENT));
    qp.setLocale(Locale.ENGLISH);
    Query q = qp.parse(query);
    ScoreDoc[] hits = is.search(q, null, 1000).scoreDocs;
    assertEquals(expected, hits.length);
  }
  private static void addDateDoc(String content, int year, int month, int day,
      int hour, int minute, int second, IndexWriter iw) throws IOException {
    Document d = new Document();
    d.add(new Field("f", content, Field.Store.YES, Field.Index.ANALYZED));
    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    cal.set(year, month - 1, day, hour, minute, second);
    d.add(new Field("date", DateField.dateToString(cal.getTime()),
        Field.Store.YES, Field.Index.NOT_ANALYZED));
    iw.addDocument(d);
  }
  @Override
  protected void tearDown() throws Exception {
    BooleanQuery.setMaxClauseCount(originalMaxClauses);
    super.tearDown();
  }
}
