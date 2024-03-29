package org.apache.tools.ant.types.optional.image;
import org.apache.tools.ant.types.EnumeratedAttribute;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
public class Scale extends TransformOperation implements DrawOperation {
    private static final int HUNDRED = 100;
    private String widthStr = "100%";
    private String heightStr = "100%";
    private boolean xPercent = true;
    private boolean yPercent = true;
    private String proportions = "ignore";
    public static class ProportionsAttribute extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"ignore", "width", "height", "cover", "fit"};
        }
    }
    public void setProportions(ProportionsAttribute pa) {
        proportions = pa.getValue();
    }
    public void setWidth(String width) {
        widthStr = width;
    }
    public void setHeight(String height) {
        heightStr = height;
    }
    public float getWidth() {
        float width = 0.0F;
        int percIndex = widthStr.indexOf('%');
        if (percIndex > 0) {
            width = Float.parseFloat(widthStr.substring(0, percIndex));
            xPercent = true;
            return width / HUNDRED;
        } else {
            xPercent = false;
            return Float.parseFloat(widthStr);
        }
    }
    public float getHeight() {
        int percIndex = heightStr.indexOf('%');
        if (percIndex > 0) {
            float height = Float.parseFloat(heightStr.substring(0, percIndex));
            yPercent = true;
            return height / HUNDRED;
        } else {
            yPercent = false;
            return Float.parseFloat(heightStr);
        }
    }
    public PlanarImage performScale(PlanarImage image) {
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        float xFl = getWidth();
        float yFl = getHeight();
        if (!xPercent) {
            xFl = (xFl / image.getWidth());
        }
        if (!yPercent) {
            yFl = (yFl / image.getHeight());
        }
        if ("width".equals(proportions)) {
            yFl = xFl;
        } else if ("height".equals(proportions)) {
            xFl = yFl;
        } else if ("fit".equals(proportions)) {
            yFl = Math.min(xFl, yFl);
            xFl = yFl;
        } else if ("cover".equals(proportions)) {
            yFl = Math.max(xFl, yFl);
            xFl = yFl;
        }
        pb.add(new Float(xFl));
        pb.add(new Float(yFl));
        log("\tScaling to " + (xFl * HUNDRED) + "% x "
            + (yFl * HUNDRED) + "%");
        return JAI.create("scale", pb);
    }
    public PlanarImage executeTransformOperation(PlanarImage image) {
        BufferedImage bi = null;
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                return performScale(image);
            } else if (instr instanceof TransformOperation) {
                bi = image.getAsBufferedImage();
                image = ((TransformOperation) instr)
                    .executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
                bi = image.getAsBufferedImage();
            }
        }
        return performScale(image);
    }
    public PlanarImage executeDrawOperation() {
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                PlanarImage image = null;
                performScale(image);
                return image;
            }
        }
        return null;
    }
}
