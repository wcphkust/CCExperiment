package org.apache.solr.search;
import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader;
import java.io.IOException;
public class MissingStringLastComparatorSource extends FieldComparatorSource {
  public static final String bigString="\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffffNULL_VAL";
  private final String missingValueProxy;
  public MissingStringLastComparatorSource() {
    this(bigString);
  }
  public MissingStringLastComparatorSource(String missingValueProxy) {
    this.missingValueProxy=missingValueProxy;
  }
  public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
    return new MissingLastOrdComparator(numHits, fieldname, sortPos, reversed, missingValueProxy);
  }
}
 class MissingLastOrdComparator extends FieldComparator {
    private static final int NULL_ORD = Integer.MAX_VALUE;
    private final String nullVal; 
    private final int[] ords;
    private final String[] values;
    private final int[] readerGen;
    private int currentReaderGen = -1;
    private String[] lookup;
    private int[] order;
    private final String field;
    private int bottomSlot = -1;
    private int bottomOrd;
    private String bottomValue;
    private final boolean reversed;
    private final int sortPos;
   public MissingLastOrdComparator(int numHits, String field, int sortPos, boolean reversed, String nullVal) {
      ords = new int[numHits];
      values = new String[numHits];
      readerGen = new int[numHits];
      this.sortPos = sortPos;
      this.reversed = reversed;
      this.field = field;
      this.nullVal = nullVal;
    }
    public int compare(int slot1, int slot2) {
      if (readerGen[slot1] == readerGen[slot2]) {
        int cmp = ords[slot1] - ords[slot2];
        if (cmp != 0) {
          return cmp;
        }
      }
      final String val1 = values[slot1];
      final String val2 = values[slot2];
      if (val1 == null) {
        if (val2 == null) {
          return 0;
        }
        return 1;
      } else if (val2 == null) {
        return -1;
      }
      return val1.compareTo(val2);
    }
    public int compareBottom(int doc) {
      assert bottomSlot != -1;
      int order = this.order[doc];
      int ord = (order == 0) ? NULL_ORD : order;
      final int cmp = bottomOrd - ord;
      if (cmp != 0) {
        return cmp;
      }
      final String val2 = lookup[order];
      if (bottomValue == val2) return 0;
      return bottomValue.compareTo(val2);
    }
    private void convert(int slot) {
      readerGen[slot] = currentReaderGen;
      int index = 0;
      String value = values[slot];
      if (value == null) {
        return;
      }
      if (sortPos == 0 && bottomSlot != -1 && bottomSlot != slot) {
        assert bottomOrd < lookup.length;
        if (reversed) {
          index = binarySearch(lookup, value, bottomOrd, lookup.length-1);
        } else {
          index = binarySearch(lookup, value, 0, bottomOrd);
        }
      } else {
        index = binarySearch(lookup, value);
      }
      if (index < 0) {
        index = -index - 2;
      }
      ords[slot] = index;
    }
    public void copy(int slot, int doc) {
      final int ord = order[doc];
      ords[slot] = ord == 0 ? NULL_ORD : ord;
      assert ord >= 0;
      values[slot] = lookup[ord];
      readerGen[slot] = currentReaderGen;
    }
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
      FieldCache.StringIndex currentReaderValues = FieldCache.DEFAULT.getStringIndex(reader, field);
      currentReaderGen++;
      order = currentReaderValues.order;
      lookup = currentReaderValues.lookup;
      assert lookup.length > 0;
      if (bottomSlot != -1) {
        convert(bottomSlot);
        bottomOrd = ords[bottomSlot];
      }
    }
    public void setBottom(final int bottom) {
      bottomSlot = bottom;
      if (readerGen[bottom] != currentReaderGen) {
        convert(bottomSlot);
      }
      bottomOrd = ords[bottom];
      assert bottomOrd >= 0;
      bottomValue = values[bottom];
    }
    public Comparable value(int slot) {
      Comparable v = values[slot];
      return v==null ? nullVal : v;
    }
    public String[] getValues() {
      return values;
    }
    public int getBottomSlot() {
      return bottomSlot;
    }
    public String getField() {
      return field;
    }
  }
