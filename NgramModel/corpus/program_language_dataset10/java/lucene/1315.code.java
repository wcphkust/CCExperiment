package org.apache.lucene.queryParser.surround.parser;
public interface QueryParserConstants {
  int EOF = 0;
  int _NUM_CHAR = 1;
  int _TERM_CHAR = 2;
  int _WHITESPACE = 3;
  int _STAR = 4;
  int _ONE_CHAR = 5;
  int _DISTOP_NUM = 6;
  int OR = 8;
  int AND = 9;
  int NOT = 10;
  int W = 11;
  int N = 12;
  int LPAREN = 13;
  int RPAREN = 14;
  int COMMA = 15;
  int COLON = 16;
  int CARAT = 17;
  int TRUNCQUOTED = 18;
  int QUOTED = 19;
  int SUFFIXTERM = 20;
  int TRUNCTERM = 21;
  int TERM = 22;
  int NUMBER = 23;
  int Boost = 0;
  int DEFAULT = 1;
  String[] tokenImage = {
    "<EOF>",
    "<_NUM_CHAR>",
    "<_TERM_CHAR>",
    "<_WHITESPACE>",
    "\"*\"",
    "\"?\"",
    "<_DISTOP_NUM>",
    "<token of kind 7>",
    "<OR>",
    "<AND>",
    "<NOT>",
    "<W>",
    "<N>",
    "\"(\"",
    "\")\"",
    "\",\"",
    "\":\"",
    "\"^\"",
    "<TRUNCQUOTED>",
    "<QUOTED>",
    "<SUFFIXTERM>",
    "<TRUNCTERM>",
    "<TERM>",
    "<NUMBER>",
  };
}
