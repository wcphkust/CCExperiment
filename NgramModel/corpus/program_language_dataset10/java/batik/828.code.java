package org.apache.batik.ext.awt.image.renderable;
import java.util.List;
import org.apache.batik.ext.awt.image.ARGBChannel;
public interface DisplacementMapRable extends FilterColorInterpolation {
    int CHANNEL_R = 1;
    int CHANNEL_G = 2;
    int CHANNEL_B = 3;
    int CHANNEL_A = 4;
    void setSources(List srcs);
    void setScale(double scale);
    double getScale();
    void setXChannelSelector(ARGBChannel xChannelSelector);
    ARGBChannel getXChannelSelector();
    void setYChannelSelector(ARGBChannel yChannelSelector);
    ARGBChannel getYChannelSelector();
}
