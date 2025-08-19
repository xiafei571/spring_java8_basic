package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class NetworkConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkConfigurationService.class);
    
    @Autowired
    private ProxyConfig proxyConfig;
    
    public void configureNetworkSettings() {
        configureSystemProperties();
        configureDNS();
    }
    
    private void configureSystemProperties() {
        System.setProperty("java.net.useSystemProxies", "false");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("socksProxyVersion");
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("java.security.krb5.conf", "");
        System.setProperty("java.security.auth.login.config", "");
        System.setProperty("sun.security.spnego.debug", "false");
    }
    
    private void configureDNS() {
        System.setProperty("java.net.useSystemProxies", "false");
        
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("socksProxyVersion");
        System.clearProperty("socksNonProxyHosts");
        
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("socksProxyVersion", "");
        System.setProperty("socksNonProxyHosts", "");
        
        System.setProperty("networkaddress.cache.ttl", "0");
        System.setProperty("networkaddress.cache.negative.ttl", "0");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        
        System.setProperty("sun.net.spi.nameservice.nameservers", "");
        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
        
        logger.info("Java network configuration set (system proxies disabled)");
        
        if (proxyConfig.isProxyEnabled()) {
            try {
                InetAddress addr = InetAddress.getByName(proxyConfig.getHost());
                logger.info("Successfully resolved proxy hostname: {} -> {}", 
                           proxyConfig.getHost(), addr.getHostAddress());
            } catch (UnknownHostException e) {
                logger.warn("Failed to resolve proxy hostname: {}. Error: {}", 
                           proxyConfig.getHost(), e.getMessage());
                logger.info("Trying alternative DNS resolution methods...");
                
                tryAlternativeDNSResolution(proxyConfig.getHost());
            }
        }
    }
    
    private void tryAlternativeDNSResolution(String hostname) {
        try {
            Process process = Runtime.getRuntime().exec("nslookup " + hostname);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Address:") && !line.contains("#53")) {
                    String ipAddress = line.split("Address:")[1].trim();
                    logger.info("System nslookup found IP: {} -> {}", hostname, ipAddress);
                    logger.info("Consider using this IP address instead of hostname");
                    break;
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            logger.warn("Alternative DNS resolution failed: {}", e.getMessage());
        }
    }
}