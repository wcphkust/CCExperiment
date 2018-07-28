package org.apache.tools.ant.types.resources.selectors;
import java.util.Iterator;
import org.apache.tools.ant.types.Resource;
public class Majority
    extends ResourceSelectorContainer implements ResourceSelector {
    private boolean tie = true;
    public Majority() {
    }
    public Majority(ResourceSelector[] r) {
        super(r);
    }
    public synchronized void setAllowtie(boolean b) {
        tie = b;
    }
    public synchronized boolean isSelected(Resource r) {
        int passed = 0;
        int failed = 0;
        int count = selectorCount();
        boolean even = count % 2 == 0;
        int threshold = count / 2;
        for (Iterator i = getSelectors(); i.hasNext();) {
            if (((ResourceSelector) i.next()).isSelected(r)) {
                ++passed;
                if (passed > threshold || (even && tie && passed == threshold)) {
                    return true;
                }
            } else {
                ++failed;
                if (failed > threshold || (even && !tie && failed == threshold)) {
                    return false;
                }
            }
        }
        return false;
    }
}