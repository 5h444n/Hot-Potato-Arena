// File: com/demo/game/controllers/MultiplayerMenuController.java
package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
// Import our new network classes
import com.demo.game.network.GameClient;
import com.demo.game.network.GameServer;
import com.demo.game.network.messages.NetworkMessage; // Base message
import com.demo.game.GameMode;
import com.demo.game.ui.MultiplayerManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.function.Consumer;

public class MultiplayerMenuController {

    @FXML private TextField ipField;
    @FXML private Label statusLabel;
    @FXML private Button hostButton;
    @FXML private Button joinButton;

    // Keep track of our network instances
    private static GameServer gameServerInstance;
    private static GameClient gameClientInstance;
    private static Thread serverThread;
    private static Thread clientThread;

    @FXML
    private void handleHostGame() {
        // Ensure previous instances are stopped
        stopExistingConnections();

        statusLabel.setText("Starting server...");
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        MultiplayerManager.getInstance().setGameMode(GameMode.MULTIPLAYER_HOST);

        // Start our custom GameServer in a new thread
        gameServerInstance = new GameServer();
        serverThread = new Thread(gameServerInstance);
        // Allow JVM to exit if only server thread is running
        serverThread.setDaemon(true);
        serverThread.start();

        // Host also connects to its own server as a client
        handleJoinGameInternal("localhost");
    }

    @FXML
    private void handleJoinGame() {
        // Ensure previous instances are stopped
        stopExistingConnections();

        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            ip = "localhost";
        }

        statusLabel.setText("Connecting to " + ip + "...");
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        MultiplayerManager.getInstance().setGameMode(GameMode.MULTIPLAYER_CLIENT);
        handleJoinGameInternal(ip);
    }

    // Internal method to handle client connection logic
    private void handleJoinGameInternal(String ipAddress) {
        gameClientInstance = new GameClient(ipAddress, GameServer.PORT);

        // Define what happens when connection status changes
        gameClientInstance.setOnConnectionStatusChanged(isConnected -> {
            Platform.runLater(() -> {
                if (isConnected) {
                    statusLabel.setText("Connected!");
                    // Switch to lobby only AFTER connection is confirmed
                    switchToView("/com/demo/game/fxml/lobby.fxml");
                } else {
                    statusLabel.setText("Connection failed or lost.");
                    // Re-enable buttons on failure/disconnect
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                    // Clean up potentially failed client instance
                    gameClientInstance = null;
                    clientThread = null;
                }
            });
        });

        // Start the client connection attempt in a new thread
        clientThread = new Thread(gameClientInstance);
        clientThread.setDaemon(true);
        clientThread.start();
    }


    @FXML
    private void handleBack() {
        stopExistingConnections(); // Stop network threads when going back
        switchToView("/com/demo/game/fxml/mainmenu.fxml");
    }

    // Helper method to stop server/client threads if they are running
    public static void stopExistingConnections() {
        if (gameClientInstance != null) {
            gameClientInstance.stopClient();
            gameClientInstance = null;
            clientThread = null;
        }
        if (gameServerInstance != null) {
            gameServerInstance.stopServer();
            gameServerInstance = null;
            serverThread = null;
        }
    }

    // Static getters for LobbyController and GameApp to access
    public static GameServer getGameServerInstance() {
        return gameServerInstance;
    }

    public static GameClient getGameClientInstance() {
        return gameClientInstance;
    }


    private void switchToView(String fxmlFile) {
        try {
            // Ensure this runs on the JavaFX application thread
            Platform.runLater(() -> {
                try {
                    Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
                    if (FXGL.getSceneService().getCurrentScene() != null && FXGL.getSceneService().getCurrentScene().getRoot() != null) {
                        FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
                    } else {
                        System.err.println("Cannot switch view: FXGL scene not ready.");
                    }
                } catch (IOException e) {
                    System.err.println("Failed to load FXML for view switch: " + fxmlFile + " - " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Error queueing view switch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Empty methods to satisfy FXML, can be removed if not in FXML
    public void handleHost(ActionEvent event) {}
    public void handleJoin(ActionEvent event) {}
}