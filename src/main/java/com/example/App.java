package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class App implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    @Value("${app.get.url}")
    private String getUrl;
    
    @Value("${app.post.url}")
    private String postUrl;

    private final OkHttpClient httpClient = createHttpClient();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static OkHttpClient createHttpClient() {
        // Use the default OkHttpClient which properly handles modern TLS and certificate validation
        // OkHttp will automatically use the system's default SSL context and trust store
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting CLI application");
        
        // Log all command line arguments
        logger.info("Command line arguments: {}", Arrays.toString(args));
        
        try {
            // Perform GET request
            performGetRequest();
            
            // Perform POST request
            performPostRequest();
            
            logger.info("All HTTP requests completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during HTTP requests", e);
        } finally {
            logger.info("Application finished, exiting...");
            System.exit(0);
        }
    }

    private void performGetRequest() throws IOException {
        logger.info("Performing GET request to: {}", getUrl);
        
        Request request = new Request.Builder()
                .url(getUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int statusCode = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            logger.info("GET Response - Status: {}, Body: {}", statusCode, responseBody);
        }
    }

    private void performPostRequest() throws IOException {
        logger.info("Performing POST request to: {}", postUrl);
        
        // Create JSON payload
        Map<String, String> payload = new HashMap<>();
        payload.put("ping", "hello-from-cli");
        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        RequestBody requestBody = RequestBody.create(
                jsonPayload, 
                MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(postUrl)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int statusCode = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            logger.info("POST Response - Status: {}, Body: {}", statusCode, responseBody);
        }
    }
}