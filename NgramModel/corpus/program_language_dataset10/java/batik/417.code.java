package org.apache.batik.css.parser;
import org.w3c.css.sac.Selector;
import org.w3c.css.sac.SimpleSelector;
public class DefaultChildSelector extends AbstractDescendantSelector {
    public DefaultChildSelector(Selector ancestor, SimpleSelector simple) {
        super(ancestor, simple);
    }
    public short getSelectorType() {
        return SAC_CHILD_SELECTOR;
    }
    public String toString() {
        SimpleSelector s = getSimpleSelector();
        if (s.getSelectorType() == SAC_PSEUDO_ELEMENT_SELECTOR) {
            return String.valueOf( getAncestorSelector() ) + s;
        }
        return getAncestorSelector() + " > " + s;
    }
}
