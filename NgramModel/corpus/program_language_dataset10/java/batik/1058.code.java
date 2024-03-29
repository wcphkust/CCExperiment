package org.apache.batik.i18n;
import java.util.Locale;
import java.util.MissingResourceException;
public interface Localizable {
    void setLocale(Locale l);
    Locale getLocale();
    String formatMessage(String key, Object[] args)
        throws MissingResourceException;
}
