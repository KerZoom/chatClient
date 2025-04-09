package main.com.chatClient.database;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import main.com.chatClient.config.FirebaseConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirestoreUtil {

    private static final Firestore db = FirebaseConfig.getFirestore();

    public static Firestore getFirestore() {
        return db;
    }

    public static void addUser(String documentId, String username, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
;
        try {
            db.collection("users").document(documentId).create(user);
            System.out.println("User created with UID: " + documentId);
        } catch (Exception e) {
            System.err.println("Failed to create user: " + e.getMessage());
            throw new RuntimeException("Error creating user in Firestore", e);
        }
    }
    public static void addMessage(String message, String uid, String username) {
        System.out.println("Db: " + db);
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("senderId", uid);
        messageData.put("timestamp", com.google.cloud.Timestamp.now());
        messageData.put("username", username);

        try {
            db.collection("messages").add(messageData);
            System.out.println("Message added successfully by " + uid);
        } catch (Exception e) {
            System.err.println("Failed to add message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<Map<String, Object>> getAllMessages() {
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            Query query = db.collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
                messages.add(document.getData());
            }
            return messages;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return messages;
        }
    }

    public static DocumentSnapshot getUserByEmail(String email) {
        try {
            Query query = db.collection("users").whereEqualTo("email", email);
            QuerySnapshot querySnapshot = query.get().get();
            if (!querySnapshot.isEmpty()) {
                return querySnapshot.getDocuments().get(0);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getUsername(String documentId) {
        try {
            DocumentSnapshot document = db.collection("users").document(documentId).get().get();
            if (document.exists()) {
                return document.getString("username");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}