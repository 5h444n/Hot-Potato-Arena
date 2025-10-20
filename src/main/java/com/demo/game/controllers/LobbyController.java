// File: com/demo/game/controllers/LobbyController.java
package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
// Import our network classes
import com.demo.game.network.GameClient;
import com.demo.game.network.GameServer;
import com.demo.game.network.messages.*; // Import all messages
import com.demo.game.GameMode;
import com.demo.game.models.User;
import com.demo.game.ui.MultiplayerManager;
import javafx.application.Platform;
import javafx.collections.FXCollections; // Need this for ObservableList
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LobbyController {
    @FXML
    private ListView<String> playerListView;
    @FXML
    private Button startGameButton;

    private GameMode gameMode;
    private User localUser;
    private GameClient gameClient; // Client connection
    private GameServer gameServer; // Server instance (only if host)


    @FXML
    public void initialize() {
        gameMode = MultiplayerManager.getInstance().getGameMode();
        localUser = MultiplayerManager.getInstance().getLocalUser();
        // Get the active client connection
        gameClient = MultiplayerMenuController.getGameClientInstance();
        // Get server if we are host
        gameServer = MultiplayerMenuController.getGameServerInstance();

        if (gameClient == null) {
            System.err.println("Lobby Error: GameClient is null. Returning to menu.");
            Platform.runLater(() -> switchToView("/com/demo/game/fxml/multiplayermenu.fxml"));
            return;
        }

        // Only the host can see the "Start Game" button
        startGameButton.setVisible(gameMode == GameMode.MULTIPLAYER_HOST);

        // --- Set up message listener ---
        // This controller now handles messages from the client
        gameClient.setOnMessageReceived(this::handleNetworkMessage);

        // --- Initial Lobby State ---
        if (gameMode == GameMode.MULTIPLAYER_HOST && gameServer != null) {
            // Host: Display initial list from server instance
            updatePlayerList(gameServer.getCurrentPlayerUsernames());
        } else {
            // Client: Will receive LobbyUpdateMessage soon
            playerListView.setItems(FXCollections.observableArrayList("Connecting..."));
        }
    }

    // Central message handler for messages received by the GameClient
    private void handleNetworkMessage(NetworkMessage message) {
        // Ensure UI updates happen on the JavaFX Application Thread
        // (GameClient already wraps this, but it's good practice)
        Platform.runLater(() -> {
            if (message instanceof LobbyUpdateMessage) {
                updatePlayerList(((LobbyUpdateMessage) message).playerUsernames);
            } else if (message instanceof GameStartMessage) {
                // Game is starting!
                System.out.println("Received GameStartMessage");
                // Save final player list
                MultiplayerManager.getInstance().setLobbyPlayers(((GameStartMessage) message).usernames);
                // Tell FXGL to start the game scene
                FXGL.getGameController().startNewGame();
            }
        });
    }

    private void updatePlayerList(List<String> usernames) {
        playerListView.setItems(FXCollections.observableArrayList(usernames));
        // Update manager too
        MultiplayerManager.getInstance().setLobbyPlayers(new ArrayList<>(usernames));
    }


    @FXML
    private void handleStartGame() {
        // Only the host can start
        if (gameMode == GameMode.MULTIPLAYER_HOST && gameServer != null) {
            System.out.println("Host clicked Start Game.");
            // Tell the server instance to start the game logic
            gameServer.submitTask(gameServer::startGame);
        }
    }

    @FXML
    private void handleDisconnect() {
        // Stop server and/or client instances
        MultiplayerMenuController.stopExistingConnections();
        switchToView("/com/demo/game/fxml/multiplayermenu.fxml");
    }

    private void switchToView(String fxmlFile) {
        // Ensure callback is removed before switching view
        if (gameClient != null) {
            gameClient.setOnMessageReceived(null);
        }

        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
            Platform.runLater(() -> {
                if (FXGL.getSceneService().getCurrentScene() != null && FXGL.getSceneService().getCurrentScene().getRoot() != null) {
                    FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
                } else {
                    System.err.println("Cannot switch view: FXGL scene not ready.");
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}