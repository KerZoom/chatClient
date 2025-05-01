package main.com.chatClient.services;

import com.google.cloud.firestore.*;
import main.com.chatClient.config.FirebaseConfig;
import main.com.chatClient.database.FirestoreUtil;

import java.util.*;

/**
 * Service for chat messaging functionality.
 */
public class ChatService {
    private final List<ChatWindowListener> messageListeners = new ArrayList<>();
    private final Firestore db = FirebaseConfig.getFirestore();
    
    // Track document IDs of messages we've already sent to listeners
    private final Set<String> notifiedMessageIds = new HashSet<>();
    
    // Track the timestamp of the most recent message
    private com.google.cloud.Timestamp latestMessageTimestamp = null;

    /**
     * Creates a new chat service.
     */
    public ChatService() {
        // Get Firestore instance
        
        // Set up message listener
        setupMessageListener();
    }

    /**
     * Sets up a real-time listener for new messages.
     */
    private void setupMessageListener() {
        if (db == null) {
            System.err.println("Cannot set up message listener: Firestore not initialized");
            return;
        }
        
        // Create query for new messages only
        db.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        System.err.println("Listen failed: " + e.getMessage());
                        return;
                    }
                    
                    if (snapshot == null || snapshot.isEmpty()) {
                        return;
                    }
                    
                    // Process document changes
                    for (DocumentChange dc : snapshot.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot document = dc.getDocument();
                            String messageId = document.getId();
                            
                            // Skip if we've already notified for this message
                            if (notifiedMessageIds.contains(messageId)) {
                                continue;
                            }
                            
                            Map<String, Object> messageData = document.getData();
                            com.google.cloud.Timestamp timestamp = (com.google.cloud.Timestamp) messageData.get("timestamp");
                            
                            // Only notify for newer messages
                            if (latestMessageTimestamp == null || 
                                (timestamp != null && timestamp.compareTo(latestMessageTimestamp) >= 0)) {
                                
                                String senderUsername = (String) messageData.get("username");
                                String message = (String) messageData.get("message");
                                
                                System.out.println("New message notification: " + messageId + " from " + senderUsername);
                                
                                // Update tracking
                                latestMessageTimestamp = timestamp;
                                notifiedMessageIds.add(messageId);

                                
                                // Notify listeners
                                notifyNewMessage(senderUsername, message);
                            }
                        }
                    }
                });
    }

    /**
     * Adds a message listener.
     * 
     * @param listener Listener to add
     */
    public void addMessageListener(ChatWindowListener listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }
    
    /**
     * Removes a message listener.
     * 
     * @param listener Listener to remove
     */
    public void removeMessageListener(ChatWindowListener listener) {
        messageListeners.remove(listener);
    }

    /**
     * Notifies all listeners of a new message.
     * 
     * @param username Sender's username
     * @param message Message content
     */
    private void notifyNewMessage(String username, String message) {
        for (ChatWindowListener listener : messageListeners) {
            listener.onNewMessage(username, message);
        }
    }

    /**
     * Sends a message.
     * 
     * @param uid User's UID
     * @param username User's username
     * @param message Message content
     * @return true if successful, false otherwise
     */
    public boolean sendMessage(String uid, String username, String message) {
        System.out.println("Sending message: " + message + " by " + uid + " (" + username + ")");
        return FirestoreUtil.addMessage(message, uid, username);
    }
}