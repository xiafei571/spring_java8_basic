package com.example;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.win.WindowsCredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.WinHttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Component
public class HttpClientFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);
    
    @Autowired
    private ProxyConfig proxyConfig;
    
    public CloseableHttpClient createHttpClient() {
        configureEnterpriseSSL();
        
        // Set system properties for proxy (similar to PowerShell approach)
        if (proxyConfig.isProxyEnabled()) {
            configureSystemProxyProperties();
        }
        
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20);
        
        // Configure timeouts
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(60000)
                .setSocketTimeout(120000)
                .setConnectionRequestTimeout(60000);
        
        // Configure proxy if enabled
        if (proxyConfig.isProxyEnabled()) {
            HttpHost proxy = new HttpHost(proxyConfig.getHost(), proxyConfig.getPortAsInt());
            requestConfigBuilder.setProxy(proxy);
            logger.info("Using proxy: {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
            
            // Configure proxy credentials if available - use simple basic auth like PowerShell
            if (proxyConfig.hasCredentials()) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt()),
                    new UsernamePasswordCredentials(proxyConfig.getUsername(), proxyConfig.getPassword())
                );
                builder.setDefaultCredentialsProvider(credentialsProvider);
                logger.info("Proxy credentials configured for user: {} (basic auth)", proxyConfig.getUsername());
            }
        }
        
        builder.setDefaultRequestConfig(requestConfigBuilder.build());
        
        // Configure SSL for enterprise environments
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();
            
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslContext, 
                    NoopHostnameVerifier.INSTANCE
            );
            
            builder.setSSLSocketFactory(sslSocketFactory);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            logger.warn("Failed to configure SSL context, using default: {}", e.getMessage());
        }
        
        return builder.build();
    }
    
    private void configureSystemProxyProperties() {
        // Set system properties similar to PowerShell proxy approach
        System.setProperty("http.proxyHost", proxyConfig.getHost());
        System.setProperty("http.proxyPort", proxyConfig.getPort());
        System.setProperty("https.proxyHost", proxyConfig.getHost());
        System.setProperty("https.proxyPort", proxyConfig.getPort());
        
        if (proxyConfig.hasCredentials()) {
            // Set proxy authentication
            System.setProperty("http.proxyUser", proxyConfig.getUsername());
            System.setProperty("http.proxyPassword", proxyConfig.getPassword());
            System.setProperty("https.proxyUser", proxyConfig.getUsername());
            System.setProperty("https.proxyPassword", proxyConfig.getPassword());
        }
        
        logger.info("System proxy properties configured: {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
    }
    
    private CredentialsProvider createCredentialsProvider() {
        // Try Windows integrated authentication first if domain is specified or on Windows
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");
        
        if (isWindows && (proxyConfig.getDomain() != null || isWindowsIntegratedAuth())) {
            try {
                // Use Windows credentials provider for NTLM/Negotiate
                WindowsCredentialsProvider winCredProvider = new WindowsCredentialsProvider(
                    new BasicCredentialsProvider()
                );
                
                // Add NTLM credentials if domain is specified
                if (proxyConfig.getDomain() != null) {
                    NTCredentials ntCredentials = new NTCredentials(
                        proxyConfig.getUsername(),
                        proxyConfig.getPassword(),
                        getWorkstation(),
                        proxyConfig.getDomain()
                    );
                    winCredProvider.setCredentials(
                        new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt()),
                        ntCredentials
                    );
                }
                
                logger.info("Using Windows integrated authentication");
                return winCredProvider;
            } catch (Exception e) {
                logger.warn("Failed to configure Windows authentication, falling back to basic: {}", e.getMessage());
            }
        }
        
        // Fallback to basic authentication
        BasicCredentialsProvider basicProvider = new BasicCredentialsProvider();
        basicProvider.setCredentials(
            new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt()),
            new UsernamePasswordCredentials(proxyConfig.getUsername(), proxyConfig.getPassword())
        );
        logger.info("Using basic authentication");
        return basicProvider;
    }
    
    private boolean isWindowsIntegratedAuth() {
        // Check if we should use Windows integrated authentication
        // This could be based on missing username/password or explicit configuration
        return proxyConfig.getUsername() == null || proxyConfig.getUsername().trim().isEmpty();
    }
    
    private String getWorkstation() {
        // Get workstation name for NTLM
        String workstation = System.getenv("COMPUTERNAME");
        if (workstation == null || workstation.trim().isEmpty()) {
            workstation = "localhost";
        }
        return workstation;
    }
    
    private static void configureEnterpriseSSL() {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("jdk.tls.useExtendedMasterSecret", "false");
        
        // Enable debug logging for proxy and SSL
        System.setProperty("java.net.debug", "all");
        System.setProperty("javax.net.debug", "ssl,handshake");
        
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("windows")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
        
        LoggerFactory.getLogger(HttpClientFactory.class).info("Enterprise SSL configured for OS: {}", osName);
    }
}