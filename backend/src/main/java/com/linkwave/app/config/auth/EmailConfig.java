package com.linkwave.app.config.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email/SMTP settings.
 * Values are loaded from application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "linkwave.mail")
public class EmailConfig {

    /**
     * SMTP server host.
     */
    private String host;

    /**
     * SMTP server port.
     */
    private int port;

    /**
     * SMTP authentication username.
     */
    private String username;

    /**
     * SMTP authentication password.
     */
    private String password;

    /**
     * From email address for outgoing emails.
     */
    private String from = "no-reply@linkwave.app";

    /**
     * Whether to enable TLS encryption.
     */
    private boolean tlsEnabled = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }
}
