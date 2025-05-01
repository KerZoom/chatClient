package main.com.chatClient.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.com.chatClient.config.FirebaseConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firebase Authentication operations like user registration and login.
 */
public class FirebaseAuthClient {
    private static final String API_KEY = FirebaseConfig.API_KEY;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Base URL for Firebase Auth REST API
    private static final String AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/v1/accounts:";

    /**
     * Registers a new user with Firebase Authentication.
     * 
     * @param email User's email address
     * @param password User's password
     * @param username User's display name
     * @return Map containing auth data (idToken, uid) or null if registration failed
     */
    public static Map<String, String> registerUser(String email, String password, String username) {
        try {
            // Create signup request
            String url = AUTH_BASE_URL + "signUp?key=" + API_KEY;
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);

            // Send request and get response
            String response = sendPostRequest(url, requestBody);
            if (response == null) {
                System.err.println("No response received from Firebase auth server");
                return null;
            }
            
            // Parse response
            JsonNode rootNode = objectMapper.readTree(response);
            if (rootNode.has("error")) {
                String errorMessage = rootNode.get("error").get("message").asText();
                System.err.println("Error during registration: " + errorMessage);
                return null;
            }
            
            // Extract auth data
            String idToken = rootNode.get("idToken").asText();
            String uid = rootNode.get("localId").asText();
            
            // Return authentication data
            Map<String, String> result = new HashMap<>();
            result.put("idToken", idToken);
            result.put("uid", uid);
            result.put("email", email);
            result.put("username", username);
            return result;
        } catch (Exception e) {
            System.err.println("Exception during registration: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Logs in an existing user with Firebase Authentication.
     * 
     * @param email User's email address
     * @param password User's password
     * @return Map containing auth data (idToken, uid) or null if login failed
     */
    public static Map<String, String> loginUser(String email, String password) {
        try {
            // Create login request
            String url = AUTH_BASE_URL + "signInWithPassword?key=" + API_KEY;
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);

            // Send request and get response
            String response = sendPostRequest(url, requestBody);
            if (response == null) {
                System.err.println("No response received from Firebase auth server");
                return null;
            }
            
            // Parse response
            JsonNode rootNode = objectMapper.readTree(response);
            if (rootNode.has("error")) {
                String errorMessage = rootNode.get("error").get("message").asText();
                System.err.println("Login failed: " + errorMessage);
                return null;
            }
            
            // Extract auth data
            String idToken = rootNode.get("idToken").asText();
            String uid = rootNode.get("localId").asText();
            
            // Return authentication data
            Map<String, String> result = new HashMap<>();
            result.put("idToken", idToken);
            result.put("uid", uid);
            result.put("email", email);
            return result;
        } catch (Exception e) {
            System.err.println("Exception during login: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends HTTP POST request to Firebase Authentication API.
     * 
     * @param urlString API endpoint URL
     * @param requestBody JSON request body
     * @return Response string or null if request failed
     */
    public static String sendPostRequest(String urlString, ObjectNode requestBody) {
        try {
            // Setup connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Send request
            String requestBodyString = requestBody.toString();
            System.out.println("Request URL: " + urlString);
            System.out.println("Request Body: " + requestBodyString);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBodyString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get response
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            StringBuilder response = new StringBuilder();
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
            }
            
            String responseStr = response.toString();
            System.out.println("Response: " + responseStr);
            return responseStr;
        } catch (Exception e) {
            System.err.println("Error sending request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}