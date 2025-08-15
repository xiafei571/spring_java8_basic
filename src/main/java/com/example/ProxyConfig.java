package com.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Component
@ConfigurationProperties(prefix = "app.proxy")
public class ProxyConfig {
    
    private String host;
    private String port;
    private String username;
    private String password;
    private String domain;

    public ProxyConfig() {
        // Skip file loading - use command line arguments only
        // loadFromFile();
    }

    private void loadFromFile() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("proxy.properties")) {
            props.load(fis);
            // Only override if not already set from command line
            if (this.host == null) this.host = props.getProperty("proxy.host");
            if (this.port == null) this.port = props.getProperty("proxy.port");
            if (this.username == null) this.username = props.getProperty("proxy.username");
            if (this.password == null) this.password = props.getProperty("proxy.password");
            if (this.domain == null) this.domain = props.getProperty("proxy.domain");
        } catch (IOException e) {
            // File doesn't exist or can't be read, keep existing values
        }
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
        // Debug logging - length only for security
        System.out.println("DEBUG: Password set (length: " + (password != null ? password.length() : 0) + ")");
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
}