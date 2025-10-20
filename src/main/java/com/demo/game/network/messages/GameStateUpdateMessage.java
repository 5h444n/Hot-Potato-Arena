package com.demo.game.network.messages;

import java.util.Map;

/**
 * Sent periodically by the server to all clients to synchronize the game state.
 * This is the primary message for real-time updates during gameplay.
 */
public class GameStateUpdateMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;

    /** Maps each client ID to their current, server-authoritative position. */
    public final Map<Integer, SPoint2D> playerPositions;

    /** The client ID of the player currently holding the bomb. -1 if no one. */
    public final int bombHolderId;

    /** The current, server-authoritative position of the bomb. */
    public final SPoint2D bombPosition;

    /** The remaining time on the bomb's timer, for UI display. */
    public final double bombTimerRemaining;

    public GameStateUpdateMessage(Map<Integer, SPoint2D> playerPositions, int bombHolderId, SPoint2D bombPosition, double bombTimerRemaining) {
        this.playerPositions = playerPositions;
        this.bombHolderId = bombHolderId;
        this.bombPosition = bombPosition;
        this.bombTimerRemaining = bombTimerRemaining;
    }
}
