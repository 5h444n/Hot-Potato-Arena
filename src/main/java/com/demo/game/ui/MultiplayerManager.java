// File: com/demo/game/ui/MultiplayerManager.java
package com.demo.game.ui;

import com.demo.game.GameMode;
import com.demo.game.models.User;
import com.demo.game.network.messages.GameStartMessage; // Import the message
import com.demo.game.network.messages.SPoint2D;          // Import SPoint2D

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map; // Import Map

/**
 * A singleton manager to hold multiplayer game state information
 * passed between UI controllers and GameApp.
 */
public class MultiplayerManager {
    private static MultiplayerManager instance;

    private GameMode gameMode = GameMode.SINGLE_PLAYER;
    private List<String> lobbyPlayers = new ArrayList<>();
    private User localUser;

    // --- NEW FIELDS to store startup data ---
    private Map<Integer, SPoint2D> initialPositions = null;
    private List<String> initialUsernames = null;
    private boolean gameStartDataAvailable = false;
    // ------------------------------------------

    private MultiplayerManager() {}

    public static MultiplayerManager getInstance() {
        if (instance == null) {
            instance = new MultiplayerManager();
        }
        return instance;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public List<String> getLobbyPlayers() {
        // Return unmodifiable list for safety
        return Collections.unmodifiableList(lobbyPlayers);
    }

    public void setLobbyPlayers(List<String> lobbyPlayers) {
        // Create a new list to avoid external modification issues
        this.lobbyPlayers = new ArrayList<>(lobbyPlayers);
    }

    public User getLocalUser() {
        // Get the logged-in user from SceneManager
        if (localUser == null) {
            localUser = SceneManager.getInstance().getCurrentUser();
        }
        return localUser;
    }

    /** Stores the initial game start data received by the LobbyController. */
    public void setGameStartData(GameStartMessage msg) {
        if (msg != null) {
            this.initialPositions = msg.initialPositions;
            this.initialUsernames = msg.usernames;
            this.gameStartDataAvailable = true;
        } else {
            resetGameStartData(); // Clear if null message received
        }
    }

    /** Checks if game start data has been stored. */
    public boolean isGameStartDataAvailable() {
        return gameStartDataAvailable;
    }

    /** Retrieves the stored initial positions. Returns null if data not available. */
    public Map<Integer, SPoint2D> getInitialPositions() {
        return initialPositions;
    }

    /** Retrieves the stored initial usernames. Returns null if data not available. */
    public List<String> getInitialUsernames() {
        return initialUsernames;
    }

    /** Clears the stored game start data. Called by GameApp after consuming it. */
    public void resetGameStartData() {
        this.initialPositions = null;
        this.initialUsernames = null;
        this.gameStartDataAvailable = false;
    }


    /** Resets the manager state, except for the logged-in user. */
    public void reset() {
        gameMode = GameMode.SINGLE_PLAYER;
        lobbyPlayers.clear();
        resetGameStartData(); // Also reset game data
        // localUser is not reset, as they are still logged in
    }
}