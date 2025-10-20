package com.demo.game.network.messages;

// Sent by the server when a player is eliminated
public class PlayerEliminatedMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public final int eliminatedClientId;

    public PlayerEliminatedMessage(int eliminatedClientId) {
        this.eliminatedClientId = eliminatedClientId;
    }
}