package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class App implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    @Value("${app.get.url}")
    private String getUrl;
    
    @Value("${app.post.url}")
    private String postUrl;

    @Autowired
    private HttpClientFactory httpClientFactory;
    
    @Autowired
    private HttpRequestService httpRequestService;
    
    @Autowired
    private ProxyArgumentParser proxyArgumentParser;
    
    @Autowired
    private NetworkConfigurationService networkConfigurationService;
    
    @Autowired
    private ProxyConfig proxyConfig;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting CLI application");
        
        // Parse command line arguments for proxy settings
        proxyArgumentParser.parseProxyArguments(args);
        
        // Configure network settings
        networkConfigurationService.configureNetworkSettings();
        
        // Log all command line arguments
        logger.info("Command line arguments: {}", Arrays.toString(args));
        
        // Check if user wants to use the minimal HttpURLConnection example
        boolean useMinimalExample = hasArgument(args, "-useMinimal");
        
        try {
            if (useMinimalExample) {
                logger.info("Using minimal HttpURLConnection example");
                testMinimalHttpURLConnection();
            } else {
                logger.info("Using original HTTP request methods");
                // Perform GET request
                httpRequestService.performGetRequest(getUrl);
                
                // Perform POST request
                Map<String, String> payload = new HashMap<>();
                payload.put("ping", "hello-from-cli");
                httpRequestService.performPostRequest(postUrl, payload);
            }
            
            logger.info("All HTTP requests completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during HTTP requests", e);
        } finally {
            logger.info("Application finished, exiting...");
            System.exit(0);
        }
    }
    
    private boolean hasArgument(String[] args, String argument) {
        for (String arg : args) {
            if (argument.equals(arg)) {
                return true;
            }
        }
        return false;
    }
    
    private void testMinimalHttpURLConnection() {
        logger.info("Testing minimal HttpURLConnection to https://www.google.com");
        
        if (!proxyConfig.isProxyEnabled()) {
            logger.warn("No proxy configuration found. Please provide proxy settings via command line arguments.");
            return;
        }
        
        String result = HttpRequestService.callWithHttpURLConnection(
            proxyConfig.getHost(),
            proxyConfig.getPortAsInt(),
            proxyConfig.getUsername(),
            proxyConfig.getPassword()
        );
        
        logger.info("Minimal HttpURLConnection result:\n{}", result);
    }
}