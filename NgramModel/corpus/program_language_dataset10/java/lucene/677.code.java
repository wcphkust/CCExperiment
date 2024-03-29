package org.apache.lucene.analysis.miscellaneous;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;
import java.io.IOException;
public class PrefixAwareTokenFilter extends TokenStream {
  private TokenStream prefix;
  private TokenStream suffix;
  private TermAttribute termAtt;
  private PositionIncrementAttribute posIncrAtt;
  private PayloadAttribute payloadAtt;
  private OffsetAttribute offsetAtt;
  private TypeAttribute typeAtt;
  private FlagsAttribute flagsAtt;
  private TermAttribute p_termAtt;
  private PositionIncrementAttribute p_posIncrAtt;
  private PayloadAttribute p_payloadAtt;
  private OffsetAttribute p_offsetAtt;
  private TypeAttribute p_typeAtt;
  private FlagsAttribute p_flagsAtt;
  public PrefixAwareTokenFilter(TokenStream prefix, TokenStream suffix) {
    super(suffix);
    this.suffix = suffix;
    this.prefix = prefix;
    prefixExhausted = false;
    termAtt = addAttribute(TermAttribute.class);
    posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    payloadAtt = addAttribute(PayloadAttribute.class);
    offsetAtt = addAttribute(OffsetAttribute.class);
    typeAtt = addAttribute(TypeAttribute.class);
    flagsAtt = addAttribute(FlagsAttribute.class);
    p_termAtt = prefix.addAttribute(TermAttribute.class);
    p_posIncrAtt = prefix.addAttribute(PositionIncrementAttribute.class);
    p_payloadAtt = prefix.addAttribute(PayloadAttribute.class);
    p_offsetAtt = prefix.addAttribute(OffsetAttribute.class);
    p_typeAtt = prefix.addAttribute(TypeAttribute.class);
    p_flagsAtt = prefix.addAttribute(FlagsAttribute.class);
  }
  private Token previousPrefixToken = new Token();
  private Token reusableToken = new Token();
  private boolean prefixExhausted;
  @Override
  public final boolean incrementToken() throws IOException {
    if (!prefixExhausted) {
      Token nextToken = getNextPrefixInputToken(reusableToken);
      if (nextToken == null) {
        prefixExhausted = true;
      } else {
        previousPrefixToken.reinit(nextToken);
        Payload p = previousPrefixToken.getPayload();
        if (p != null) {
          previousPrefixToken.setPayload((Payload) p.clone());
        }
        setCurrentToken(nextToken);
        return true;
      }
    }
    Token nextToken = getNextSuffixInputToken(reusableToken);
    if (nextToken == null) {
      return false;
    }
    nextToken = updateSuffixToken(nextToken, previousPrefixToken);
    setCurrentToken(nextToken);
    return true;
  }
  private void setCurrentToken(Token token) {
    if (token == null) return;
    clearAttributes();
    termAtt.setTermBuffer(token.termBuffer(), 0, token.termLength());
    posIncrAtt.setPositionIncrement(token.getPositionIncrement());
    flagsAtt.setFlags(token.getFlags());
    offsetAtt.setOffset(token.startOffset(), token.endOffset());
    typeAtt.setType(token.type());
    payloadAtt.setPayload(token.getPayload());
  }
  private Token getNextPrefixInputToken(Token token) throws IOException {
    if (!prefix.incrementToken()) return null;
    token.setTermBuffer(p_termAtt.termBuffer(), 0, p_termAtt.termLength());
    token.setPositionIncrement(p_posIncrAtt.getPositionIncrement());
    token.setFlags(p_flagsAtt.getFlags());
    token.setOffset(p_offsetAtt.startOffset(), p_offsetAtt.endOffset());
    token.setType(p_typeAtt.type());
    token.setPayload(p_payloadAtt.getPayload());
    return token;
  }
  private Token getNextSuffixInputToken(Token token) throws IOException {
    if (!suffix.incrementToken()) return null;
    token.setTermBuffer(termAtt.termBuffer(), 0, termAtt.termLength());
    token.setPositionIncrement(posIncrAtt.getPositionIncrement());
    token.setFlags(flagsAtt.getFlags());
    token.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset());
    token.setType(typeAtt.type());
    token.setPayload(payloadAtt.getPayload());
    return token;
  }
  public Token updateSuffixToken(Token suffixToken, Token lastPrefixToken) {
    suffixToken.setStartOffset(lastPrefixToken.endOffset() + suffixToken.startOffset());
    suffixToken.setEndOffset(lastPrefixToken.endOffset() + suffixToken.endOffset());
    return suffixToken;
  }
  @Override
  public void close() throws IOException {
    prefix.close();
    suffix.close();
  }
  @Override
  public void reset() throws IOException {
    super.reset();
    if (prefix != null) {
      prefixExhausted = false;
      prefix.reset();
    }
    if (suffix != null) {
      suffix.reset();
    }
  }
  public TokenStream getPrefix() {
    return prefix;
  }
  public void setPrefix(TokenStream prefix) {
    this.prefix = prefix;
  }
  public TokenStream getSuffix() {
    return suffix;
  }
  public void setSuffix(TokenStream suffix) {
    this.suffix = suffix;
  }
}
