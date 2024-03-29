package org.apache.xerces.xs;
public interface XSAttributeUse extends XSObject {
    public boolean getRequired();
    public XSAttributeDeclaration getAttrDeclaration();
    public short getConstraintType();
    public String getConstraintValue();
    public Object getActualVC()
                                       throws XSException;
    public short getActualVCType()
                                       throws XSException;
    public ShortList getItemValueTypes()
                                       throws XSException;
    public XSValue getValueConstraintValue();
    public XSObjectList getAnnotations();    
}
