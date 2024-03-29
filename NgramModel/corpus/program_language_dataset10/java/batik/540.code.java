package org.apache.batik.dom.svg;
import org.apache.batik.anim.values.AnimatableIntegerValue;
import org.apache.batik.anim.values.AnimatableValue;
import org.apache.batik.dom.anim.AnimationTarget;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.svg.SVGAnimatedInteger;
public class SVGOMAnimatedInteger
        extends AbstractSVGAnimatedValue
        implements SVGAnimatedInteger {
    protected int defaultValue;
    protected boolean valid;
    protected int baseVal;
    protected int animVal;
    protected boolean changing;
    public SVGOMAnimatedInteger(AbstractElement elt,
                               String ns,
                               String ln,
                               int    val) {
        super(elt, ns, ln);
        defaultValue = val;
    }
    public int getBaseVal() {
        if (!valid) {
            update();
        }
        return baseVal;
    }
    protected void update() {
        Attr attr = element.getAttributeNodeNS(namespaceURI, localName);
        if (attr == null) {
            baseVal = defaultValue;
        } else {
            baseVal = Integer.parseInt(attr.getValue());
        }
        valid = true;
    }
    public void setBaseVal(int baseVal) throws DOMException {
        try {
            this.baseVal = baseVal;
            valid = true;
            changing = true;
            element.setAttributeNS(namespaceURI, localName,
                                   String.valueOf(baseVal));
        } finally {
            changing = false;
        }
    }
    public int getAnimVal() {
        if (hasAnimVal) {
            return animVal;
        }
        if (!valid) {
            update();
        }
        return baseVal;
    }
    public AnimatableValue getUnderlyingValue(AnimationTarget target) {
        return new AnimatableIntegerValue(target, getBaseVal());
    }
    protected void updateAnimatedValue(AnimatableValue val) {
        if (val == null) {
            hasAnimVal = false;
        } else {
            hasAnimVal = true;
            this.animVal = ((AnimatableIntegerValue) val).getValue();
        }
        fireAnimatedAttributeListeners();
    }
    public void attrAdded(Attr node, String newv) {
        if (!changing) {
            valid = false;
        }
        fireBaseAttributeListeners();
        if (!hasAnimVal) {
            fireAnimatedAttributeListeners();
        }
    }
    public void attrModified(Attr node, String oldv, String newv) {
        if (!changing) {
            valid = false;
        }
        fireBaseAttributeListeners();
        if (!hasAnimVal) {
            fireAnimatedAttributeListeners();
        }
    }
    public void attrRemoved(Attr node, String oldv) {
        if (!changing) {
            valid = false;
        }
        fireBaseAttributeListeners();
        if (!hasAnimVal) {
            fireAnimatedAttributeListeners();
        }
    }
}
