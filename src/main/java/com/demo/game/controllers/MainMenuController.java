package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.DatabaseConnection;
import com.demo.game.models.User;
import com.demo.game.ui.SceneManager;
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
        // THIS IS THE NEW LOGIC
        // It tells the running "Movie Theater" to switch from the lobby to the cinema.
        FXGL.getGameController().startNewGame();
    }

    @FXML
    private void handleMultiplayer() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Multiplayer Mode");
        alert.setContentText("Multiplayer mode is coming soon! Stay tuned for updates.");
        alert.showAndWait();
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
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
            FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
        } catch (IOException e) {
            System.err.println("Failed to switch view: " + e.getMessage());
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
