//package com.demo.game.ui;
//
//import com.almasb.fxgl.scene.SubScene;
//import com.almasb.fxgl.dsl.FXGL;
//import com.almasb.fxgl.scene.SubScene;
//import javafx.scene.control.Button;
//import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Rectangle;
//import javafx.scene.text.Text;
//
//public class PauseMenu extends SubScene {
//    public PauseMenu() {
//        // Dark background
//        Rectangle bg = new Rectangle(FXGL.getAppWidth(), FXGL.getAppHeight(), Color.color(0, 0, 0, 0.5));
//
//        // Paused text
//        Text pausedText = FXGL.getUIFactoryService().newText("PAUSED", Color.WHITE, 48);
//        pausedText.setTranslateX(FXGL.getAppWidth() / 2.0 - 100);
//        pausedText.setTranslateY(FXGL.getAppHeight() / 2.0 - 100);
//
//        // Resume button
//        Button resumeButton = new Button("Resume");
//        resumeButton.setOnAction(e -> FXGL.getGameController().popSubScene());
//
//        // Quit to Main Menu button
//        Button quitButton = new Button("Quit to Main Menu");
//        quitButton.setOnAction(e -> {
//            FXGL.getGameController().popSubScene(); // Close pause menu
//            FXGL.getGameController().gotoMainMenu(); // Go to menu
//        });
//
//        VBox buttonBox = new VBox(10, resumeButton, quitButton);
//        buttonBox.setTranslateX(FXGL.getAppWidth() / 2.0 - 75);
//        buttonBox.setTranslateY(FXGL.getAppHeight() / 2.0);
//
//        getContentRoot().getChildren().addAll(bg, pausedText, buttonBox);
//    }
//}
