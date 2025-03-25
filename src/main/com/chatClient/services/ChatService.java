package com.chatClient.services;

import com.chatClient.database.FirestoreUtil;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ChatService {
    private final List<ChatWindowListener> messageListeners = new ArrayList<>();

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

    /**
     * Sends a message to the chat.
     *
     * @param email    The email of the sender.
     * @param username The username of the sender.
     * @param message  The message content.
     */
    public void sendMessage(String email, String username, String message) {
        // Get the sender's document ID using their email
        DocumentSnapshot userDoc = FirestoreUtil.getUserByEmail(email);
        if (userDoc != null && userDoc.exists()) {
            String senderId = userDoc.getId();

            // Add the message to Firestore
            FirestoreUtil.addMessage(message, senderId, username);

            // Notify listeners about the new message
            notifyNewMessage(username, message);
        } else {
            System.err.println("User not found with email: " + email);
        }
    }

    /**
     * Retrieves the document ID of a user by their username.
     *
     * @param username The username of the user.
     * @return The document ID of the user, or null if not found.
     */
    public String getDocumentIdByUsername(String username) {
        DocumentSnapshot userDoc = FirestoreUtil.getUserByUsername(username);
        if (userDoc != null && userDoc.exists()) {
            return userDoc.getId();
        }
        return null;
    }

    /**
     * Retrieves the document ID of a user by their email.
     *
     * @param email The email of the user.
     * @return The document ID of the user, or null if not found.
     */
    public String getDocumentIdByEmail(String email) {
        DocumentSnapshot userDoc = FirestoreUtil.getUserByEmail(email);
        if (userDoc != null && userDoc.exists()) {
            return userDoc.getId();
        }
        return null;
    }
}