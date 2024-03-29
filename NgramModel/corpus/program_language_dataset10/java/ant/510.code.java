package org.apache.tools.ant.taskdefs.optional.net;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.ProxySetup;
public class SetProxy extends Task {
    private static final int HTTP_PORT = 80;
    private static final int SOCKS_PORT = 1080;
    protected String proxyHost = null;
    protected int proxyPort = HTTP_PORT;
    private String socksProxyHost = null;
    private int socksProxyPort = SOCKS_PORT;
    private String nonProxyHosts = null;
    private String proxyUser = null;
    private String proxyPassword = null;
    public void setProxyHost(String hostname) {
        proxyHost = hostname;
    }
    public void setProxyPort(int port) {
        proxyPort = port;
    }
    public void setSocksProxyHost(String host) {
        this.socksProxyHost = host;
    }
    public void setSocksProxyPort(int port) {
        this.socksProxyPort = port;
    }
    public void setNonProxyHosts(String nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts;
    }
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
    public void applyWebProxySettings() {
        boolean settingsChanged = false;
        boolean enablingProxy = false;
        Properties sysprops = System.getProperties();
        if (proxyHost != null) {
            settingsChanged = true;
            if (proxyHost.length() != 0) {
                traceSettingInfo();
                enablingProxy = true;
                sysprops.put(ProxySetup.HTTP_PROXY_HOST, proxyHost);
                String portString = Integer.toString(proxyPort);
                sysprops.put(ProxySetup.HTTP_PROXY_PORT, portString);
                sysprops.put(ProxySetup.HTTPS_PROXY_HOST, proxyHost);
                sysprops.put(ProxySetup.HTTPS_PROXY_PORT, portString);
                sysprops.put(ProxySetup.FTP_PROXY_HOST, proxyHost);
                sysprops.put(ProxySetup.FTP_PROXY_PORT, portString);
                if (nonProxyHosts != null) {
                    sysprops.put(ProxySetup.HTTP_NON_PROXY_HOSTS, nonProxyHosts);
                    sysprops.put(ProxySetup.HTTPS_NON_PROXY_HOSTS, nonProxyHosts);
                    sysprops.put(ProxySetup.FTP_NON_PROXY_HOSTS, nonProxyHosts);
                }
                if (proxyUser != null) {
                    sysprops.put(ProxySetup.HTTP_PROXY_USERNAME, proxyUser);
                    sysprops.put(ProxySetup.HTTP_PROXY_PASSWORD, proxyPassword);
                }
            } else {
                log("resetting http proxy", Project.MSG_VERBOSE);
                sysprops.remove(ProxySetup.HTTP_PROXY_HOST);
                sysprops.remove(ProxySetup.HTTP_PROXY_PORT);
                sysprops.remove(ProxySetup.HTTP_PROXY_USERNAME);
                sysprops.remove(ProxySetup.HTTP_PROXY_PASSWORD);
                sysprops.remove(ProxySetup.HTTPS_PROXY_HOST);
                sysprops.remove(ProxySetup.HTTPS_PROXY_PORT);
                sysprops.remove(ProxySetup.FTP_PROXY_HOST);
                sysprops.remove(ProxySetup.FTP_PROXY_PORT);
            }
        }
        if (socksProxyHost != null) {
            settingsChanged = true;
            if (socksProxyHost.length() != 0) {
                enablingProxy = true;
                sysprops.put(ProxySetup.SOCKS_PROXY_HOST, socksProxyHost);
                sysprops.put(ProxySetup.SOCKS_PROXY_PORT, Integer.toString(socksProxyPort));
                if (proxyUser != null) {
                    sysprops.put(ProxySetup.SOCKS_PROXY_USERNAME, proxyUser);
                    sysprops.put(ProxySetup.SOCKS_PROXY_PASSWORD, proxyPassword);
                }
            } else {
                log("resetting socks proxy", Project.MSG_VERBOSE);
                sysprops.remove(ProxySetup.SOCKS_PROXY_HOST);
                sysprops.remove(ProxySetup.SOCKS_PROXY_PORT);
                sysprops.remove(ProxySetup.SOCKS_PROXY_USERNAME);
                sysprops.remove(ProxySetup.SOCKS_PROXY_PASSWORD);
            }
        }
        if (proxyUser != null) {
            if (enablingProxy) {
                Authenticator.setDefault(new ProxyAuth(proxyUser,
                                                       proxyPassword));
            } else if (settingsChanged) {
                Authenticator.setDefault(new ProxyAuth("", ""));
            }
        }
    }
    private void traceSettingInfo() {
        log("Setting proxy to "
                + (proxyHost != null ? proxyHost : "''")
                + ":" + proxyPort,
                Project.MSG_VERBOSE);
    }
    public void execute() throws BuildException {
        applyWebProxySettings();
    }
    private static final class ProxyAuth extends Authenticator {
        private PasswordAuthentication auth;
        private ProxyAuth(String user, String pass) {
            auth = new PasswordAuthentication(user, pass.toCharArray());
        }
        protected PasswordAuthentication getPasswordAuthentication() {
            return auth;
        }
    }
}
