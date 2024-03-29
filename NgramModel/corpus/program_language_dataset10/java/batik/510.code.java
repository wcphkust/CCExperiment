package org.apache.batik.dom.svg;
import java.awt.geom.AffineTransform;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGTransform;
public abstract class AbstractSVGTransform implements SVGTransform {
    protected short type = SVG_TRANSFORM_UNKNOWN;
    protected AffineTransform affineTransform;
    protected float angle;
    protected float x;
    protected float y;
    protected abstract SVGMatrix createMatrix();
    protected void setType(short type) {
        this.type = type;
    }
    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }
    public void assign(AbstractSVGTransform t) {
        this.type = t.type;
        this.affineTransform = t.affineTransform;
        this.angle = t.angle;
        this.x = t.x;
        this.y = t.y;
    }
    public short getType() {
        return type;
    }
    public SVGMatrix getMatrix() {
        return createMatrix();
    }
    public float getAngle() {
        return angle;
    }
    public void setMatrix(SVGMatrix matrix) {
        type = SVG_TRANSFORM_MATRIX;
        affineTransform =
            new AffineTransform(matrix.getA(), matrix.getB(), matrix.getC(),
                                matrix.getD(), matrix.getE(), matrix.getF());
    }
    public void setTranslate(float tx, float ty) {
        type = SVG_TRANSFORM_TRANSLATE;
        affineTransform = AffineTransform.getTranslateInstance(tx, ty);
    }
    public void setScale(float sx, float sy) {
        type = SVG_TRANSFORM_SCALE;
        affineTransform = AffineTransform.getScaleInstance(sx, sy);
    }
    public void setRotate(float angle, float cx, float cy) {
        type = SVG_TRANSFORM_ROTATE;
        affineTransform =
            AffineTransform.getRotateInstance(Math.toRadians(angle), cx, cy);
        this.angle = angle;
        this.x = cx;
        this.y = cy;
    }
    public void setSkewX(float angle) {
        type = SVG_TRANSFORM_SKEWX;
        affineTransform =
            AffineTransform.getShearInstance(Math.tan(Math.toRadians(angle)),
                                             0.0);
        this.angle = angle;
    }
    public void setSkewY(float angle) {
        type = SVG_TRANSFORM_SKEWY;
        affineTransform =
            AffineTransform.getShearInstance(0.0,
                                             Math.tan(Math.toRadians(angle)));
        this.angle = angle;
    }
}
