package main.com.chatClient.database;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import main.com.chatClient.config.FirebaseConfig;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Utility class for Firestore database operations.
 */
public class FirestoreUtil {

    private static final Firestore db = FirebaseConfig.getFirestore();

    /**
     * Gets the Firestore database instance, initializing if necessary.
     *
     * @return Firestore instance
     */

    /**
     * Adds or updates a user in Firestore.
     *
     * @param uid User's unique ID from Firebase Auth
     * @param username User's display name
     * @param email User's email address
     * @return true if successful, false otherwise
     */
    public static boolean addUser(String uid, String username, String email) {
        try {
            if (db == null) {
                System.err.println("Firestore not initialized");
                return false;
            }

            Map<String, Object> user = new HashMap<>();
            user.put("username", username);
            user.put("email", email);
            user.put("createdAt", com.google.cloud.Timestamp.now());

            db.collection("users").document(uid).set(user).get();
            System.out.println("User created/updated with UID: " + uid);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to create/update user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a message to Firestore.
     *
     * @param message Message text
     * @param uid User's unique ID
     * @param username User's display name
     * @return true if successful, false otherwise
     */
    public static boolean addMessage(String message, String uid, String username) {
        try {
            if (db == null) {
                System.err.println("Firestore not initialized");
                return false;
            }

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("message", message);
            messageData.put("senderId", uid);
            messageData.put("senderEmail", uid);
            messageData.put("timestamp", com.google.cloud.Timestamp.now());
            messageData.put("username", username);

            DocumentReference docRef = db.collection("messages").add(messageData).get();
            System.out.println("Message added successfully by " + uid + " (" + username + ") with ID: " + docRef.getId());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to add message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the most recent messages from Firestore.
     *
     * @param limit Maximum number of messages to retrieve
     * @return List of message data maps in chronological order (oldest to newest)
     */
    public static List<Map<String, Object>> getRecentMessages(int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            
            if (db == null) {
                System.err.println("Firestore not initialized");
                return messages;
            }

            Query query = db.collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                messages.add(document.getData());
            }

            // Reverse to chronological order (oldest first)
            Collections.reverse(messages);
            System.out.println("Retrieved " + messages.size() + " recent messages (oldest first)");
            return messages;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving recent messages: " + e.getMessage());
            e.printStackTrace();
            return messages;
        }
    }

    /**
     * Gets messages older than a specified timestamp.
     *
     * @param timestamp Timestamp of the oldest message currently loaded
     * @param limit Maximum number of messages to retrieve
     * @return List of message data maps in chronological order (oldest to newest)
     */
    public static List<Map<String, Object>> getOlderMessages(com.google.cloud.Timestamp timestamp, int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            
            if (db == null) {
                System.err.println("Firestore not initialized");
                return messages;
            }

            Query query = db.collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(timestamp)
                    .limit(limit);

            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                Map<String, Object> data = new HashMap<>(document.getData());
                data.put("id", document.getId());
                messages.add(data);
            }

            // Reverse to chronological order (oldest first)
            Collections.reverse(messages);
            System.out.println("Retrieved " + messages.size() + " older messages (oldest first)");
            return messages;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving older messages: " + e.getMessage());
            e.printStackTrace();
            return messages;
        }
    }

    /**
     * Gets a user document by email.
     *
     * @param email User's email address
     * @return DocumentSnapshot or null if not found
     */
    public static DocumentSnapshot getUserByEmail(String email) {
        try {
            
            if (db == null) {
                System.err.println("Firestore not initialized");
                return null;
            }

            Query query = db.collection("users").whereEqualTo("email", email);
            QuerySnapshot querySnapshot = query.get().get();

            if (!querySnapshot.isEmpty()) {
                return querySnapshot.getDocuments().get(0);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error retrieving user by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a user document by UID.
     *
     * @param uid User's unique ID
     * @return Map of user data or null if not found
     */
    public static Map<String, Object> getUserByUid(String uid) {
        try {
            
            if (db == null) {
                System.err.println("Firestore not initialized");
                return null;
            }

            DocumentReference docRef = db.collection("users").document(uid);
            DocumentSnapshot document = docRef.get().get();

            if (document.exists()) {
                System.out.println("User found: " + document.getData());
                return document.getData();
            } else {
                System.out.println("No user found with ID: " + uid);
            }
        } catch (Exception e) {
            System.err.println("Error retrieving user by UID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a username from Firestore by user UID.
     *
     * @param uid User's unique ID
     * @return Username or null if not found
     */
    public static String getUsername(String uid) {
        try {
            Map<String, Object> userData = getUserByUid(uid);
            if (userData != null && userData.containsKey("username")) {
                return (String) userData.get("username");
            }
        } catch (Exception e) {
            System.err.println("Error retrieving username: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}