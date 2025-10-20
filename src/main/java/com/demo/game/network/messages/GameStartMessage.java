package com.demo.game.network.messages;

import java.util.List;
import java.util.Map;

/**
 * Sent by the server to all clients when the game begins.
 * Contains all necessary information to spawn the initial game state.
 */
public class GameStartMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    /** Maps a client's ID to their starting position using the serializable SPoint2D. */
    public final Map<Integer, SPoint2D> initialPositions;

    /** An ordered list of usernames. The index corresponds to the client ID. */
    public final List<String> usernames;

    public GameStartMessage(Map<Integer, SPoint2D> initialPositions, List<String> usernames) {
        this.initialPositions = initialPositions;
        this.usernames = usernames;
    }
}
