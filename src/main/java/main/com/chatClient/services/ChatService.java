package main.com.chatClient.services;

import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import main.com.chatClient.database.FirestoreUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatService {
    private final List<ChatWindowListener> messageListeners = new ArrayList<>();
    private final Firestore db;

    public ChatService() {
        this.db = FirestoreUtil.getFirestore();
        setupMessageListener();
    }

    private void setupMessageListener() {
        db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        System.err.println("Listen failed: " + e);
                        return;
                    }
                    if (snapshot != null && !snapshot.isEmpty()) {
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

    private void notifyNewMessage(String username, String message) {
        for (ChatWindowListener listener : messageListeners) {
            listener.onNewMessage(username, message);
        }
    }

    public void sendMessage(String uid, String username, String message) {
        System.out.println("Message sending: " + message + " by " + uid + " (" + username + ")....");
        FirestoreUtil.addMessage(message, uid, username);
        System.out.println("Message sent: " + message + " by " + uid + " (" + username + ")");
    }
}