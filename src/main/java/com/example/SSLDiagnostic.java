package com.example;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.cert.X509Certificate;

/**
 * SSL/TLS diagnostic tool to identify handshake issues
 */
public class SSLDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== SSL/TLS Diagnostic Tool ===\n");
        
        // Enable minimal SSL debugging (remove if too verbose)
        // System.setProperty("javax.net.debug", "ssl,handshake");
        
        // Test different approaches
        testSSLContext();
        testDirectSSLSocket();
        testWithDifferentProtocols();
    }
    
    private static void testSSLContext() {
        System.out.println("1. Testing SSL Context:");
        
        try {
            SSLContext context = SSLContext.getDefault();
            System.out.println("   Default SSL Context: " + context.getProtocol());
            
            SSLParameters params = context.getDefaultSSLParameters();
            System.out.println("   Supported Protocols: " + java.util.Arrays.toString(params.getProtocols()));
            System.out.println("   Supported Cipher Suites: " + java.util.Arrays.toString(params.getCipherSuites()));
            
        } catch (Exception e) {
            System.out.println("   Error: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testDirectSSLSocket() {
        System.out.println("2. Testing Direct SSL Socket Connection:");
        
        String[] hosts = {"jsonplaceholder.typicode.com", "httpbin.org"};
        
        for (String host : hosts) {
            testSSLConnection(host, 443);
        }
        System.out.println();
    }
    
    private static void testSSLConnection(String hostname, int port) {
        try {
            System.out.println("   Testing " + hostname + ":" + port);
            
            // Create SSL socket factory
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            
            // Create socket with timeout
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(hostname, port), 10000);
            
            // Wrap with SSL
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, hostname, port, true);
            
            // Set protocols explicitly
            sslSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.1", "TLSv1"});
            
            // Perform handshake
            sslSocket.startHandshake();
            
            SSLSession session = sslSocket.getSession();
            System.out.println("     SUCCESS - Protocol: " + session.getProtocol());
            System.out.println("     Cipher Suite: " + session.getCipherSuite());
            
            sslSocket.close();
            
        } catch (Exception e) {
            System.out.println("     FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
    
    private static void testWithDifferentProtocols() {
        System.out.println("3. Testing Different TLS Protocols:");
        
        String[] protocols = {"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"};
        
        for (String protocol : protocols) {
            testSpecificProtocol("jsonplaceholder.typicode.com", 443, protocol);
        }
        System.out.println();
    }
    
    private static void testSpecificProtocol(String hostname, int port, String protocol) {
        try {
            System.out.println("   Testing " + protocol + " with " + hostname);
            
            SSLContext context = SSLContext.getInstance(protocol);
            context.init(null, null, null);
            
            SSLSocketFactory factory = context.getSocketFactory();
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(hostname, port), 10000);
            
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, hostname, port, true);
            sslSocket.setEnabledProtocols(new String[]{protocol});
            
            sslSocket.startHandshake();
            
            System.out.println("     " + protocol + ": SUCCESS");
            sslSocket.close();
            
        } catch (Exception e) {
            System.out.println("     " + protocol + ": FAILED - " + e.getMessage());
        }
    }
}