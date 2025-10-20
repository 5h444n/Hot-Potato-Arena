// File: com/demo/game/controllers/LobbyController.java
package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.network.GameClient;
import com.demo.game.network.GameServer;
import com.demo.game.network.messages.*;
import com.demo.game.GameMode;
import com.demo.game.models.User;
import com.demo.game.ui.MultiplayerManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
    private GameClient gameClient;
    private GameServer gameServer;


    @FXML
    public void initialize() {
        gameMode = MultiplayerManager.getInstance().getGameMode();
        localUser = MultiplayerManager.getInstance().getLocalUser();
        gameClient = MultiplayerMenuController.getGameClientInstance();
        gameServer = MultiplayerMenuController.getGameServerInstance();

        if (gameClient == null) {
            System.err.println("Lobby Error: GameClient is null. Returning to menu.");
            Platform.runLater(() -> switchToView("/com/demo/game/fxml/multiplayermenu.fxml"));
            return;
        }

        startGameButton.setVisible(gameMode == GameMode.MULTIPLAYER_HOST);
        gameClient.setOnMessageReceived(this::handleNetworkMessage);

        if (gameMode == GameMode.MULTIPLAYER_HOST && gameServer != null) {
            updatePlayerList(gameServer.getCurrentPlayerUsernames());
        } else {
            playerListView.setItems(FXCollections.observableArrayList("Connecting..."));
        }
    }

    /**
     * Handles messages while in the lobby. Updates player list or stores game start data.
     */
    private void handleNetworkMessage(NetworkMessage message) {
        Platform.runLater(() -> {
            if (message instanceof LobbyUpdateMessage) {
                updatePlayerList(((LobbyUpdateMessage) message).playerUsernames);

            } else if (message instanceof GameStartMessage) {
                System.out.println("LobbyController: Received GameStartMessage.");

                // --- !!! THE FIX IS HERE !!! ---
                // 1. Store the message data in the MultiplayerManager
                MultiplayerManager.getInstance().setGameStartData((GameStartMessage) message);
                // ---------------------------------

                // 2. Remove THIS controller's message handler
                if (gameClient != null) {
                    gameClient.setOnMessageReceived(null);
                }

                // 3. Tell FXGL to transition to the game scene
                System.out.println("LobbyController: Starting game scene...");
                FXGL.getGameController().startNewGame();
            }
        });
    }

    private void updatePlayerList(List<String> usernames) {
        playerListView.setItems(FXCollections.observableArrayList(usernames));
        MultiplayerManager.getInstance().setLobbyPlayers(new ArrayList<>(usernames));
    }


    @FXML
    private void handleStartGame() {
        if (gameMode == GameMode.MULTIPLAYER_HOST && gameServer != null) {
            System.out.println("Host clicked Start Game.");
            gameServer.submitTask(gameServer::startGame);
        }
    }

    @FXML
    private void handleDisconnect() {
        MultiplayerMenuController.stopExistingConnections();
        switchToView("/com/demo/game/fxml/multiplayermenu.fxml");
    }

    private void switchToView(String fxmlFile) {
        if (gameClient != null) {
            gameClient.setOnMessageReceived(null); // Ensure handler is removed
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