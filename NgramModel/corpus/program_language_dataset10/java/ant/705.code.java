package org.apache.tools.ant.types.resources.selectors;
import java.util.Iterator;
import org.apache.tools.ant.types.Resource;
public class And extends ResourceSelectorContainer implements ResourceSelector {
    public And() {
    }
    public And(ResourceSelector[] r) {
        super(r);
    }
    public boolean isSelected(Resource r) {
        for (Iterator i = getSelectors(); i.hasNext();) {
            if (!((ResourceSelector) i.next()).isSelected(r)) {
                return false;
            }
        }
        return true;
    }
}