package org.apache.batik.ext.awt.image.renderable;
import java.util.List;
import org.apache.batik.ext.awt.image.CompositeRule;
public interface CompositeRable extends FilterColorInterpolation {
    void setSources(List srcs);
    void setCompositeRule(CompositeRule cr);
    CompositeRule getCompositeRule();
}
