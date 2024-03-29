package org.apache.batik.svggen;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
public class SVGPaintDescriptor implements SVGDescriptor, SVGSyntax{
    private Element def;
    private String paintValue;
    private String opacityValue;
    public SVGPaintDescriptor(String paintValue,
                              String opacityValue){
        this.paintValue = paintValue;
        this.opacityValue = opacityValue;
    }
    public SVGPaintDescriptor(String paintValue,
                              String opacityValue,
                              Element def){
        this(paintValue, opacityValue);
        this.def = def;
    }
    public String getPaintValue(){
        return paintValue;
    }
    public String getOpacityValue(){
        return opacityValue;
    }
    public Element getDef(){
        return def;
    }
    public Map getAttributeMap(Map attrMap){
        if(attrMap == null)
            attrMap = new HashMap();
        attrMap.put(SVG_FILL_ATTRIBUTE, paintValue);
        attrMap.put(SVG_STROKE_ATTRIBUTE, paintValue);
        attrMap.put(SVG_FILL_OPACITY_ATTRIBUTE, opacityValue);
        attrMap.put(SVG_STROKE_OPACITY_ATTRIBUTE, opacityValue);
        return attrMap;
    }
    public List getDefinitionSet(List defSet){
        if(defSet == null)
            defSet = new LinkedList();
        if(def != null)
            defSet.add(def);
        return defSet;
    }
}
