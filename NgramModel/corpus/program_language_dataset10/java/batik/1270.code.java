package org.apache.batik.svggen.font.table;
public class Panose {
  byte bFamilyType = 0;
  byte bSerifStyle = 0;
  byte bWeight = 0;
  byte bProportion = 0;
  byte bContrast = 0;
  byte bStrokeVariation = 0;
  byte bArmStyle = 0;
  byte bLetterform = 0;
  byte bMidline = 0;
  byte bXHeight = 0;
  public Panose(byte[] panose) {
    bFamilyType = panose[0];
    bSerifStyle = panose[1];
    bWeight = panose[2];
    bProportion = panose[3];
    bContrast = panose[4];
    bStrokeVariation = panose[5];
    bArmStyle = panose[6];
    bLetterform = panose[7];
    bMidline = panose[8];
    bXHeight = panose[9];
  }
  public byte getFamilyType() {
    return bFamilyType;
  }
  public byte getSerifStyle() {
    return bSerifStyle;
  }
  public byte getWeight() {
    return bWeight;
  }
  public byte getProportion() {
    return bProportion;
  }
  public byte getContrast() {
    return bContrast;
  }
  public byte getStrokeVariation() {
    return bStrokeVariation;
  }
  public byte getArmStyle() {
    return bArmStyle;
  }
  public byte getLetterForm() {
    return bLetterform;
  }
  public byte getMidline() {
    return bMidline;
  }
  public byte getXHeight() {
    return bXHeight;
  }
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(String.valueOf(bFamilyType)).append(" ")
      .append(String.valueOf(bSerifStyle)).append(" ")
      .append(String.valueOf(bWeight)).append(" ")
      .append(String.valueOf(bProportion)).append(" ")
      .append(String.valueOf(bContrast)).append(" ")
      .append(String.valueOf(bStrokeVariation)).append(" ")
      .append(String.valueOf(bArmStyle)).append(" ")
      .append(String.valueOf(bLetterform)).append(" ")
      .append(String.valueOf(bMidline)).append(" ")
      .append(String.valueOf(bXHeight));
    return sb.toString();
  }
}
