package com.demo.game.scenes;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class LoginScene extends FXGLMenu {
    public LoginScene() {
        super(MenuType.MAIN_MENU);

        try {
            // Load your login.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/demo/game/fxml/login.fxml"));
            Parent fxmlRoot = loader.load();

            // Add the loaded FXML content to this scene
            getContentRoot().getChildren().add(fxmlRoot);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load login.fxml: " + e.getMessage(), e);
        }
    }
}
