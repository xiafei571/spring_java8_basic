package com.example;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;

/**
 * Version that completely bypasses SSL verification
 * WARNING: Only for testing in corporate environments
 */
public class BypassSSLApp {

    public static void main(String[] args) {
        System.out.println("Starting app with SSL bypass (TESTING ONLY)...");
        System.out.println("Command line arguments: " + java.util.Arrays.toString(args));
        
        try {
            // Globally disable SSL verification
            disableSSLVerification();
            
            OkHttpClient client = createUnsafeOkHttpClient();
            ObjectMapper mapper = new ObjectMapper();
            
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
    
    private static void disableSSLVerification() throws NoSuchAlgorithmException, KeyManagementException {
        // Create trust manager that accepts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                public void checkServerTrusted(X509Certificate[] certs, String authType) { }
            }
        };
        
        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }
    
    private static OkHttpClient createUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        // Create trust manager that accepts all certificates
        final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        };
        
        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        
        return new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
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