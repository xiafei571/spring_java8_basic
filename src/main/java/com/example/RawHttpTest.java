package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Base64;

public class RawHttpTest {
    
    public static void main(String[] args) throws Exception {
        
        // Proxy configuration
        String proxyHost = "inet-proxy-b.adns.ubs.net";
        int proxyPort = 8085;
        String username = "svc_flare_dev";
        String password = args.length > 0 ? args[0] : "your_password";
        
        // Disable system proxy detection
        System.setProperty("java.net.useSystemProxies", "false");
        
        // Create Basic auth header
        String credentials = username + ":" + password;
        String authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        
        System.out.println("Making raw HTTP request through proxy...");
        System.out.println("Proxy: " + proxyHost + ":" + proxyPort);
        System.out.println("User: " + username);
        System.out.println("Auth header length: " + authHeaderValue.length());
        
        try {
            // Create proxy object
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            
            // Create URL connection through proxy
            URL url = new URL("https://jsonplaceholder.typicode.com/todos/1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            
            // Set method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Java-Raw-HTTP/1.0");
            connection.setRequestProperty("Accept", "application/json");
            
            // Add proxy authentication header
            connection.setRequestProperty("Proxy-Authorization", authHeaderValue);
            
            // Set timeouts
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            
            // Make the request
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            
            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Message: " + responseMessage);
            
            // Read response
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
            
            System.out.println("Response Body:");
            System.out.println(response.toString());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}