package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.DatabaseConnection;
import com.demo.game.models.User;
import com.demo.game.ui.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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
        SceneManager.getInstance().showSettings();
    }

    @FXML
    private void handleCredits() {
        SceneManager.getInstance().showCredits();
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
