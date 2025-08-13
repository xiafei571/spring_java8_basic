package com.example;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.Security;

/**
 * Test for enterprise SSL environments with various compatibility settings
 */
public class EnterpriseSSLTest {

    public static void main(String[] args) {
        System.out.println("=== Enterprise SSL Environment Testing ===\n");
        
        printJavaInfo();
        testWithLegacySettings();
        testWithModernSettings();
        testWithEnterpriseSettings();
    }
    
    private static void printJavaInfo() {
        System.out.println("Java Environment:");
        System.out.println("  Java Version: " + System.getProperty("java.version"));
        System.out.println("  Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("  Available Protocols: ");
        
        try {
            SSLContext context = SSLContext.getDefault();
            String[] protocols = context.getSupportedSSLParameters().getProtocols();
            for (String protocol : protocols) {
                System.out.println("    " + protocol);
            }
        } catch (Exception e) {
            System.out.println("    Error getting protocols: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testWithLegacySettings() {
        System.out.println("1. Testing with Legacy SSL Settings:");
        
        // Set legacy SSL properties
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        
        try {
            testConnection("https://jsonplaceholder.typicode.com/todos/1");
            System.out.println("   SUCCESS with legacy settings");
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testWithModernSettings() {
        System.out.println("2. Testing with Modern SSL Settings:");
        
        // Clear previous settings and set modern ones
        System.clearProperty("https.protocols");
        System.clearProperty("jdk.tls.client.protocols");
        
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3");
        System.setProperty("jdk.tls.useExtendedMasterSecret", "false");
        
        try {
            testConnection("https://httpbin.org/ip");
            System.out.println("   SUCCESS with modern settings");
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testWithEnterpriseSettings() {
        System.out.println("3. Testing with Enterprise Network Settings:");
        
        // Enterprise-friendly settings
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("https.protocols", "TLSv1.2");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("jdk.tls.useExtendedMasterSecret", "false");
        System.setProperty("jdk.tls.allowUnsafeServerCertChange", "true");
        System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        
        try {
            testConnection("https://jsonplaceholder.typicode.com/todos/1");
            System.out.println("   SUCCESS with enterprise settings");
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testConnection(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        // Set generous timeouts for enterprise networks
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        
        // Add enterprise-friendly headers
        connection.setRequestProperty("User-Agent", "Java-Enterprise-Client/1.0");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "close");
        
        int responseCode = connection.getResponseCode();
        System.out.println("     Response Code: " + responseCode);
        
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = reader.readLine();
            if (line != null && line.length() > 0) {
                System.out.println("     Response: " + line.substring(0, Math.min(line.length(), 50)) + "...");
            }
            reader.close();
        }
        
        connection.disconnect();
    }
}