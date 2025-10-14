package com.demo.game;

import com.demo.game.database.DatabaseConnection;
import com.demo.game.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize database connection
        DatabaseConnection.getInstance().connect();

        // Initialize SceneManager
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.setStage(primaryStage);

        // Set window properties
        primaryStage.setTitle("Hot Potato Arena");
        primaryStage.setResizable(false);

        // Load login scene
        sceneManager.showLogin();

        primaryStage.show();
    }

    @Override
    public void stop() {
        // Close database connection
        DatabaseConnection.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
