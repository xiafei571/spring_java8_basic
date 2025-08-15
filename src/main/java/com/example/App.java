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

import java.io.IOException;
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
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting CLI application");
        
        // Parse command line arguments for proxy settings
        parseProxyArguments(args);
        
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