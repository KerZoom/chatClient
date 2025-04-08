package main.com.chatClient.database;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
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

        db.collection("users").document(documentId).set(user);
    }

    public static void addMessage(String message, String senderId, String username) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("senderId", senderId);
        messageData.put("username", username);
        messageData.put("timestamp", Timestamp.now());

        db.collection("messages").add(messageData);
    }

    public static List<Map<String, Object>> getAllMessages() {
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            Firestore db = getFirestore(); // your Firestore instance
            ApiFuture<QuerySnapshot> query = db.collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get();

            List<QueryDocumentSnapshot> documents = query.get().getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                messages.add(doc.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
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