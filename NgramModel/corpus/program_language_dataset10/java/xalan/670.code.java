package org.apache.xml.serializer.dom3;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
final class DOMErrorHandlerImpl implements DOMErrorHandler {
    DOMErrorHandlerImpl() {
    }
    public boolean handleError(DOMError error) {
        boolean fail = true;
        String severity = null;
        if (error.getSeverity() == DOMError.SEVERITY_WARNING) {
            fail = false;
            severity = "[Warning]";
        } else if (error.getSeverity() == DOMError.SEVERITY_ERROR) {
            severity = "[Error]";
        } else if (error.getSeverity() == DOMError.SEVERITY_FATAL_ERROR) {
            severity = "[Fatal Error]";
        }
        System.err.println(severity + ": " + error.getMessage() + "\t");
        System.err.println("Type : " + error.getType() + "\t" + "Related Data: "
                + error.getRelatedData() + "\t" + "Related Exception: "
                + error.getRelatedException() );
        return fail;
    }
}
