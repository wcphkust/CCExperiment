package org.apache.maven.settings.crypto;
public interface SettingsDecrypter
{
    SettingsDecryptionResult decrypt( SettingsDecryptionRequest request );
}
