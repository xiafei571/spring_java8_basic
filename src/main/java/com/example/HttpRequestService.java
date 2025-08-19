package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
public class HttpRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestService.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private ProxyConfig proxyConfig;
    
    public void performGetRequest(String url) throws IOException {
        logger.info("Performing GET request to: {}", url);
        performRawHttpRequest(url);
    }
    
    public void performPostRequest(String url, Object payload) throws IOException {
        logger.info("Performing POST request to: {}", url);
        String jsonPayload = objectMapper.writeValueAsString(payload);
        performRawHttpPostRequest(url, jsonPayload);
    }
    
    private void performRawHttpRequest(String urlString) throws IOException {
        try {
            configureSSLTrustAll();
            
            String credentials = proxyConfig.getUsername() + ":" + proxyConfig.getPassword();
            String authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPortAsInt()));
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(getTrustAllSSLSocketFactory());
                httpsConnection.setHostnameVerifier(getTrustAllHostnameVerifier());
            }
            
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Java-PowerShell-Compatible/1.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Proxy-Authorization", authHeaderValue);
            
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            
            logger.info("Raw HTTP Response - Code: {}, Message: {}", responseCode, responseMessage);
            
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
            
            logger.info("Raw HTTP Response Body: {}", response.toString());
            
        } catch (Exception e) {
            logger.error("Raw HTTP request failed: {}", e.getMessage(), e);
        }
    }
    
    private void performRawHttpPostRequest(String urlString, String jsonPayload) throws IOException {
        try {
            configureSSLTrustAll();
            
            String credentials = proxyConfig.getUsername() + ":" + proxyConfig.getPassword();
            String authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
            
            Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPortAsInt()));
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                httpsConnection.setSSLSocketFactory(getTrustAllSSLSocketFactory());
                httpsConnection.setHostnameVerifier(getTrustAllHostnameVerifier());
            }
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Java-PowerShell-Compatible/1.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Proxy-Authorization", authHeaderValue);
            connection.setDoOutput(true);
            
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            connection.getOutputStream().write(jsonPayload.getBytes("UTF-8"));
            connection.getOutputStream().flush();
            connection.getOutputStream().close();
            
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            
            logger.info("Raw HTTP POST Response - Code: {}, Message: {}", responseCode, responseMessage);
            
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
            
            logger.info("Raw HTTP POST Response Body: {}", response.toString());
            
        } catch (Exception e) {
            logger.error("Raw HTTP POST request failed: {}", e.getMessage(), e);
        }
    }
    
    private void configureSSLTrustAll() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) { return true; }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
        } catch (Exception e) {
            logger.warn("Failed to configure SSL trust all: {}", e.getMessage());
        }
    }
    
    private SSLSocketFactory getTrustAllSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            logger.warn("Failed to create trust-all SSL socket factory: {}", e.getMessage());
            return null;
        }
    }
    
    private HostnameVerifier getTrustAllHostnameVerifier() {
        return new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { 
                return true; 
            }
        };
    }
}