package com.example;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Deep SSL diagnostic for enterprise environments
 */
public class DeepSSLDiagnostic {

    public static void main(String[] args) {
        System.out.println("=== Deep SSL Diagnostic ===\n");
        
        analyzeSystemCertificates();
        testWithSystemKeystore();
        testRawSSLConnection();
        testJavaVMFlags();
    }
    
    private static void analyzeSystemCertificates() {
        System.out.println("1. System Certificate Analysis:");
        
        try {
            // Check default truststore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    X509TrustManager x509tm = (X509TrustManager) tm;
                    X509Certificate[] certs = x509tm.getAcceptedIssuers();
                    System.out.println("   Default truststore has " + certs.length + " certificates");
                    
                    // Look for corporate certificates
                    int corporateCerts = 0;
                    for (X509Certificate cert : certs) {
                        String issuer = cert.getIssuerDN().getName();
                        if (issuer.toLowerCase().contains("corporate") || 
                            issuer.toLowerCase().contains("company") ||
                            issuer.toLowerCase().contains("enterprise")) {
                            corporateCerts++;
                        }
                    }
                    System.out.println("   Potential corporate certificates: " + corporateCerts);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("   Error analyzing certificates: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testWithSystemKeystore() {
        System.out.println("2. Testing with Windows System Keystore:");
        
        // Test with Windows-ROOT truststore
        String[] trustStoreTypes = {"Windows-ROOT", "Windows-MY", "JKS"};
        
        for (String storeType : trustStoreTypes) {
            try {
                System.setProperty("javax.net.ssl.trustStoreType", storeType);
                
                SSLContext context = SSLContext.getInstance("TLSv1.2");
                context.init(null, null, null);
                
                testQuickConnection("jsonplaceholder.typicode.com", 443, context);
                System.out.println("   SUCCESS with " + storeType + " truststore");
                
            } catch (Exception e) {
                System.out.println("   FAILED with " + storeType + ": " + e.getMessage());
            }
        }
        System.out.println();
    }
    
    private static void testRawSSLConnection() {
        System.out.println("3. Raw SSL Connection Test:");
        
        String[] protocols = {"TLSv1.2", "TLSv1.1", "TLSv1"};
        String[] hosts = {"jsonplaceholder.typicode.com", "httpbin.org"};
        
        for (String host : hosts) {
            System.out.println("   Testing " + host + ":");
            for (String protocol : protocols) {
                testRawSSL(host, 443, protocol);
            }
        }
        System.out.println();
    }
    
    private static void testRawSSL(String hostname, int port, String protocol) {
        try {
            // Create socket with explicit protocol
            SSLContext context = SSLContext.getInstance(protocol);
            context.init(null, null, null);
            
            SSLSocketFactory factory = context.getSocketFactory();
            
            // Create regular socket first
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(hostname, port), 10000);
            
            // Wrap with SSL
            SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, hostname, port, true);
            sslSocket.setEnabledProtocols(new String[]{protocol});
            
            // Try handshake with timeout
            sslSocket.setSoTimeout(30000);
            sslSocket.startHandshake();
            
            SSLSession session = sslSocket.getSession();
            System.out.println("     " + protocol + ": SUCCESS - " + session.getCipherSuite());
            
            sslSocket.close();
            
        } catch (Exception e) {
            System.out.println("     " + protocol + ": FAILED - " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("certificate")) {
                System.out.println("       ^ Certificate issue detected");
            }
        }
    }
    
    private static void testQuickConnection(String hostname, int port, SSLContext context) throws Exception {
        SSLSocketFactory factory = context.getSocketFactory();
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(hostname, port), 5000);
        
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, hostname, port, true);
        sslSocket.setSoTimeout(10000);
        sslSocket.startHandshake();
        sslSocket.close();
    }
    
    private static void testJavaVMFlags() {
        System.out.println("4. Recommended JVM Flags for your environment:");
        System.out.println("   Based on diagnosis, try these VM options in IDEA:");
        System.out.println();
        
        System.out.println("   Option A (Windows Certificate Store):");
        System.out.println("   -Djavax.net.ssl.trustStoreType=Windows-ROOT");
        System.out.println("   -Djava.net.useSystemProxies=true");
        System.out.println("   -Dhttps.protocols=TLSv1.2");
        System.out.println();
        
        System.out.println("   Option B (Legacy Compatibility):");
        System.out.println("   -Djava.net.useSystemProxies=true");
        System.out.println("   -Dhttps.protocols=TLSv1.2,TLSv1.1,TLSv1");
        System.out.println("   -Dcom.sun.net.ssl.checkRevocation=false");
        System.out.println("   -Djdk.tls.useExtendedMasterSecret=false");
        System.out.println();
        
        System.out.println("   Option C (Corporate Network):");
        System.out.println("   -Djava.net.useSystemProxies=true");
        System.out.println("   -Djavax.net.ssl.trustStoreType=Windows-ROOT");
        System.out.println("   -Dhttps.protocols=TLSv1.2");
        System.out.println("   -Dcom.sun.net.ssl.checkRevocation=false");
        System.out.println("   -Djdk.tls.allowUnsafeServerCertChange=true");
        System.out.println("   -Dsun.security.ssl.allowUnsafeRenegotiation=true");
        System.out.println();
        
        System.out.println("   Option D (Debug - to see what's happening):");
        System.out.println("   -Djavax.net.debug=ssl:handshake:verbose");
        System.out.println("   -Djava.net.useSystemProxies=true");
        System.out.println();
    }
}