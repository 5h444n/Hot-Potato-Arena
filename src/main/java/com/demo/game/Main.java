package com.demo.game;

import com.almasb.fxgl.app.GameApplication;
import com.demo.game.database.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        // Connect to the database
        DatabaseConnection.getInstance().connect();

        // Launch the entire application through FXGL
        GameApplication.launch(GameApp.class, args);
    }
}
