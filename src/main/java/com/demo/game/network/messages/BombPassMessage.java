package com.demo.game.network.messages;

// Sent by the server when the bomb is passed
public class BombPassMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public final int newHolderClientId;

    public BombPassMessage(int newHolderClientId) {
        this.newHolderClientId = newHolderClientId;
    }
}