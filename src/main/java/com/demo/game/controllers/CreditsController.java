package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.ui.SceneManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URI;
import java.awt.Desktop;

public class CreditsController {
    @FXML
    private ScrollPane scrollPane;
    @FXML private VBox creditsContent;
    @FXML private Button backButton;
    @FXML private Button autoScrollButton;
    @FXML private Label versionLabel;
    @FXML private Hyperlink githubLink;
    @FXML private Hyperlink websiteLink;

    private Timeline autoScrollTimeline;
    private boolean isAutoScrolling = false;

    @FXML
    public void initialize() {
        // Set version
        versionLabel.setText("Version 1.0.0");

        // Add fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), creditsContent);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Setup auto-scroll
        setupAutoScroll();

        // Add hover effects
        addHoverEffects();
    }

    private void setupAutoScroll() {
        autoScrollTimeline = new Timeline(
                new KeyFrame(Duration.millis(50), e -> {
                    double currentValue = scrollPane.getVvalue();
                    if (currentValue < 1.0) {
                        scrollPane.setVvalue(currentValue + 0.005);
                    } else {
                        stopAutoScroll();
                    }
                })
        );
        autoScrollTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    @FXML
    private void handleAutoScroll() {
        if (isAutoScrolling) {
            stopAutoScroll();
        } else {
            startAutoScroll();
        }
    }

    private void startAutoScroll() {
        isAutoScrolling = true;
        autoScrollButton.setText("Stop Auto-Scroll");
        scrollPane.setVvalue(0); // Reset to top
        autoScrollTimeline.play();
    }

    private void stopAutoScroll() {
        isAutoScrolling = false;
        autoScrollButton.setText("Auto-Scroll");
        autoScrollTimeline.stop();
    }

    @FXML
    private void handleGitHub() {
        openWebpage("https://github.com/yourusername/hot-potato-arena");
    }

    @FXML
    private void handleWebsite() {
        openWebpage("https://www.yourwebsite.com");
    }

    private void openWebpage(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("Failed to open webpage: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        if (isAutoScrolling) {
            stopAutoScroll();
        }
        // Switch the view back to the main menu
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

    private void addHoverEffects() {
        // Add scale animation on hover for interactive elements
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200));
        scaleIn.setToX(1.1);
        scaleIn.setToY(1.1);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200));
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        backButton.setOnMouseEntered(e -> {
            scaleIn.setNode(backButton);
            scaleIn.play();
        });

        backButton.setOnMouseExited(e -> {
            scaleOut.setNode(backButton);
            scaleOut.play();
        });
    }
}
