package org.apache.tools.ant.types.selectors;
import java.io.File;
import java.util.Enumeration;
public class MajoritySelector extends BaseSelectorContainer {
    private boolean allowtie = true;
    public MajoritySelector() {
    }
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (hasSelectors()) {
            buf.append("{majorityselect: ");
            buf.append(super.toString());
            buf.append("}");
        }
        return buf.toString();
    }
    public void setAllowtie(boolean tiebreaker) {
        allowtie = tiebreaker;
    }
    public boolean isSelected(File basedir, String filename, File file) {
        validate();
        int yesvotes = 0;
        int novotes = 0;
        Enumeration e = selectorElements();
        boolean result;
        while (e.hasMoreElements()) {
            result = ((FileSelector) e.nextElement()).isSelected(basedir,
                    filename, file);
            if (result) {
                yesvotes = yesvotes + 1;
            } else {
                novotes = novotes + 1;
            }
        }
        if (yesvotes > novotes) {
            return true;
        } else if (novotes > yesvotes) {
            return false;
        }
        return allowtie;
    }
}