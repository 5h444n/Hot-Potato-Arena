package com.demo.game.network.messages;

import java.util.List;

// Sent by the server to all clients to update the list of players in the lobby
public class LobbyUpdateMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public final List<String> playerUsernames;

    public LobbyUpdateMessage(List<String> playerUsernames) {
        this.playerUsernames = playerUsernames;
    }
}