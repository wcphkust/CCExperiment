package org.apache.log4j.helpers;
public class Transform {
   private static final String CDATA_START  = "<![CDATA[";
   private static final String CDATA_END    = "]]>";
   private static final String CDATA_PSEUDO_END = "]]&gt;";
   private static final String CDATA_EMBEDED_END = CDATA_END + CDATA_PSEUDO_END + CDATA_START;
   private static final int CDATA_END_LEN = CDATA_END.length();
  static public String escapeTags(final String input) {
    if(input == null
       || input.length() == 0
       || (input.indexOf('"') == -1 &&
           input.indexOf('&') == -1 &&
           input.indexOf('<') == -1 &&
           input.indexOf('>') == -1)) {
      return input;
    }
    StringBuffer buf = new StringBuffer(input.length() + 6);
    char ch = ' ';
    int len = input.length();
    for(int i=0; i < len; i++) {
      ch = input.charAt(i);
      if (ch > '>') {
          buf.append(ch);
      } else if(ch == '<') {
	      buf.append("&lt;");
      } else if(ch == '>') {
	      buf.append("&gt;");
      } else if(ch == '&') {
	      buf.append("&amp;");
      } else if(ch == '"') {
	      buf.append("&quot;");
      } else {
	      buf.append(ch);
      }
    }
    return buf.toString();
  }
  static public void appendEscapingCDATA(final StringBuffer buf,
                                         final String str) {
      if (str != null) {
          int end = str.indexOf(CDATA_END);
          if (end < 0) {
              buf.append(str);
          } else {
              int start = 0;
              while (end > -1) {
                  buf.append(str.substring(start, end));
                  buf.append(CDATA_EMBEDED_END);
                  start = end + CDATA_END_LEN;
                  if (start < str.length()) {
                      end = str.indexOf(CDATA_END, start);
                  } else {
                      return;
                  }
              }
              buf.append(str.substring(start));
          }
      }
  }
}
