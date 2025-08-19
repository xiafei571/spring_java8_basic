package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProxyArgumentParser {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyArgumentParser.class);
    
    @Autowired
    private ProxyConfig proxyConfig;
    
    public void parseProxyArguments(String[] args) {
        logger.info("Checking proxy configuration from application.properties and command line arguments");
        logCurrentConfig();
        
        for (int i = 0; i < args.length; i++) {
            if ("-proxyHost".equals(args[i]) && i + 1 < args.length) {
                if (proxyConfig.isHostEmpty()) {
                    String host = args[i + 1];
                    if (host.startsWith("http://")) {
                        host = host.substring(7);
                    } else if (host.startsWith("https://")) {
                        host = host.substring(8);
                    }
                    proxyConfig.setHost(host);
                    logger.info("Proxy host set from command line to: {}", host);
                } else {
                    logger.info("Proxy host already configured in application.properties: {}", proxyConfig.getHost());
                }
            } else if ("-proxyPort".equals(args[i]) && i + 1 < args.length) {
                if (proxyConfig.isPortEmpty()) {
                    proxyConfig.setPort(args[i + 1]);
                    logger.info("Proxy port set from command line to: {}", args[i + 1]);
                } else {
                    logger.info("Proxy port already configured in application.properties: {}", proxyConfig.getPort());
                }
            } else if ("-proxyUser".equals(args[i]) && i + 1 < args.length) {
                if (proxyConfig.isUsernameEmpty()) {
                    String user = args[i + 1];
                    proxyConfig.setUsername(user);
                    logger.info("Proxy user set from command line to: {}", user);
                } else {
                    logger.info("Proxy username already configured in application.properties: {}", proxyConfig.getUsername());
                }
            } else if ("-proxyPassword".equals(args[i]) && i + 1 < args.length) {
                if (proxyConfig.isPasswordEmpty()) {
                    String pass = args[i + 1];
                    proxyConfig.setPassword(pass);
                    logger.info("Proxy password set from command line (length: {})", pass != null ? pass.length() : 0);
                } else {
                    logger.info("Proxy password already configured in application.properties (length: {})", 
                               proxyConfig.getPassword() != null ? proxyConfig.getPassword().length() : 0);
                }
            } else if ("-proxyDomain".equals(args[i]) && i + 1 < args.length) {
                if (proxyConfig.isDomainEmpty()) {
                    proxyConfig.setDomain(args[i + 1]);
                    logger.info("Proxy domain set from command line to: {}", args[i + 1]);
                } else {
                    logger.info("Proxy domain already configured in application.properties: {}", proxyConfig.getDomain());
                }
            }
        }
        
        logger.info("Final proxy configuration:");
        logCurrentConfig();
    }
    
    private void logCurrentConfig() {
        logger.info("  Host: {}", proxyConfig.isEmpty(proxyConfig.getHost()) ? "<empty>" : proxyConfig.getHost());
        logger.info("  Port: {}", proxyConfig.isEmpty(proxyConfig.getPort()) ? "<empty>" : proxyConfig.getPort());
        logger.info("  Username: {}", proxyConfig.isEmpty(proxyConfig.getUsername()) ? "<empty>" : proxyConfig.getUsername());
        logger.info("  Password: {}", proxyConfig.isEmpty(proxyConfig.getPassword()) ? "<empty>" : "<configured>");
        logger.info("  Domain: {}", proxyConfig.isEmpty(proxyConfig.getDomain()) ? "<empty>" : proxyConfig.getDomain());
    }
}