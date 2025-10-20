package com.demo.game.ui;

import com.demo.game.GameMode;
import com.demo.game.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A singleton manager to hold multiplayer game state *before* the game starts,
 * and to pass information from the lobby controllers to GameApp.
 */
public class MultiplayerManager {
    private static MultiplayerManager instance;

    private GameMode gameMode = GameMode.SINGLE_PLAYER;
    private List<String> lobbyPlayers = new ArrayList<>();
    private User localUser;

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
        return Collections.unmodifiableList(lobbyPlayers);
    }

    public void setLobbyPlayers(List<String> lobbyPlayers) {
        this.lobbyPlayers = lobbyPlayers;
    }

    public User getLocalUser() {
        // Get the logged-in user from SceneManager
        if (localUser == null) {
            localUser = SceneManager.getInstance().getCurrentUser();
        }
        return localUser;
    }

    public void reset() {
        gameMode = GameMode.SINGLE_PLAYER;
        lobbyPlayers.clear();
        // localUser is not reset, as they are still logged in
    }
}