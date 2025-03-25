package com.chatClient.models;

import com.google.cloud.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String senderEmail;
    private String senderUsername;
    private String message;
    private Timestamp timestamp;

    public Message(String senderEmail, String senderUsername, String message) {
        this.senderEmail = senderEmail;
        this.senderUsername = senderUsername;
        this.message = message;
        this.timestamp = Timestamp.now();
    }

    public Map<String, Object> toFirestoreMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("senderEmail", senderEmail);
        map.put("username", senderUsername);
        map.put("message", message);
        map.put("timestamp", timestamp);
        return map;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp.getSeconds();
    }
}