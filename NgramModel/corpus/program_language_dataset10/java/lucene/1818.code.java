package org.apache.lucene.util;
final public class UnicodeUtil {
  public static final int UNI_SUR_HIGH_START = 0xD800;
  public static final int UNI_SUR_HIGH_END = 0xDBFF;
  public static final int UNI_SUR_LOW_START = 0xDC00;
  public static final int UNI_SUR_LOW_END = 0xDFFF;
  public static final int UNI_REPLACEMENT_CHAR = 0xFFFD;
  private static final long UNI_MAX_BMP = 0x0000FFFF;
  private static final int HALF_BASE = 0x0010000;
  private static final long HALF_SHIFT = 10;
  private static final long HALF_MASK = 0x3FFL;
  public static final class UTF8Result {
    public byte[] result = new byte[10];
    public int length;
    public void setLength(int newLength) {
      if (result.length < newLength) {
        result = ArrayUtil.grow(result, newLength);
      }
      length = newLength;
    }
  }
  public static final class UTF16Result {
    public char[] result = new char[10];
    public int[] offsets = new int[10];
    public int length;
    public void setLength(int newLength) {
      if (result.length < newLength) {
        result = ArrayUtil.grow(result, newLength);
      }
      length = newLength;
    }
    public void copyText(UTF16Result other) {
      setLength(other.length);
      System.arraycopy(other.result, 0, result, 0, length);
    }
  }
  public static void UTF16toUTF8(final char[] source, final int offset, UTF8Result result) {
    int upto = 0;
    int i = offset;
    byte[] out = result.result;
    while(true) {
      final int code = (int) source[i++];
      if (upto+4 > out.length) {
        out = result.result = ArrayUtil.grow(out, upto+4);
      }
      if (code < 0x80)
        out[upto++] = (byte) code;
      else if (code < 0x800) {
        out[upto++] = (byte) (0xC0 | (code >> 6));
        out[upto++] = (byte)(0x80 | (code & 0x3F));
      } else if (code < 0xD800 || code > 0xDFFF) {
        if (code == 0xffff)
          break;
        out[upto++] = (byte)(0xE0 | (code >> 12));
        out[upto++] = (byte)(0x80 | ((code >> 6) & 0x3F));
        out[upto++] = (byte)(0x80 | (code & 0x3F));
      } else {
        if (code < 0xDC00 && source[i] != 0xffff) {
          int utf32 = (int) source[i];
          if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) { 
            utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
            i++;
            out[upto++] = (byte)(0xF0 | (utf32 >> 18));
            out[upto++] = (byte)(0x80 | ((utf32 >> 12) & 0x3F));
            out[upto++] = (byte)(0x80 | ((utf32 >> 6) & 0x3F));
            out[upto++] = (byte)(0x80 | (utf32 & 0x3F));
            continue;
          }
        }
        out[upto++] = (byte) 0xEF;
        out[upto++] = (byte) 0xBF;
        out[upto++] = (byte) 0xBD;
      }
    }
    result.length = upto;
  }
  public static void UTF16toUTF8(final char[] source, final int offset, final int length, UTF8Result result) {
    int upto = 0;
    int i = offset;
    final int end = offset + length;
    byte[] out = result.result;
    while(i < end) {
      final int code = (int) source[i++];
      if (upto+4 > out.length) {
        out = result.result = ArrayUtil.grow(out, upto+4);
      }
      if (code < 0x80)
        out[upto++] = (byte) code;
      else if (code < 0x800) {
        out[upto++] = (byte) (0xC0 | (code >> 6));
        out[upto++] = (byte)(0x80 | (code & 0x3F));
      } else if (code < 0xD800 || code > 0xDFFF) {
        out[upto++] = (byte)(0xE0 | (code >> 12));
        out[upto++] = (byte)(0x80 | ((code >> 6) & 0x3F));
        out[upto++] = (byte)(0x80 | (code & 0x3F));
      } else {
        if (code < 0xDC00 && i < end && source[i] != 0xffff) {
          int utf32 = (int) source[i];
          if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) { 
            utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
            i++;
            out[upto++] = (byte)(0xF0 | (utf32 >> 18));
            out[upto++] = (byte)(0x80 | ((utf32 >> 12) & 0x3F));
            out[upto++] = (byte)(0x80 | ((utf32 >> 6) & 0x3F));
            out[upto++] = (byte)(0x80 | (utf32 & 0x3F));
            continue;
          }
        }
        out[upto++] = (byte) 0xEF;
        out[upto++] = (byte) 0xBF;
        out[upto++] = (byte) 0xBD;
      }
    }
    result.length = upto;
  }
  public static void UTF16toUTF8(final String s, final int offset, final int length, UTF8Result result) {
    final int end = offset + length;
    byte[] out = result.result;
    int upto = 0;
    for(int i=offset;i<end;i++) {
      final int code = (int) s.charAt(i);
      if (upto+4 > out.length) {
        out = result.result = ArrayUtil.grow(out, upto+4);
      }
      if (code < 0x80)
        out[upto++] = (byte) code;
      else if (code < 0x800) {
        out[upto++] = (byte) (0xC0 | (code >> 6));
        out[upto++] = (byte)(0x80 | (code & 0x3F));
      } else if (code < 0xD800 || code > 0xDFFF) {
        out[upto++] = (byte)(0xE0 | (code >> 12));
        out[upto++] = (byte)(0x80 | ((code >> 6) & 0x3F));
        out[upto++] = (byte)(0x80 | (code & 0x3F));
      } else {
        if (code < 0xDC00 && (i < end-1)) {
          int utf32 = (int) s.charAt(i+1);
          if (utf32 >= 0xDC00 && utf32 <= 0xDFFF) { 
            utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
            i++;
            out[upto++] = (byte)(0xF0 | (utf32 >> 18));
            out[upto++] = (byte)(0x80 | ((utf32 >> 12) & 0x3F));
            out[upto++] = (byte)(0x80 | ((utf32 >> 6) & 0x3F));
            out[upto++] = (byte)(0x80 | (utf32 & 0x3F));
            continue;
          }
        }
        out[upto++] = (byte) 0xEF;
        out[upto++] = (byte) 0xBF;
        out[upto++] = (byte) 0xBD;
      }
    }
    result.length = upto;
  }
  public static void UTF8toUTF16(final byte[] utf8, final int offset, final int length, final UTF16Result result) {
    final int end = offset + length;
    char[] out = result.result;
    if (result.offsets.length <= end) {
      result.offsets = ArrayUtil.grow(result.offsets, end+1);
    }
    final int[] offsets = result.offsets;
    int upto = offset;
    while(offsets[upto] == -1)
      upto--;
    int outUpto = offsets[upto];
    if (outUpto+length >= out.length) {
      out = result.result = ArrayUtil.grow(out, outUpto+length+1);
    }
    while (upto < end) {
      final int b = utf8[upto]&0xff;
      final int ch;
      offsets[upto++] = outUpto;
      if (b < 0xc0) {
        assert b < 0x80;
        ch = b;
      } else if (b < 0xe0) {
        ch = ((b&0x1f)<<6) + (utf8[upto]&0x3f);
        offsets[upto++] = -1;
      } else if (b < 0xf0) {
        ch = ((b&0xf)<<12) + ((utf8[upto]&0x3f)<<6) + (utf8[upto+1]&0x3f);
        offsets[upto++] = -1;
        offsets[upto++] = -1;
      } else {
        assert b < 0xf8;
        ch = ((b&0x7)<<18) + ((utf8[upto]&0x3f)<<12) + ((utf8[upto+1]&0x3f)<<6) + (utf8[upto+2]&0x3f);
        offsets[upto++] = -1;
        offsets[upto++] = -1;
        offsets[upto++] = -1;
      }
      if (ch <= UNI_MAX_BMP) {
        out[outUpto++] = (char) ch;
      } else {
        final int chHalf = ch - HALF_BASE;
        out[outUpto++] = (char) ((chHalf >> HALF_SHIFT) + UNI_SUR_HIGH_START);
        out[outUpto++] = (char) ((chHalf & HALF_MASK) + UNI_SUR_LOW_START);
      }
    }
    offsets[upto] = outUpto;
    result.length = outUpto;
  }
}
