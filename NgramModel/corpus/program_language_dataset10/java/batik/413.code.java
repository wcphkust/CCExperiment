package org.apache.batik.css.parser;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SelectorList;
public class CSSSelectorList implements SelectorList {
    protected Selector[] list = new Selector[3];
    protected int length;
    public int getLength() {
        return length;
    }
    public Selector item(int index) {
        if (index < 0 || index >= length) {
            return null;
        }
        return list[index];
    }
    public void append(Selector item) {
        if (length == list.length) {
            Selector[] tmp = list;
            list = new Selector[ 1+ list.length + list.length / 2];
            System.arraycopy( tmp, 0, list, 0, tmp.length );
        }
        list[length++] = item;
    }
}