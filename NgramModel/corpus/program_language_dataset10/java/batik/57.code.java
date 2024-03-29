package org.apache.batik.anim.values;
import org.apache.batik.dom.anim.AnimationTarget;
public class AnimatableStringValue extends AnimatableValue {
    protected String string;
    protected AnimatableStringValue(AnimationTarget target) {
        super(target);
    }
    public AnimatableStringValue(AnimationTarget target, String s) {
        super(target);
        string = s;
    }
    public AnimatableValue interpolate(AnimatableValue result,
                                       AnimatableValue to, float interpolation,
                                       AnimatableValue accumulation,
                                       int multiplier) {
        AnimatableStringValue res;
        if (result == null) {
            res = new AnimatableStringValue(target);
        } else {
            res = (AnimatableStringValue) result;
        }
        String newString;
        if (to != null && interpolation >= 0.5) {
            AnimatableStringValue toValue =
                (AnimatableStringValue) to;
            newString = toValue.string;
        } else {
            newString = string;
        }
        if (res.string == null || !res.string.equals(newString)) {
            res.string = newString;
            res.hasChanged = true;
        }
        return res;
    }
    public String getString() {
        return string;
    }
    public boolean canPace() {
        return false;
    }
    public float distanceTo(AnimatableValue other) {
        return 0f;
    }
    public AnimatableValue getZeroValue() {
        return new AnimatableStringValue(target, "");
    }
    public String getCssText() {
        return string;
    }
}
