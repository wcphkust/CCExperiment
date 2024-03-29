package org.apache.batik.dom.svg;
import java.awt.geom.AffineTransform;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.TransformListHandler;
import org.apache.batik.parser.TransformListParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.svg.SVGException;
import org.w3c.dom.svg.SVGMatrix;
import org.w3c.dom.svg.SVGTransform;
import org.w3c.dom.svg.SVGTransformList;
public abstract class AbstractSVGTransformList
    extends AbstractSVGList
    implements SVGTransformList {
    public static final String SVG_TRANSFORMATION_LIST_SEPARATOR
        = "";
    protected String getItemSeparator() {
        return SVG_TRANSFORMATION_LIST_SEPARATOR;
    }
    protected abstract SVGException createSVGException(short type,
                                                       String key,
                                                       Object[] args);
    public SVGTransform initialize(SVGTransform newItem)
            throws DOMException, SVGException {
        return (SVGTransform) initializeImpl(newItem);
    }
    public SVGTransform getItem(int index) throws DOMException {
        return (SVGTransform) getItemImpl(index);
    }
    public SVGTransform insertItemBefore(SVGTransform newItem, int index)
            throws DOMException, SVGException {
        return (SVGTransform) insertItemBeforeImpl(newItem, index);
    }
    public SVGTransform replaceItem(SVGTransform newItem, int index)
            throws DOMException, SVGException {
        return (SVGTransform) replaceItemImpl(newItem, index);
    }
    public SVGTransform removeItem(int index) throws DOMException {
        return (SVGTransform) removeItemImpl(index);
    }
    public SVGTransform appendItem(SVGTransform newItem)
            throws DOMException, SVGException {
        return (SVGTransform) appendItemImpl(newItem);
    }
    public SVGTransform createSVGTransformFromMatrix(SVGMatrix matrix) {
        SVGOMTransform transform = new SVGOMTransform();
        transform.setMatrix(matrix);
        return transform;
    }
    public SVGTransform consolidate() {
        revalidate();
        int size = itemList.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return getItem(0);
        }
        SVGTransformItem t = (SVGTransformItem) getItemImpl(0);
        AffineTransform at = (AffineTransform) t.affineTransform.clone();
        for (int i = 1; i < size; i++) {
            t = (SVGTransformItem) getItemImpl(i);
            at.concatenate(t.affineTransform);
        }
        SVGOMMatrix matrix = new SVGOMMatrix(at);
        return initialize(createSVGTransformFromMatrix(matrix));
    }
    public AffineTransform getAffineTransform() {
        AffineTransform at = new AffineTransform();
        for (int i = 0; i < getNumberOfItems(); i++) {
            SVGTransformItem item = (SVGTransformItem) getItem(i);
            at.concatenate(item.affineTransform);
        }
        return at;
    }
    protected SVGItem createSVGItem(Object newItem) {
        return new SVGTransformItem((SVGTransform) newItem);
    }
    protected void doParse(String value, ListHandler handler)
            throws ParseException {
        TransformListParser transformListParser = new TransformListParser();
        TransformListBuilder builder = new TransformListBuilder(handler);
        transformListParser.setTransformListHandler(builder);
        transformListParser.parse(value);
    }
    protected void checkItemType(Object newItem) {
        if (!(newItem instanceof SVGTransform)) {
            createSVGException(SVGException.SVG_WRONG_TYPE_ERR,
                               "expected.transform", null);
        }
    }
    protected class SVGTransformItem
            extends AbstractSVGTransform
            implements SVGItem {
        protected boolean xOnly;
        protected boolean angleOnly;
        protected AbstractSVGList parent;
        protected String itemStringValue;
        protected SVGTransformItem() {
        }
        protected SVGTransformItem(SVGTransform transform) {
            assign(transform);
        }
        protected void resetAttribute() {
            if (parent != null) {
                itemStringValue = null;
                parent.itemChanged();
            }
        }
        public void setParent(AbstractSVGList list) {
            parent = list;
        }
        public AbstractSVGList getParent() {
            return parent;
        }
        public String getValueAsString() {
            if (itemStringValue == null) {
                itemStringValue = getStringValue();
            }
            return itemStringValue;
        }
        public void assign(SVGTransform transform) {
            type = transform.getType();
            SVGMatrix matrix = transform.getMatrix();
            switch (type) {
                case SVGTransform.SVG_TRANSFORM_TRANSLATE:
                    setTranslate(matrix.getE(), matrix.getF());
                    break;
                case SVGTransform.SVG_TRANSFORM_SCALE:
                    setScale(matrix.getA(), matrix.getD());
                    break;
                case SVGTransform.SVG_TRANSFORM_ROTATE:
                    if (matrix.getE() == 0.0f) {
                        rotate(transform.getAngle());
                    } else {
                        angleOnly = false;
                        if (matrix.getA() == 1.0f) {
                            setRotate(transform.getAngle(),
                                      matrix.getE(), matrix.getF());
                        } else if (transform instanceof AbstractSVGTransform) {
                            AbstractSVGTransform internal =
                                (AbstractSVGTransform) transform;
                            setRotate(internal.getAngle(),
                                      internal.getX(), internal.getY());
                        } else {
                        }
                    }
                    break;
                case SVGTransform.SVG_TRANSFORM_SKEWX:
                    setSkewX(transform.getAngle());
                    break;
                case SVGTransform.SVG_TRANSFORM_SKEWY:
                    setSkewY(transform.getAngle());
                    break;
                case SVGTransform.SVG_TRANSFORM_MATRIX:
                    setMatrix(matrix);
                    break;
            }
        }
        protected void translate(float x) {
            xOnly = true;
            setTranslate(x, 0.0f);
        }
        protected void rotate(float angle) {
            angleOnly = true;
            setRotate(angle, 0.0f, 0.0f);
        }
        protected void scale(float x) {
            xOnly = true;
            setScale(x, x);
        }
        protected void matrix(float a, float b, float c,
                              float d, float e, float f) {
            setMatrix(new SVGOMMatrix(new AffineTransform(a, b, c, d, e, f)));
        }
        public void setMatrix(SVGMatrix matrix) {
            super.setMatrix(matrix);
            resetAttribute();
        }
        public void setTranslate(float tx, float ty) {
            super.setTranslate(tx, ty);
            resetAttribute();
        }
        public void setScale(float sx, float sy) {
            super.setScale(sx, sy);
            resetAttribute();
        }
        public void setRotate(float angle, float cx, float cy) {
            super.setRotate(angle, cx, cy);
            resetAttribute();
        }
        public void setSkewX(float angle) {
            super.setSkewX(angle);
            resetAttribute();
        }
        public void setSkewY(float angle) {
            super.setSkewY(angle);
            resetAttribute();
        }
        protected SVGMatrix createMatrix() {
            return new AbstractSVGMatrix() {
                protected AffineTransform getAffineTransform() {
                    return SVGTransformItem.this.affineTransform;
                }
                public void setA(float a) throws DOMException {
                    SVGTransformItem.this.type = SVGTransform.SVG_TRANSFORM_MATRIX;
                    super.setA(a);
                    SVGTransformItem.this.resetAttribute();
                }
                public void setB(float b) throws DOMException {
                    SVGTransformItem.this.type = SVGTransform.SVG_TRANSFORM_MATRIX;
                    super.setB(b);
                    SVGTransformItem.this.resetAttribute();
                }
                public void setC(float c) throws DOMException {
                    SVGTransformItem.this.type = SVGTransform.SVG_TRANSFORM_MATRIX;
                    super.setC(c);
                    SVGTransformItem.this.resetAttribute();
                }
                public void setD(float d) throws DOMException {
                    SVGTransformItem.this.type = SVGTransform.SVG_TRANSFORM_MATRIX;
                    super.setD(d);
                    SVGTransformItem.this.resetAttribute();
                }
                public void setE(float e) throws DOMException {
                    SVGTransformItem.this.type = SVGTransform.SVG_TRANSFORM_MATRIX;
                    super.setE(e);
                    SVGTransformItem.this.resetAttribute();
                }
                public void setF(float f) throws DOMException {
                    SVGTransformItem.this.type = SVGTransform.SVG_TRANSFORM_MATRIX;
                    super.setF(f);
                    SVGTransformItem.this.resetAttribute();
                }
            };
        }
        protected String getStringValue(){
            StringBuffer buf = new StringBuffer();
            switch(type) {
                case SVGTransform.SVG_TRANSFORM_TRANSLATE:
                    buf.append("translate(");
                    buf.append((float) affineTransform.getTranslateX());
                    if (!xOnly) {
                        buf.append(' ');
                        buf.append((float) affineTransform.getTranslateY());
                    }
                    buf.append(')');
                    break;
                case SVGTransform.SVG_TRANSFORM_ROTATE:
                    buf.append("rotate(");
                    buf.append(angle);
                    if (!angleOnly) {
                        buf.append(' ');
                        buf.append(x);
                        buf.append(' ');
                        buf.append(y);
                    }
                    buf.append(')');
                    break;
                case SVGTransform.SVG_TRANSFORM_SCALE:
                    buf.append("scale(");
                    buf.append((float) affineTransform.getScaleX());
                    if (!xOnly) {
                        buf.append(' ');
                        buf.append((float) affineTransform.getScaleY());
                    }
                    buf.append(')');
                    break;
                case SVGTransform.SVG_TRANSFORM_SKEWX:
                    buf.append("skewX(");
                    buf.append(angle);
                    buf.append(')');
                    break;
                case SVGTransform.SVG_TRANSFORM_SKEWY:
                    buf.append("skewY(");
                    buf.append(angle);
                    buf.append(')');
                    break;
                case SVGTransform.SVG_TRANSFORM_MATRIX:
                    buf.append("matrix(");
                    double[] matrix = new double[6];
                    affineTransform.getMatrix(matrix);
                    for(int i = 0; i < 6; i++) {
                        if (i != 0) {
                            buf.append(' ');
                        }
                        buf.append((float) matrix[i]);
                    }
                    buf.append(')');
                    break;
            }
            return buf.toString();
        }
    }
    protected class TransformListBuilder implements TransformListHandler {
        protected ListHandler listHandler;
        public TransformListBuilder(ListHandler listHandler) {
            this.listHandler = listHandler;
        }
        public void startTransformList() throws ParseException {
            listHandler.startList();
        }
        public void matrix(float a, float b, float c, float d, float e, float f)
                throws ParseException {
            SVGTransformItem item  = new SVGTransformItem();
            item.matrix(a, b, c, d, e, f);
            listHandler.item(item);
        }
        public void rotate(float theta) throws ParseException {
            SVGTransformItem item = new SVGTransformItem();
            item.rotate(theta);
            listHandler.item(item);
        }
        public void rotate(float theta, float cx, float cy)
                throws ParseException {
            SVGTransformItem item = new SVGTransformItem();
            item.setRotate(theta, cx, cy);
            listHandler.item(item);
        }
        public void translate(float tx) throws ParseException {
            SVGTransformItem item = new SVGTransformItem();
            item.translate(tx);
            listHandler.item(item);
        }
        public void translate(float tx, float ty) throws ParseException {
            SVGTransformItem item = new SVGTransformItem();
            item.setTranslate(tx, ty);
            listHandler.item(item);
        }
        public void scale(float sx) throws ParseException {
            SVGTransformItem item  = new SVGTransformItem();
            item.scale(sx);
            listHandler.item(item);
        }
        public void scale(float sx, float sy) throws ParseException {
            SVGTransformItem item = new SVGTransformItem();
            item.setScale(sx, sy);
            listHandler.item(item);
        }
        public void skewX(float skx) throws ParseException {
            SVGTransformItem item = new SVGTransformItem();
            item.setSkewX(skx);
            listHandler.item(item);
        }
        public void skewY(float sky) throws ParseException {
            SVGTransformItem item  = new SVGTransformItem();
            item.setSkewY(sky);
            listHandler.item(item);
        }
        public void endTransformList() throws ParseException {
            listHandler.endList();
        }
    }
}
