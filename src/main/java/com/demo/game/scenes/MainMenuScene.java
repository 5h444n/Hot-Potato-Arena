package com.demo.game.scenes;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.FXGLScene;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class MainMenuScene extends FXGLMenu {
    public MainMenuScene() {
        super(MenuType.MAIN_MENU);

        try {
            // 1. Load your FXML file like you did before
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/demo/game/fxml/mainmenu.fxml"));
            Parent fxmlRoot = loader.load();

            // 2. Add the loaded FXML content to this menu scene
            getContentRoot().getChildren().add(fxmlRoot);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for Main Menu: " + e.getMessage(), e);
        }
    }
}
