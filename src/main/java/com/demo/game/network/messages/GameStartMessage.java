package com.demo.game.network.messages;

import java.util.List;
import javafx.geometry.Point2D;
import java.util.Map;

// Sent by the host to tell clients the game is starting and provides initial positions
public class GameStartMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    // Map Client ID (assigned by server) to their starting position
    public final Map<Integer, Point2D> initialPositions;
    public final List<String> usernames; // In order of client IDs

    public GameStartMessage(Map<Integer, Point2D> initialPositions, List<String> usernames) {
        this.initialPositions = initialPositions;
        this.usernames = usernames;
    }
}