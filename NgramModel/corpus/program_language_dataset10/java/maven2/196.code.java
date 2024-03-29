package org.apache.maven.usability.diagnostics;
public interface ErrorDiagnoser
{
    String ROLE = ErrorDiagnoser.class.getName();
    boolean canDiagnose( Throwable error );
    String diagnose( Throwable error );
}
