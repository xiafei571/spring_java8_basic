package com.example;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SimpleProxyTest implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleProxyTest.class);
    
    @Autowired
    private ProxyConfig proxyConfig;
    
    public static void main(String[] args) {
        SpringApplication.run(SimpleProxyTest.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Parse command line arguments
        parseProxyArguments(args);
        
        if (!proxyConfig.isProxyEnabled()) {
            logger.error("Proxy not configured. Please provide -proxyHost and -proxyPort");
            return;
        }
        
        logger.info("Testing simple proxy connection...");
        logger.info("Proxy: {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
        logger.info("User: {}", proxyConfig.getUsername());
        
        try {
            testDirectConnection();
        } catch (Exception e) {
            logger.error("Direct connection test failed: {}", e.getMessage(), e);
        }
        
        System.exit(0);
    }
    
    private void testDirectConnection() throws Exception {
        // Explicitly disable SOCKS proxy to force HTTP proxy
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("java.net.useSystemProxies", "false");
        
        // Create simple HTTP client with proxy
        HttpHost proxy = new HttpHost(proxyConfig.getHost(), proxyConfig.getPortAsInt(), "http");
        
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (proxyConfig.hasCredentials()) {
            credentialsProvider.setCredentials(
                new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt()),
                new UsernamePasswordCredentials(proxyConfig.getUsername(), proxyConfig.getPassword())
            );
        }
        
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setProxy(proxy)
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
        
        HttpGet request = new HttpGet("https://jsonplaceholder.typicode.com/todos/1");
        
        try {
            logger.info("Making request through proxy...");
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            
            logger.info("SUCCESS! Status: {}", statusCode);
            logger.info("Response: {}", body);
            
        } finally {
            httpClient.close();
        }
    }
    
    private void parseProxyArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-proxyHost".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setHost(args[i + 1]);
            } else if ("-proxyPort".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setPort(args[i + 1]);
            } else if ("-proxyUser".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setUsername(args[i + 1]);
            } else if ("-proxyPassword".equals(args[i]) && i + 1 < args.length) {
                proxyConfig.setPassword(args[i + 1]);
            }
        }
    }
}