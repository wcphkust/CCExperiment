package org.apache.log4j.varia;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.OptionConverter;
public class StringMatchFilter extends Filter {
  public static final String STRING_TO_MATCH_OPTION = "StringToMatch";
  public static final String ACCEPT_ON_MATCH_OPTION = "AcceptOnMatch";
  boolean acceptOnMatch = true;
  String stringToMatch;
  public
  String[] getOptionStrings() {
    return new String[] {STRING_TO_MATCH_OPTION, ACCEPT_ON_MATCH_OPTION};
  }
  public
  void setOption(String key, String value) { 
    if(key.equalsIgnoreCase(STRING_TO_MATCH_OPTION)) {
      stringToMatch = value;
    } else if (key.equalsIgnoreCase(ACCEPT_ON_MATCH_OPTION)) {
      acceptOnMatch = OptionConverter.toBoolean(value, acceptOnMatch);
    }
  }
  public
  void setStringToMatch(String s) {
    stringToMatch = s;
  }
  public
  String getStringToMatch() {
    return stringToMatch;
  }
  public
  void setAcceptOnMatch(boolean acceptOnMatch) {
    this.acceptOnMatch = acceptOnMatch;
  }
  public
  boolean getAcceptOnMatch() {
    return acceptOnMatch;
  }
  public
  int decide(LoggingEvent event) {
    String msg = event.getRenderedMessage();
    if(msg == null ||  stringToMatch == null)
      return Filter.NEUTRAL;
    if( msg.indexOf(stringToMatch) == -1 ) {
      return Filter.NEUTRAL;
    } else { 
      if(acceptOnMatch) {
	return Filter.ACCEPT;
      } else {
	return Filter.DENY;
      }
    }
  }
}