package org.apache.batik.bridge;
public class NoLoadExternalResourceSecurity implements ExternalResourceSecurity {
    public static final String ERROR_NO_EXTERNAL_RESOURCE_ALLOWED
        = "NoLoadExternalResourceSecurity.error.no.external.resource.allowed";
    protected SecurityException se;
    public void checkLoadExternalResource(){
        if (se != null) {
            se.fillInStackTrace();
            throw se;
        }
    }
    public NoLoadExternalResourceSecurity(){
        se = new SecurityException
            (Messages.formatMessage(ERROR_NO_EXTERNAL_RESOURCE_ALLOWED,
                                    null));
    }
}
