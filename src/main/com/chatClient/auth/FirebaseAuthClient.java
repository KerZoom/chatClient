package main.com.chatClient.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.com.chatClient.config.FirebaseConfig;
import main.com.chatClient.database.FirestoreUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthClient {
    private static final String API_KEY = FirebaseConfig.API_KEY;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String sendPostRequest(String urlString, ObjectNode requestBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String requestBodyString = requestBody.toString();
            System.out.println("Request URL: " + urlString);
            System.out.println("Request Body: " + requestBodyString);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBodyString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

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
            System.out.println("Response: " + response.toString());
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String registerUser(String email, String password, String username) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", true);

        String response = sendPostRequest(url, requestBody);
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                if (rootNode.has("error")) {
                    System.err.println("Error during registration: " + rootNode.get("error").get("message").asText());
                    return null;
                }
                String idToken = rootNode.get("idToken").asText();
                String uid = rootNode.get("localId").asText();
                FirestoreUtil.addUser(uid, username, email);
                return idToken;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static Map<String, String> loginUser(String email, String password) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", true);

        String response = sendPostRequest(url, requestBody);
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                if (rootNode.has("error")) {
                    System.err.println("Login failed: " + rootNode.get("error").get("message").asText());
                    return null;
                }
                String idToken = rootNode.get("idToken").asText();
                String uid = rootNode.get("localId").asText();
                Map<String, String> result = new HashMap<>();
                result.put("idToken", idToken);
                result.put("documentId", uid);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}