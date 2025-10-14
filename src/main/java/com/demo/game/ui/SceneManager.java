package com.demo.game.ui;

import com.demo.game.models.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneManager {
    private static SceneManager instance;
    private Stage stage;
    private User currentUser;
    private boolean isGameRunning = false;

    private SceneManager() {
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    // Add these methods to manage game state
    public boolean isGameRunning() {
        return isGameRunning;
    }

    public void setGameRunning(boolean running) {
        isGameRunning = running;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void loadScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/demo/game/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/demo/game/css/style.css")).toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Hot Potato Arena - " + title);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showLogin() {
        loadScene("login.fxml", "Login");
    }

    public void showRegister() {
        loadScene("register.fxml", "Register");
    }

    public void showMainMenu() {
        loadScene("mainmenu.fxml", "Main Menu");
    }

    public void showSettings() {
        loadScene("settings.fxml", "Settings");
    }

    public void showCredits() {
        loadScene("credits.fxml", "Credits");
    }


    public Stage getStage() {
        return stage;
    }
}
