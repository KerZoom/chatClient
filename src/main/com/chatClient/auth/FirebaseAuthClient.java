package main.com.chatClient.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import main.com.chatClient.database.FirestoreUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseAuthClient {
    private static final String API_KEY = "AIzaSyCGqisilUJHpLCESYQgML09tHTuZQUJw04"; // Replace with your actual API key
    private static final Firestore db = FirestoreUtil.getFirestore();
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

    /**
     * Registers a new user with Firebase Authentication and stores user details in Firestore.
     *
     * @param email     The user's email.
     * @param password  The user's password.
     * @param username  The user's username.
     * @return The Firebase ID token if registration is successful, null otherwise.
     */
    public static String registerUser(String email, String password, String username) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", true);


        String response = sendPostRequest(url, requestBody);
        if (response != null) {
            try {
                String jsonResponse = response.toString();
                System.out.println("JSON Response: " + jsonResponse);
                JsonNode rootNode = objectMapper.readTree(jsonResponse);

                if (rootNode.has("error")) {
                    System.err.println("Error during registration: " + rootNode.get("error").get("message").asText());
                    return null;
                }

                String idToken = rootNode.get("idToken").asText();
                String uid = rootNode.get("localId").asText();
                storeUserInFirestoreAndAuth(uid, email, username);
                return idToken;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Logs in an existing user with Firebase Authentication.
     *
     * @param email    The user's email.
     * @param password The user's password.
     * @return A map containing the Firebase ID token and the user's document ID if login is successful, null otherwise.
     */
    public static Map<String, String> loginUser(String email, String password) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("returnSecureToken", true);

        String response = sendPostRequest(url, requestBody);
        if (response != null) {
            try {
                String jsonResponse = response.toString();
                String idToken = objectMapper.readTree(jsonResponse).get("idToken").asText();
                DocumentSnapshot userDoc = FirestoreUtil.getUserByEmail(email);
                if (userDoc != null && userDoc.exists()) {
                    String uid = userDoc.getId();
                    Map<String, String> result = new HashMap<>();
                    result.put("idToken", idToken);
                    result.put("documentId", uid);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Stores the user's details in Firestore and Firebase Authentication.
     *
     * @param uid      The user's unique ID from Firebase Authentication.
     * @param email    The user's email.
     * @param username The user's username.
     */
    private static void storeUserInFirestoreAndAuth(String uid, String email, String username) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("createdAt", Instant.now());

        try {
            db.collection("users").document(uid).set(user).get();
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                    .setDisplayName(username);
            FirebaseAuth.getInstance().updateUser(request);
        } catch (InterruptedException | ExecutionException | FirebaseAuthException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if a username already exists.
     *
     * @param username The username to check.
     * @return True if the username exists, false otherwise.
     */
    public static boolean usernameExists(String username) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(username);
            return userRecord != null;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }

    /**
     * Checks if an email already exists.
     *
     * @param email The email to check.
     * @return True if the email exists, false otherwise.
     */
    public static boolean emailExists(String email) {
        return FirestoreUtil.emailExists(email);
    }
}