package com.example;

import java.io.IOException;
import java.net.*;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

/**
 * Network diagnostic tool for corporate environments
 */
public class NetworkDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== Network Diagnostic Tool ===\n");
        
        // 1. Check Java version and system properties
        checkJavaEnvironment();
        
        // 2. Check proxy settings
        checkProxySettings();
        
        // 3. Test basic connectivity
        testConnectivity();
        
        // 4. Test with different protocols
        testProtocols();
    }
    
    private static void checkJavaEnvironment() {
        System.out.println("1. Java Environment:");
        System.out.println("   Java Version: " + System.getProperty("java.version"));
        System.out.println("   Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("   OS: " + System.getProperty("os.name"));
        
        // SSL/TLS properties
        System.out.println("   Enabled Protocols: " + System.getProperty("https.protocols"));
        System.out.println("   Cipher Suites: " + System.getProperty("https.cipherSuites"));
        System.out.println("   Use System Proxies: " + System.getProperty("java.net.useSystemProxies"));
        
        // Proxy properties
        System.out.println("   HTTP Proxy Host: " + System.getProperty("http.proxyHost"));
        System.out.println("   HTTP Proxy Port: " + System.getProperty("http.proxyPort"));
        System.out.println("   HTTPS Proxy Host: " + System.getProperty("https.proxyHost"));
        System.out.println("   HTTPS Proxy Port: " + System.getProperty("https.proxyPort"));
        System.out.println();
    }
    
    private static void checkProxySettings() {
        System.out.println("2. Proxy Detection:");
        
        try {
            System.setProperty("java.net.useSystemProxies", "true");
            
            List<Proxy> proxies = ProxySelector.getDefault().select(
                new URI("https://jsonplaceholder.typicode.com"));
            
            if (proxies.isEmpty()) {
                System.out.println("   No proxies detected");
            } else {
                for (Proxy proxy : proxies) {
                    System.out.println("   Detected proxy: " + proxy);
                }
            }
        } catch (Exception e) {
            System.out.println("   Error detecting proxies: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testConnectivity() {
        System.out.println("3. Basic Connectivity Tests:");
        
        // Test DNS resolution
        testDNS("jsonplaceholder.typicode.com");
        testDNS("httpbin.org");
        
        // Test TCP connection
        testTCPConnection("jsonplaceholder.typicode.com", 443);
        testTCPConnection("httpbin.org", 443);
        
        System.out.println();
    }
    
    private static void testDNS(String hostname) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            System.out.println("   DNS " + hostname + ": " + addresses[0].getHostAddress());
        } catch (UnknownHostException e) {
            System.out.println("   DNS " + hostname + ": FAILED - " + e.getMessage());
        }
    }
    
    private static void testTCPConnection(String hostname, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hostname, port), 10000);
            System.out.println("   TCP " + hostname + ":" + port + ": CONNECTED");
        } catch (IOException e) {
            System.out.println("   TCP " + hostname + ":" + port + ": FAILED - " + e.getMessage());
        }
    }
    
    private static void testProtocols() {
        System.out.println("4. Protocol Tests:");
        
        // Test HTTP (non-SSL)
        testHTTPConnection("http://httpbin.org/ip");
        
        // Test with basic URLConnection
        testURLConnection("https://jsonplaceholder.typicode.com/todos/1");
        
        System.out.println();
    }
    
    private static void testHTTPConnection(String url) {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            System.out.println("   HTTP " + url + ": SUCCESS");
        } catch (Exception e) {
            System.out.println("   HTTP " + url + ": FAILED - " + e.getMessage());
        }
    }
    
    private static void testURLConnection(String url) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("   URLConnection " + url + ": " + responseCode);
        } catch (Exception e) {
            System.out.println("   URLConnection " + url + ": FAILED - " + e.getMessage());
        }
    }
}