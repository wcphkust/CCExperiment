package org.apache.xerces.impl.dv.xs;
import org.apache.xerces.impl.dv.InvalidDatatypeValueException;
import org.apache.xerces.impl.dv.ValidationContext;
class AnyAtomicDV extends TypeValidator {
    public short getAllowedFacets() {
        return 0;
    }
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        return content;
    }
} 
