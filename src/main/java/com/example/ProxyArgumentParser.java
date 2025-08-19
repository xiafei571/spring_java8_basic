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
        for (int i = 0; i < args.length; i++) {
            if ("-proxyHost".equals(args[i]) && i + 1 < args.length) {
                String host = args[i + 1];
                if (host.startsWith("http://")) {
                    host = host.substring(7);
                } else if (host.startsWith("https://")) {
                    host = host.substring(8);
                }
                proxyConfig.setHost(host);
                logger.info("Proxy host set to: {}", host);
            } else if ("-proxyPort".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setPort(args[i + 1]);
            } else if ("-proxyUser".equals(args[i]) && i + 1 < args.length) {
                String user = args[i + 1];
                proxyConfig.setUsername(user);
                logger.info("Proxy user set to: {}", user);
            } else if ("-proxyPassword".equals(args[i]) && i + 1 < args.length) {
                String pass = args[i + 1];
                proxyConfig.setPassword(pass);
                logger.info("Proxy password set (length: {})", pass != null ? pass.length() : 0);
            } else if ("-proxyDomain".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setDomain(args[i + 1]);
            }
        }
    }
}