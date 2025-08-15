package com.example;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
            
            // Configure proxy credentials if available
            if (proxyConfig.hasCredentials()) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt()),
                    new UsernamePasswordCredentials(proxyConfig.getUsername(), proxyConfig.getPassword())
                );
                builder.setDefaultCredentialsProvider(credentialsProvider);
                logger.info("Proxy credentials configured for user: {}", proxyConfig.getUsername());
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
    
    private static void configureEnterpriseSSL() {
        System.setProperty("java.net.useSystemProxies", "true");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("jdk.tls.useExtendedMasterSecret", "false");
        
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("windows")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }
}