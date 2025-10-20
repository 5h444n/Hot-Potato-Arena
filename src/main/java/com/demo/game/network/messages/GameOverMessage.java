package com.demo.game.network.messages;

// Sent by the server when the game ends
public class GameOverMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public final String winnerUsername; // Username of the winner

    public GameOverMessage(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }
}