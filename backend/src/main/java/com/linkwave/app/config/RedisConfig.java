package com.linkwave.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration properties for Redis connection and session management.
 * Values are loaded from application.yml or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "linkwave.redis")
@Profile("!test")
public class RedisConfig {

    /**
     * Redis server host.
     */
    private String host = "localhost";

    /**
     * Redis server port.
     */
    private int port = 6379;

    /**
     * Redis authentication password (optional).
     */
    private String password;

    /**
     * Session timeout in minutes.
     */
    private int sessionTimeoutMinutes = 30;

    /**
     * Redis namespace prefix for session keys.
     */
    private String namespace = "linkwave:session:";

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
