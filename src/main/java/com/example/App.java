package com.example;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

@SpringBootApplication
public class App implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    @Autowired
    private ProxyArgumentParser proxyArgumentParser;
    
    @Autowired
    private ProxyConfig proxyConfig;

    public static void main(String[] args) {
        // CRITICAL: Disable SOCKS before ANYTHING else
        System.setProperty("java.net.useSystemProxies", "false");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("socksProxyVersion");
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        
        // Force Basic authentication only - disable all integrated auth
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("java.security.krb5.conf", "");
        System.setProperty("java.security.auth.login.config", "");
        System.setProperty("sun.security.spnego.debug", "false");
        
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting CLI application");
        
        // Parse command line arguments for proxy settings
        proxyArgumentParser.parseProxyArguments(args);
        
        // Configure DNS settings to resolve proxy hostname
        configureDNS();
        
        // Log all command line arguments
        logger.info("Command line arguments: {}", Arrays.toString(args));
        
        try {
            logger.info("Using Apache HttpClient with NTLM proxy");
            testHttpClientNTLM();
            
            logger.info("HTTP request completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during HTTP request", e);
        } finally {
            logger.info("Application finished, exiting...");
            System.exit(0);
        }
    }
    
    private void testHttpClientNTLM() {
        logger.info("Testing Apache HttpClient with NTLM proxy to https://www.google.com");
        
        if (!proxyConfig.isProxyEnabled()) {
            logger.warn("No proxy configuration found. Please provide proxy settings via command line arguments.");
            return;
        }
        
        String result = testInternetProxyAccess(
            proxyConfig.getHost(),
            proxyConfig.getPortAsInt(),
            proxyConfig.getUsername(),
            proxyConfig.getPassword(),
            proxyConfig.getDomain()
        );
        
        logger.info("Apache HttpClient NTLM result:\n{}", result);
    }
    
    public static String testInternetProxyAccess(String proxyHost, int proxyPort, String username, String password, String domain) {
        logger.info("Testing internet proxy access - equivalent to: curl --proxy-ntlm --proxy-user '{}:***' --proxy 'http://{}:{}' 'https://www.google.com'", 
                   username, proxyHost, proxyPort);
        
        return callWithHttpClientNTLM(proxyHost, proxyPort, username, password, domain);
    }
    
    public static String callWithHttpClientNTLM(String proxyHost, int proxyPort, String username, String password, String domain) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        
        try {
            // Extract domain from username if in DOMAIN\\username format
            String actualUsername = username;
            String actualDomain = domain;
            
            if (username != null && username.contains("\\")) {
                String[] parts = username.split("\\\\", 2);
                if (parts.length == 2) {
                    actualDomain = parts[0];
                    actualUsername = parts[1];
                }
            }
            
            // Get workstation name
            String workstation = "";
            try {
                workstation = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                workstation = ""; // Use empty string if can't get hostname
            }
            
            // Set up proxy host
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            
            // Set up NTLM credentials
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            NTCredentials ntCredentials = new NTCredentials(
                actualUsername, 
                password, 
                workstation, 
                actualDomain != null ? actualDomain : ""
            );
            credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), ntCredentials);
            
            // Configure request with proxy
            RequestConfig config = RequestConfig.custom()
                .setProxy(proxy)
                .setConnectTimeout(30000)
                .setSocketTimeout(30000)
                .build();
            
            // Create HttpClient with NTLM support
            httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(config)
                .build();
            
            // Create GET request
            HttpGet httpGet = new HttpGet("https://www.google.com");
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            // Execute request
            response = httpClient.execute(httpGet);
            
            // Get response
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            return "Response Code: " + statusCode + "\n" + responseBody;
            
        } catch (Exception e) {
            logger.error("Error in callWithHttpClientNTLM", e);
            return "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage() + 
                   "\nFull stack trace logged to console";
        } finally {
            // Clean up resources
            try {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    private void configureDNS() {
        // CRITICAL: Completely disable ALL system proxy detection
        System.setProperty("java.net.useSystemProxies", "false");
        
        // CRITICAL: Explicitly clear ALL SOCKS proxy settings
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("socksProxyVersion");
        System.clearProperty("socksNonProxyHosts");
        
        // Additional SOCKS cleanup
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("socksProxyVersion", "");
        System.setProperty("socksNonProxyHosts", "");
        
        // Network settings
        System.setProperty("networkaddress.cache.ttl", "0");
        System.setProperty("networkaddress.cache.negative.ttl", "0");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        
        // DNS settings
        System.setProperty("sun.net.spi.nameservice.nameservers", "");
        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
        
        logger.info("Java network configuration set (system proxies disabled)");
        
        // If proxy is configured, try to resolve the hostname first
        if (proxyConfig.isProxyEnabled()) {
            try {
                InetAddress addr = InetAddress.getByName(proxyConfig.getHost());
                logger.info("Successfully resolved proxy hostname: {} -> {}", 
                           proxyConfig.getHost(), addr.getHostAddress());
            } catch (UnknownHostException e) {
                logger.warn("Failed to resolve proxy hostname: {}. Error: {}", 
                           proxyConfig.getHost(), e.getMessage());
                logger.info("Will proceed anyway - HttpClient may still be able to resolve it");
            }
        }
    }
}