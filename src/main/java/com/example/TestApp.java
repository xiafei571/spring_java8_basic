package com.example;

import okhttp3.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Test version that bypasses SSL verification for corporate networks
 * WARNING: Only use this for testing in corporate environments
 */
public class TestApp {

    public static void main(String[] args) {
        System.out.println("Testing HTTP requests with relaxed SSL...");
        
        try {
            OkHttpClient client = createUnsafeClient();
            
            // Test GET request
            testGet(client);
            
            // Test POST request  
            testPost(client);
            
            System.out.println("All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static OkHttpClient createUnsafeClient() throws NoSuchAlgorithmException, KeyManagementException {
        // Create trust manager that accepts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                
                @Override  
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        };
        
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        return new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }
    
    private static void testGet(OkHttpClient client) throws IOException {
        Request request = new Request.Builder()
                .url("https://jsonplaceholder.typicode.com/todos/1")
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("GET Status: " + response.code());
            System.out.println("GET Body: " + response.body().string());
        }
    }
    
    private static void testPost(OkHttpClient client) throws IOException {
        RequestBody body = RequestBody.create(
                "{\"ping\":\"hello-from-cli\"}", 
                MediaType.parse("application/json"));
                
        Request request = new Request.Builder()
                .url("https://httpbin.org/post")
                .post(body)
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            System.out.println("POST Status: " + response.code());
            System.out.println("POST Body: " + response.body().string());
        }
    }
}