package com.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.proxy")
public class ProxyConfig {
    
    private String host;
    private String port;
    private String username;
    private String password;
    private String domain;

    public ProxyConfig() {
        // Configuration will be automatically injected by Spring from application.properties
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (host != null) {
            // Clean up hostname - remove http:// or https:// prefix if present
            if (host.startsWith("http://")) {
                host = host.substring(7);
            } else if (host.startsWith("https://")) {
                host = host.substring(8);
            }
        }
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
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

    public boolean isProxyEnabled() {
        return host != null && !host.trim().isEmpty() && 
               port != null && !port.trim().isEmpty();
    }

    public boolean hasCredentials() {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getPortAsInt() {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 8080; // default port
        }
    }
    
    public boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    public boolean isHostEmpty() {
        return isEmpty(host);
    }
    
    public boolean isPortEmpty() {
        return isEmpty(port);
    }
    
    public boolean isUsernameEmpty() {
        return isEmpty(username);
    }
    
    public boolean isPasswordEmpty() {
        return isEmpty(password);
    }
    
    public boolean isDomainEmpty() {
        return isEmpty(domain);
    }
}