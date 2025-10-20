package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public class GameModeSelectionController {

    @FXML
    private void handleCampaign() {
        // This starts the "Campaign" mode (your existing game)
        // We will tell GameApp to load the campaign.
        FXGL.getWorldProperties().setValue("gameMode", "campaign");
        FXGL.getGameController().startNewGame();
    }

    @FXML
    private void handleDeathmatch() {
        // This starts the "Deathmatch" mode
        // We will tell GameApp to load the deathmatch.
        FXGL.getWorldProperties().setValue("gameMode", "deathmatch");
        FXGL.getGameController().startNewGame();
    }

    @FXML
    private void handleLeaderboard() {
        switchToView("/com/demo/game/fxml/leaderboard.fxml");
    }

    @FXML
    private void handleBack() {
        switchToView("/com/demo/game/fxml/mainmenu.fxml");
    }

    private void switchToView(String fxmlFile) {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
            FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
        } catch (IOException e) {
            System.err.println("Failed to switch view: " + e.getMessage());
        }
    }
}