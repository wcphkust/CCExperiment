package org.apache.tools.ant.types.optional.image;
import javax.media.jai.PlanarImage;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
public class Rectangle extends BasicShape implements DrawOperation {
    protected int width = 0;
    protected int height = 0;
    protected int arcwidth = 0;
    protected int archeight = 0;
    public void setWidth(int w) {
        width = w;
    }
    public void setHeight(int h) {
        height = h;
    }
    public void setArcwidth(int w) {
        arcwidth = w;
    }
    public void setArcheight(int h) {
        archeight = h;
    }
    public PlanarImage executeDrawOperation() {
        log("\tCreating Rectangle w=" + width + " h=" + height + " arcw="
            + arcwidth + " arch=" + archeight);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
        Graphics2D graphics = (Graphics2D) bi.getGraphics();
        if (!stroke.equals("transparent")) {
            BasicStroke bStroke = new BasicStroke(stroke_width);
            graphics.setColor(ColorMapper.getColorByName(stroke));
            graphics.setStroke(bStroke);
            if ((arcwidth != 0) || (archeight != 0)) {
                graphics.drawRoundRect(0, 0, width, height, arcwidth, archeight);
            } else {
                graphics.drawRect(0, 0, width, height);
            }
        }
        if (!fill.equals("transparent")) {
            graphics.setColor(ColorMapper.getColorByName(fill));
            if ((arcwidth != 0) || (archeight != 0)) {
                graphics.fillRoundRect(stroke_width, stroke_width,
                    width - (stroke_width * 2), height - (stroke_width * 2),
                    arcwidth, archeight);
            } else {
                graphics.fillRect(stroke_width, stroke_width,
                    width - (stroke_width * 2), height - (stroke_width * 2));
            }
        }
        for (int i = 0; i < instructions.size(); i++) {
            ImageOperation instr = ((ImageOperation) instructions.elementAt(i));
            if (instr instanceof DrawOperation) {
                PlanarImage img = ((DrawOperation) instr).executeDrawOperation();
                graphics.drawImage(img.getAsBufferedImage(), null, 0, 0);
            } else if (instr instanceof TransformOperation) {
                graphics = (Graphics2D) bi.getGraphics();
                PlanarImage image
                    = ((TransformOperation) instr)
                    .executeTransformOperation(PlanarImage.wrapRenderedImage(bi));
                bi = image.getAsBufferedImage();
            }
        }
        return PlanarImage.wrapRenderedImage(bi);
    }
}
