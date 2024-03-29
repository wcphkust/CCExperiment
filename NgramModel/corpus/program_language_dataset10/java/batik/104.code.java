package org.apache.batik.apps.svgbrowser;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
public class TransformHistory {
    protected List transforms = new ArrayList();
    protected int position = -1;
    public void back() {
        position -= 2;
    }
    public boolean canGoBack() {
        return position > 0;
    }
    public void forward() {
    }
    public boolean canGoForward() {
        return position < transforms.size() - 1;
    }
    public AffineTransform currentTransform() {
        return (AffineTransform)transforms.get(position + 1);
    }
    public void update(AffineTransform at) {
        if (position < -1) {
            position = -1;
        }
        if (++position < transforms.size()) {
            if (!transforms.get(position).equals(at)) {
                transforms = transforms.subList(0, position + 1);
            }
            transforms.set(position, at);
        } else {
            transforms.add(at);
        }
    }
}
