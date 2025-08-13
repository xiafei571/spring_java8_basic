package com.example;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Test HTTPS connections using proper SSL configuration
 */
public class ProperSSLTest {

    public static void main(String[] args) {
        System.out.println("=== Testing HTTPS with Proper SSL Configuration ===\n");
        
        // Test with different Java SSL configurations
        testWithSystemProperties();
        testWithStandardConnection();
        testWithTLS12();
    }
    
    private static void testWithSystemProperties() {
        System.out.println("1. Testing with system proxy properties:");
        
        // Use system proxy settings
        System.setProperty("java.net.useSystemProxies", "true");
        
        try {
            testHttpsConnection("https://jsonplaceholder.typicode.com/todos/1");
            System.out.println("   SUCCESS with system properties");
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testWithStandardConnection() {
        System.out.println("2. Testing with standard HTTPS connection:");
        
        try {
            testHttpsConnection("https://httpbin.org/ip");
            System.out.println("   SUCCESS with standard connection");
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testWithTLS12() {
        System.out.println("3. Testing with explicit TLS 1.2:");
        
        // Force TLS 1.2
        System.setProperty("https.protocols", "TLSv1.2");
        
        try {
            testHttpsConnection("https://jsonplaceholder.typicode.com/todos/1");
            System.out.println("   SUCCESS with TLS 1.2");
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testHttpsConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        // Set timeouts
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        
        // Get response
        int responseCode = connection.getResponseCode();
        System.out.println("     Response Code: " + responseCode);
        
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                System.out.println("     Response (first line): " + line.substring(0, Math.min(line.length(), 50)) + "...");
            }
            reader.close();
        }
        
        connection.disconnect();
    }
}