package org.apache.lucene.analysis.shingle;
import java.io.IOException;
import java.util.LinkedList;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
public final class ShingleFilter extends TokenFilter {
  public static final char[] FILLER_TOKEN = { '_' };
  public static final int DEFAULT_MAX_SHINGLE_SIZE = 2;
  public static final int DEFAULT_MIN_SHINGLE_SIZE = 2;
  public static final String DEFAULT_TOKEN_TYPE = "shingle";
  public static final String TOKEN_SEPARATOR = " ";
  private LinkedList<State> inputWindow = new LinkedList<State>();
  private CircularSequence gramSize;
  private StringBuilder shingleBuilder = new StringBuilder();
  private String tokenType = DEFAULT_TOKEN_TYPE;
  private String tokenSeparator = TOKEN_SEPARATOR;
  private boolean outputUnigrams = true;
  private int maxShingleSize;
  private int minShingleSize;
  private int numFillerTokensToInsert;
  private State nextInputStreamToken;
  private final TermAttribute termAtt;
  private final OffsetAttribute offsetAtt;
  private final PositionIncrementAttribute posIncrAtt;
  private final TypeAttribute typeAtt;
  public ShingleFilter(TokenStream input, int minShingleSize, int maxShingleSize) {
    super(input);
    setMaxShingleSize(maxShingleSize);
    setMinShingleSize(minShingleSize);
    this.termAtt = addAttribute(TermAttribute.class);
    this.offsetAtt = addAttribute(OffsetAttribute.class);
    this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    this.typeAtt = addAttribute(TypeAttribute.class);
  }
  public ShingleFilter(TokenStream input, int maxShingleSize) {
    this(input, DEFAULT_MIN_SHINGLE_SIZE, maxShingleSize);
  }
  public ShingleFilter(TokenStream input) {
    this(input, DEFAULT_MIN_SHINGLE_SIZE, DEFAULT_MAX_SHINGLE_SIZE);
  }
  public ShingleFilter(TokenStream input, String tokenType) {
    this(input, DEFAULT_MIN_SHINGLE_SIZE, DEFAULT_MAX_SHINGLE_SIZE);
    setTokenType(tokenType);
  }
  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }
  public void setOutputUnigrams(boolean outputUnigrams) {
    this.outputUnigrams = outputUnigrams;
    gramSize = new CircularSequence();
  }
  public void setMaxShingleSize(int maxShingleSize) {
    if (maxShingleSize < 2) {
      throw new IllegalArgumentException("Max shingle size must be >= 2");
    }
    this.maxShingleSize = maxShingleSize;
  }
  public void setMinShingleSize(int minShingleSize) {
    if (minShingleSize < 2) {
      throw new IllegalArgumentException("Min shingle size must be >= 2");
    }
    if (minShingleSize > maxShingleSize) {
      throw new IllegalArgumentException
        ("Min shingle size must be <= max shingle size");
    }
    this.minShingleSize = minShingleSize;
    gramSize = new CircularSequence();
  }
  public void setTokenSeparator(String tokenSeparator) {
    this.tokenSeparator = null == tokenSeparator ? "" : tokenSeparator;
  }
  @Override
  public final boolean incrementToken() throws IOException {
    boolean tokenAvailable = false; 
    if (gramSize.atMinValue() || inputWindow.size() < gramSize.getValue()) {
      shiftInputWindow();
    }
    if ( ! inputWindow.isEmpty()) {
      restoreState(inputWindow.getFirst());
      if (1 == gramSize.getValue()) {
        posIncrAtt.setPositionIncrement(1);
        gramSize.advance();
        tokenAvailable = true;
      } else if (inputWindow.size() >= gramSize.getValue()) {
        getNextShingle();
        gramSize.advance();
        tokenAvailable = true;
      }
    }
    return tokenAvailable;
  }
  private void getNextShingle() {
    int startOffset = offsetAtt.startOffset();
    int minTokNum = gramSize.getValue() - 1; 
    if (gramSize.getValue() == minShingleSize) {
      shingleBuilder.setLength(0);
      minTokNum = 0;
    }
    for (int tokNum = minTokNum ; tokNum < gramSize.getValue() ; ++tokNum) {
      if (tokNum > 0) {
        shingleBuilder.append(tokenSeparator);
      }
      restoreState(inputWindow.get(tokNum));
      shingleBuilder.append(termAtt.termBuffer(), 0, termAtt.termLength());
    }
    char[] termBuffer = termAtt.termBuffer();
    int termLength = shingleBuilder.length();
    if (termBuffer.length < termLength) {
      termBuffer = termAtt.resizeTermBuffer(termLength);
    }
    shingleBuilder.getChars(0, termLength, termBuffer, 0);
    termAtt.setTermLength(termLength);
    posIncrAtt.setPositionIncrement(gramSize.atMinValue() ? 1 : 0);
    typeAtt.setType(tokenType);
    offsetAtt.setOffset(startOffset, offsetAtt.endOffset());
  }
  private boolean getNextToken() throws IOException {
    boolean success = false;
    if (numFillerTokensToInsert > 0) {
      insertFillerToken();
      success = true;
    } else if (null != nextInputStreamToken) {
      restoreState(nextInputStreamToken);
      nextInputStreamToken = null;
      success = true;
    } else if (input.incrementToken()) {
      if (posIncrAtt.getPositionIncrement() > 1) {
        numFillerTokensToInsert = posIncrAtt.getPositionIncrement() - 1;
        insertFillerToken();
      }
      success = true;
    }
    return success;
	}
  private void insertFillerToken() {
    if (null == nextInputStreamToken) {
      nextInputStreamToken = captureState();
    } else {
      restoreState(nextInputStreamToken);
    }
    --numFillerTokensToInsert;
    offsetAtt.setOffset(offsetAtt.startOffset(), offsetAtt.startOffset());
    termAtt.setTermBuffer(FILLER_TOKEN, 0, FILLER_TOKEN.length);
  }
  private void shiftInputWindow() throws IOException {
    if (inputWindow.size() > 0) {
      inputWindow.removeFirst();
    }
    while (getNextToken()) {
      inputWindow.add(captureState());
      if (inputWindow.size() == maxShingleSize) {
        break;
      }
    }
    gramSize.reset();
  }
  @Override
  public void reset() throws IOException {
    super.reset();
    gramSize.reset();
    inputWindow.clear();
    numFillerTokensToInsert = 0;
  }
  private class CircularSequence {
    private int value;
    private int minValue;
    public CircularSequence() {
      minValue = outputUnigrams ? 1 : minShingleSize;
      reset();
    }
    public int getValue() {
      return value;
    }
    public int advance() {
      if (value == 1) {
        value = minShingleSize;
      } else if (value == maxShingleSize) {
        reset();
      } else {
        ++value;
      }
      return value;
    }
    public void reset() {
      value = minValue;
    }
    public boolean atMinValue() {
      return value == minValue;
    }
  }
}
