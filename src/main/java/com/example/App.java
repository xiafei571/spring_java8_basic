package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class App implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    @Value("${app.get.url}")
    private String getUrl;
    
    @Value("${app.post.url}")
    private String postUrl;

    @Autowired
    private HttpClientFactory httpClientFactory;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
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
        
        // Enable Windows integrated authentication (like PSCredential)
        System.setProperty("java.security.auth.login.config", "");
        System.setProperty("java.security.krb5.conf", "");
        System.setProperty("sun.security.krb5.debug", "false");
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting CLI application");
        
        // Parse command line arguments for proxy settings
        parseProxyArguments(args);
        
        // Configure DNS settings to resolve proxy hostname
        configureDNS();
        
        // Log all command line arguments
        logger.info("Command line arguments: {}", Arrays.toString(args));
        
        try (CloseableHttpClient httpClient = httpClientFactory.createHttpClient()) {
            // Perform GET request
            performGetRequest(httpClient);
            
            // Perform POST request
            performPostRequest(httpClient);
            
            logger.info("All HTTP requests completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during HTTP requests", e);
        } finally {
            logger.info("Application finished, exiting...");
            System.exit(0);
        }
    }

    private void performGetRequest(CloseableHttpClient httpClient) throws IOException {
        logger.info("Performing GET request to: {}", getUrl);
        
        HttpGet request = new HttpGet(getUrl);
        
        // Use PowerShell-compatible headers only
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT; Windows NT 10.0; en-US) WindowsPowerShell/5.1.19041.4648");
        
        try {
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            logger.info("GET Response - Status: {}, Body: {}", statusCode, responseBody);
        } finally {
            request.releaseConnection();
        }
    }

    private void performPostRequest(CloseableHttpClient httpClient) throws IOException {
        logger.info("Performing POST request to: {}", postUrl);
        
        // Create JSON payload
        Map<String, String> payload = new HashMap<>();
        payload.put("ping", "hello-from-cli");
        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        HttpPost request = new HttpPost(postUrl);
        StringEntity entity = new StringEntity(jsonPayload);
        entity.setContentType("application/json");
        request.setEntity(entity);
        
        // Use PowerShell-compatible headers only
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT; Windows NT 10.0; en-US) WindowsPowerShell/5.1.19041.4648");
        request.setHeader("Content-Type", "application/json");
        
        try {
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            logger.info("POST Response - Status: {}, Body: {}", statusCode, responseBody);
        } finally {
            request.releaseConnection();
        }
    }
    
    private void parseProxyArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-proxyHost".equals(args[i]) && i + 1 < args.length) {
                String host = args[i + 1];
                // Clean up hostname - remove http:// or https:// prefix if present
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
                proxyConfig.setUsername(args[i + 1]);
            } else if ("-proxyPassword".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setPassword(args[i + 1]);
            } else if ("-proxyDomain".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setDomain(args[i + 1]);
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
                logger.info("Trying alternative DNS resolution methods...");
                
                // Try alternative resolution methods
                tryAlternativeDNSResolution(proxyConfig.getHost());
            }
        }
    }
    
    private void tryAlternativeDNSResolution(String hostname) {
        try {
            // Try using system DNS directly
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