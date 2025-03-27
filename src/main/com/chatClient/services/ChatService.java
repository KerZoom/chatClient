package main.com.chatClient.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import main.com.chatClient.database.FirestoreUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatService {
    private final List<ChatWindowListener> messageListeners = new ArrayList<>();
    private final Firestore db;

    public ChatService() {
        try {
            FileInputStream serviceAccount = new FileInputStream("firebase-adminsdk.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            this.db = FirestoreClient.getFirestore();
            setupMessageListener();
        } catch (IOException e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    private void setupMessageListener() {
        db.collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        System.err.println("Listen failed: " + e);
                        return;
                    }
                    if (snapshot != null) {
                        for (DocumentChange dc : snapshot.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Map<String, Object> messageData = dc.getDocument().getData();
                                String senderUsername = (String) messageData.get("username");
                                String message = (String) messageData.get("message");
                                notifyNewMessage(senderUsername, message);
                            }
                        }
                    }
                });
    }

    public void addMessageListener(ChatWindowListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(ChatWindowListener listener) {
        messageListeners.remove(listener);
    }

    private void notifyNewMessage(String username, String message) {
        for (ChatWindowListener listener : messageListeners) {
            listener.onNewMessage(username, message);
        }
    }

    public void sendMessage(String email, String username, String message) {
        DocumentSnapshot userDoc = FirestoreUtil.getUserByEmail(email);
        if (userDoc != null && userDoc.exists()) {
            String senderId = userDoc.getId();
            FirestoreUtil.addMessage(message, senderId, username);
        } else {
            System.err.println("User not found with email: " + email);
        }
    }
}
