package com.chatClient.services;

public interface ChatWindowListener {
    void onNewMessage(String username, String message);
}