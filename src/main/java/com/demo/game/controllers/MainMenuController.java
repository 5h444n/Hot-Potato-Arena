// File: com/demo/game/controllers/MainMenuController.java
package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.DatabaseConnection;
import com.demo.game.models.User;
import com.demo.game.ui.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

public class MainMenuController {
    @FXML private Label welcomeLabel;
    @FXML private Label highScoreLabel;
    @FXML
    private Button singlePlayerButton;
    @FXML private Button multiplayerButton;
    @FXML private Button settingsButton;
    @FXML private Button creditsButton;
    @FXML private Button quitButton;

    @FXML
    public void initialize() {
        User currentUser = SceneManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getUsername() + "!");
            highScoreLabel.setText("High Score: " + currentUser.getHighScore());
        }
    }

    @FXML
    private void handleSinglePlayer() {
        // FIX: Switch to the game mode selection screen
        switchToView("/com/demo/game/fxml/gamemodeselection.fxml");
    }

    @FXML
    private void handleMultiplayer() {
        // FIX: Switch to the (soon to be uncommented) multiplayer menu
        switchToView("/com/demo/game/fxml/multiplayermenu.fxml");
    }

    // **********************************
    // ** THIS IS THE NEW, FIXED METHOD **
    // **********************************
    @FXML
    private void handleProfile() {
        // Switch to the profile view
        switchToView("/com/demo/game/fxml/profile.fxml");
    }

    @FXML
    private void handleSettings() {
        switchToView("/com/demo/game/fxml/settings.fxml");
    }

    @FXML
    private void handleCredits() {
        switchToView("/com/demo/game/fxml/credits.fxml");
    }

    private void switchToView(String fxmlFile) {
        try {
            // Get the URL for the resource
            java.net.URL resourceUrl = getClass().getResource(fxmlFile);

            // Check if the resource was found
            if (resourceUrl == null) {
                System.err.println("!!! Critical Error: FXML resource not found at path: " + fxmlFile);
                System.err.println("!!! Please ensure the file exists in src/main/resources" + fxmlFile);
                // Optionally show an alert to the user
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("UI Error");
                    alert.setHeaderText("Cannot Load View");
                    alert.setContentText("Failed to find the required UI file:\n" + fxmlFile + "\nPlease check application resources.");
                    alert.showAndWait();
                });
                return; // Stop execution if resource is missing
            }

            // Load the FXML using the found URL
            Parent newRoot = FXMLLoader.load(resourceUrl);

            // Ensure FXGL scene is ready before modifying it
            if (FXGL.getSceneService().getCurrentScene() != null && FXGL.getSceneService().getCurrentScene().getRoot() != null) {
                // Use Platform.runLater just in case, although FXGL usually handles this
                Platform.runLater(() -> FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot));
            } else {
                System.err.println("Cannot switch view: FXGL scene service or current scene root is null.");
            }

        } catch (IOException e) {
            System.err.println("Failed to load FXML file: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace(); // Print stack trace for IO errors
        } catch (IllegalStateException e) {
            System.err.println("Error during view switch (likely scene state issue): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuit() {
        System.out.println("Quit button clicked. Disconnecting database...");

        // 1. Manually disconnect the database first.
        DatabaseConnection.getInstance().disconnect();

        // 2. Then, tell FXGL to exit the application.
        FXGL.getGameController().exit();
    }
}