package com.example;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Windows-compatible version that uses system settings
 */
public class WindowsCompatibleApp {

    public static void main(String[] args) {
        // Set system properties for Windows compatibility
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        
        System.out.println("Starting Windows-compatible HTTP client test...");
        System.out.println("Command line arguments: " + java.util.Arrays.toString(args));
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            // Test GET request
            System.out.println("\nPerforming GET request...");
            testGet(client);
            
            // Test POST request
            System.out.println("\nPerforming POST request...");
            testPost(client, mapper);
            
            System.out.println("\nAll requests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testGet(OkHttpClient client) throws IOException {
        Request request = new Request.Builder()
                .url("https://jsonplaceholder.typicode.com/todos/1")
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("GET Status: " + response.code());
            String body = response.body() != null ? response.body().string() : "";
            System.out.println("GET Body: " + body);
        }
    }
    
    private static void testPost(OkHttpClient client, ObjectMapper mapper) throws IOException {
        Map<String, String> payload = new HashMap<>();
        payload.put("ping", "hello-from-cli");
        String jsonPayload = mapper.writeValueAsString(payload);
        
        RequestBody requestBody = RequestBody.create(
                jsonPayload, 
                MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(requestBody)
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("POST Status: " + response.code());
            String body = response.body() != null ? response.body().string() : "";
            System.out.println("POST Body: " + body);
        }
    }
}