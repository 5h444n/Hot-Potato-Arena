package com.demo.game.controllers;

import com.demo.game.database.DatabaseConnection;
import com.demo.game.models.User;
import com.demo.game.ui.SceneManager;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsController {
    @FXML
    private CheckBox soundEnabledCheckBox;
    @FXML private Slider musicVolumeSlider;
    @FXML private Slider sfxVolumeSlider;
    @FXML private Label musicVolumeLabel;
    @FXML private Label sfxVolumeLabel;
    @FXML private CheckBox fullscreenCheckBox;
    @FXML private ComboBox<String> resolutionComboBox;
    @FXML private ComboBox<String> difficultyComboBox;
    @FXML private Button saveButton;
    @FXML private Button resetButton;
    @FXML private Button backButton;
    @FXML private Label statusLabel;

    // Controls section
    @FXML private TextField moveLeftKey;
    @FXML private TextField moveRightKey;
    @FXML private TextField moveUpKey;
    @FXML private TextField moveDownKey;
    @FXML private TextField passBombKey;
    @FXML private Button resetControlsButton;

    private Connection connection;
    private User currentUser;

    @FXML
    public void initialize() {
        connection = DatabaseConnection.getInstance().getConnection();
        currentUser = SceneManager.getInstance().getCurrentUser();

        // Setup UI bindings
        setupVolumeSliders();
        setupComboBoxes();
        setupKeyBindings();

        // Load current settings
        loadUserSettings();

        statusLabel.setVisible(false);
    }

    private void setupVolumeSliders() {
        // Bind labels to slider values
        musicVolumeLabel.textProperty().bind(
                Bindings.format("%.0f%%", musicVolumeSlider.valueProperty())
        );
        sfxVolumeLabel.textProperty().bind(
                Bindings.format("%.0f%%", sfxVolumeSlider.valueProperty())
        );

        // Set initial values
        musicVolumeSlider.setValue(50);
        sfxVolumeSlider.setValue(50);
    }

    private void setupComboBoxes() {
        // Add resolution options
        resolutionComboBox.getItems().addAll(
                "800x600",
                "1024x768",
                "1280x720",
                "1920x1080",
                "2560x1440"
        );
        resolutionComboBox.setValue("800x600");

        // Add difficulty options
        difficultyComboBox.getItems().addAll(
                "Easy",
                "Normal",
                "Hard",
                "Extreme"
        );
        difficultyComboBox.setValue("Normal");
    }

    private void setupKeyBindings() {
        // Set default key bindings
        moveLeftKey.setText("A");
        moveRightKey.setText("D");
        moveUpKey.setText("W");
        moveDownKey.setText("S");
        passBombKey.setText("SPACE");

        // Make fields non-editable but clickable for key capture
        moveLeftKey.setEditable(false);
        moveRightKey.setEditable(false);
        moveUpKey.setEditable(false);
        moveDownKey.setEditable(false);
        passBombKey.setEditable(false);

        // Add click listeners to capture keys
        addKeyCapture(moveLeftKey);
        addKeyCapture(moveRightKey);
        addKeyCapture(moveUpKey);
        addKeyCapture(moveDownKey);
        addKeyCapture(passBombKey);
    }

    private void addKeyCapture(TextField field) {
        field.setOnMouseClicked(e -> {
            field.setText("Press any key...");
            field.setOnKeyPressed(keyEvent -> {
                field.setText(keyEvent.getCode().toString());
                field.setOnKeyPressed(null);
            });
        });
    }

    private void loadUserSettings() {
        if (currentUser == null) return;

        String sql = "SELECT * FROM user_settings WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, currentUser.getId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                soundEnabledCheckBox.setSelected(rs.getBoolean("sound_enabled"));
                musicVolumeSlider.setValue(rs.getDouble("music_volume") * 100);
                sfxVolumeSlider.setValue(rs.getDouble("sfx_volume") * 100);
                fullscreenCheckBox.setSelected(rs.getBoolean("fullscreen"));

                // Load additional settings if they exist in your extended schema
                String resolution = rs.getString("resolution");
                if (resolution != null) {
                    resolutionComboBox.setValue(resolution);
                }

                String difficulty = rs.getString("difficulty");
                if (difficulty != null) {
                    difficultyComboBox.setValue(difficulty);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (currentUser == null) return;

        String sql = "UPDATE user_settings SET sound_enabled = ?, music_volume = ?, " +
                "sfx_volume = ?, fullscreen = ? WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, soundEnabledCheckBox.isSelected());
            stmt.setDouble(2, musicVolumeSlider.getValue() / 100.0);
            stmt.setDouble(3, sfxVolumeSlider.getValue() / 100.0);
            stmt.setBoolean(4, fullscreenCheckBox.isSelected());
            stmt.setInt(5, currentUser.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                showStatus("Settings saved successfully!", "success");
            } else {
                showStatus("Failed to save settings", "error");
            }
        } catch (SQLException e) {
            System.err.println("Error saving settings: " + e.getMessage());
            showStatus("Error saving settings", "error");
        }

        // Save key bindings to a config file or preferences
        saveKeyBindings();
    }

    private void saveKeyBindings() {
        // In a real application, you would save these to a properties file or database
        // For now, just print them
        System.out.println("Saving key bindings:");
        System.out.println("Move Left: " + moveLeftKey.getText());
        System.out.println("Move Right: " + moveRightKey.getText());
        System.out.println("Move Up: " + moveUpKey.getText());
        System.out.println("Move Down: " + moveDownKey.getText());
        System.out.println("Pass Bomb: " + passBombKey.getText());
    }

    @FXML
    private void handleReset() {
        // Reset to default values
        soundEnabledCheckBox.setSelected(true);
        musicVolumeSlider.setValue(50);
        sfxVolumeSlider.setValue(50);
        fullscreenCheckBox.setSelected(false);
        resolutionComboBox.setValue("800x600");
        difficultyComboBox.setValue("Normal");

        showStatus("Settings reset to defaults", "info");
    }

    @FXML
    private void handleResetControls() {
        moveLeftKey.setText("A");
        moveRightKey.setText("D");
        moveUpKey.setText("W");
        moveDownKey.setText("S");
        passBombKey.setText("SPACE");

        showStatus("Controls reset to defaults", "info");
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().showMainMenu();
    }

    private void showStatus(String message, String type) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);

        // Style based on type
        switch (type) {
            case "success":
                statusLabel.setStyle("-fx-text-fill: #27AE60;");
                break;
            case "error":
                statusLabel.setStyle("-fx-text-fill: #E74C3C;");
                break;
            case "info":
                statusLabel.setStyle("-fx-text-fill: #3498DB;");
                break;
        }

        // Auto-hide after 3 seconds
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3)
        );
        pause.setOnFinished(e -> statusLabel.setVisible(false));
        pause.play();
    }
}
