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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

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
        
        // Enable preemptive authentication to avoid 407 challenge
        builder.addInterceptorFirst(new PreemptiveAuthInterceptor());
        
        // Configure timeouts and authentication schemes
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(60000)
                .setSocketTimeout(120000)
                .setConnectionRequestTimeout(60000)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC, AuthSchemes.NTLM))
                .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC, AuthSchemes.NTLM));
        
        // Configure proxy if enabled
        if (proxyConfig.isProxyEnabled()) {
            // Explicitly use HTTP proxy (not SOCKS)
            HttpHost proxy = new HttpHost(proxyConfig.getHost(), proxyConfig.getPortAsInt(), "http");
            requestConfigBuilder.setProxy(proxy);
            logger.info("Using HTTP proxy: {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
            
            // Configure proxy credentials for NTLM/NEGOTIATE authentication
            if (proxyConfig.hasCredentials()) {
                CredentialsProvider credentialsProvider = createCredentialsProvider();
                builder.setDefaultCredentialsProvider(credentialsProvider);
                logger.info("Proxy credentials configured for user: {} (NTLM/NEGOTIATE)", proxyConfig.getUsername());
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
        // CRITICAL: Force disable ALL system proxy detection
        System.setProperty("java.net.useSystemProxies", "false");
        
        // CRITICAL: Complete SOCKS proxy elimination
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("socksProxyVersion");
        System.clearProperty("socksNonProxyHosts");
        
        System.setProperty("socksProxyHost", "");
        System.setProperty("socksProxyPort", "");
        System.setProperty("socksProxyVersion", "");
        System.setProperty("socksNonProxyHosts", "");
        
        // Force disable automatic proxy detection on Windows
        System.setProperty("java.net.useSystemProxies", "false");
        System.setProperty("com.sun.net.useExclusiveBind", "false");
        
        // Network configuration
        System.setProperty("networkaddress.cache.ttl", "0");
        System.setProperty("networkaddress.cache.negative.ttl", "0");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        
        // Set ONLY HTTP proxy properties (explicitly not SOCKS)
        System.setProperty("http.proxyHost", proxyConfig.getHost());
        System.setProperty("http.proxyPort", proxyConfig.getPort());
        System.setProperty("https.proxyHost", proxyConfig.getHost());
        System.setProperty("https.proxyPort", proxyConfig.getPort());
        
        if (proxyConfig.hasCredentials()) {
            System.setProperty("http.proxyUser", proxyConfig.getUsername());
            System.setProperty("http.proxyPassword", proxyConfig.getPassword());
            System.setProperty("https.proxyUser", proxyConfig.getUsername());
            System.setProperty("https.proxyPassword", proxyConfig.getPassword());
        }
        
        logger.info("HTTP proxy properties configured (SOCKS disabled): {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
    }
    
    private CredentialsProvider createCredentialsProvider() {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        AuthScope proxyAuthScope = new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt());
        
        String username = proxyConfig.getUsername();
        String password = proxyConfig.getPassword();
        String domain = proxyConfig.getDomain();
        
        // Extract domain from username if format is domain\username
        if (domain == null && username.contains("\\")) {
            String[] parts = username.split("\\\\", 2);
            domain = parts[0];
            username = parts[1];
            logger.info("Extracted domain '{}' from username", domain);
        }
        
        // PowerShell ProxyCredential uses Basic Auth with preemptive authentication
        // This is the most reliable approach for corporate proxies
        UsernamePasswordCredentials basicCredentials = new UsernamePasswordCredentials(
            username, 
            password
        );
        credentialsProvider.setCredentials(proxyAuthScope, basicCredentials);
        
        // Also try NTLM if domain is available (as backup)
        if (domain != null && !domain.trim().isEmpty()) {
            try {
                NTCredentials ntCredentials = new NTCredentials(
                    username,
                    password,
                    getWorkstation(),
                    domain
                );
                credentialsProvider.setCredentials(
                    new AuthScope(proxyConfig.getHost(), proxyConfig.getPortAsInt(), AuthScope.ANY_REALM, "ntlm"),
                    ntCredentials
                );
                logger.info("Configured both Basic and NTLM auth: user={}, domain={}", username, domain);
            } catch (Exception e) {
                logger.warn("Failed to add NTLM credentials: {}", e.getMessage());
            }
        } else {
            logger.info("Configured Basic auth (like PowerShell ProxyCredential): user={}", username);
        }
        
        return credentialsProvider;
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
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.1,TLSv1");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("jdk.tls.useExtendedMasterSecret", "false");
        
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("windows")) {
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
        
        LoggerFactory.getLogger(HttpClientFactory.class).info("Enterprise SSL configured for OS: {}", osName);
    }
    
    /**
     * Preemptive authentication interceptor to avoid 407 challenges
     * This sends credentials proactively like PowerShell does
     */
    private static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            AuthState proxyAuthState = (AuthState) context.getAttribute(HttpClientContext.PROXY_AUTH_STATE);
            
            if (proxyAuthState != null && proxyAuthState.getAuthScheme() == null) {
                CredentialsProvider credentialsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
                HttpHost proxy = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                
                if (credentialsProvider != null && proxy != null) {
                    Credentials credentials = credentialsProvider.getCredentials(new AuthScope(proxy));
                    if (credentials != null) {
                        proxyAuthState.update(new BasicScheme(), credentials);
                    }
                }
            }
        }
    }
}