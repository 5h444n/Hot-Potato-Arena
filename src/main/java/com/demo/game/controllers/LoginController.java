// File: com/demo/game/controllers/LoginController.java
package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.DatabaseConnection;
import com.demo.game.database.UserDAO;
import com.demo.game.models.User;
import com.demo.game.ui.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        User user = userDAO.loginUser(username, password);

        if (user != null) {
            // Login successful
            statusLabel.setVisible(false);
            System.out.println("Login successful: " + user.getUsername());
            // Store user in the singleton manager
            SceneManager.getInstance().setCurrentUser(user);

            // Switch to the main menu FXML
            switchToView("/com/demo/game/fxml/mainmenu.fxml");

        } else {
            // Login failed
            showError("Invalid username or password.");
        }
    }

    // **********************************
    // ** THIS IS THE NEW, FIXED METHOD **
    // **********************************
    @FXML
    private void handleShowRegister() {
        // Switch to the registration view
        switchToView("/com/demo/game/fxml/register.fxml");
    }

    @FXML
    private void handleQuit() {
        System.out.println("Quit button clicked. Disconnecting database...");
        DatabaseConnection.getInstance().disconnect();
        FXGL.getGameController().exit();
    }

    private void switchToView(String fxmlFile) {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
            // Ensure we are on the JavaFX thread for scene graph modifications
            Platform.runLater(() -> {
                FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
            });
        } catch (IOException e) {
            System.err.println("Failed to switch view: " + e.getMessage());
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
    }
}