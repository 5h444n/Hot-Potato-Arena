package com.demo.game.network.messages;

// Sent by a client when they first connect to tell the server their username
public class ClientInfoMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public final String username;

    public ClientInfoMessage(String username) {
        this.username = username;
    }
}