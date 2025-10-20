package com.demo.game.network.messages;
import javafx.geometry.Point2D;
import java.util.Map;
public class GameStateUpdateMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public final Map<Integer, Point2D> playerPositions;
    public final int bombHolderId;
    public final Point2D bombPosition;
    public final double bombTimerRemaining;
    public GameStateUpdateMessage(Map<Integer, Point2D> playerPositions, int bombHolderId, Point2D bombPosition, double bombTimerRemaining) {
        this.playerPositions = playerPositions;
        this.bombHolderId = bombHolderId;
        this.bombPosition = bombPosition;
        this.bombTimerRemaining = bombTimerRemaining;
    }
}