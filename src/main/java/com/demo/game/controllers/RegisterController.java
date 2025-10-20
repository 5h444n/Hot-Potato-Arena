package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.UserDAO;
// Import the new utility
import com.demo.game.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.regex.Pattern;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private Button registerButton;
    @FXML
    private Button backButton;
    @FXML
    private CheckBox termsCheckBox;
    @FXML
    private ProgressBar passwordStrengthBar;
    @FXML
    private Label passwordStrengthLabel;

    private UserDAO userDAO = new UserDAO();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        passwordStrengthBar.setProgress(0);
        passwordStrengthLabel.setText("");

        // Add password strength checker
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            updatePasswordStrength(newText);
        });

        // Enable register button only when terms are checked
        registerButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Basic validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Invalid email format.");
            return;
        }

        // 2. Use PasswordUtils for validation
        PasswordUtils.ValidationResult validation = PasswordUtils.validatePassword(password);
        if (!validation.isValid()) {
            showError(validation.getMessage());
            return;
        }

        // 3. Try to register user
        try {
            boolean success = userDAO.registerUser(username, email, password);
            if (success) {
                showSuccess("Registration successful! You can now log in.");
                // Clear fields after success
                usernameField.clear();
                emailField.clear();
                passwordField.clear();
                confirmPasswordField.clear();
                termsCheckBox.setSelected(false);
            } else {
                showError("Registration failed. Username may already be taken.");
            }
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
        }
    }

    // This method now only updates the UI, it doesn't perform validation.
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthBar.setProgress(0);
            passwordStrengthLabel.setText("");
            return;
        }

        // Simple length-based UI feedback
        double strength = 0;
        if (password.length() >= 6) strength += 0.25;
        if (password.matches(".*[A-Z].*")) strength += 0.25;
        if (password.matches(".*[0-9].*")) strength += 0.25;
        if (password.matches(".*[!@#$%^&*(),.?\\\":{}|<>].*")) strength += 0.25;

        passwordStrengthBar.setProgress(strength);

        if (strength < 0.5) {
            passwordStrengthLabel.setText("Weak");
            passwordStrengthLabel.setTextFill(Color.RED);
            passwordStrengthBar.setStyle("-fx-accent: #E74C3C;");
        } else if (strength < 0.75) {
            passwordStrengthLabel.setText("Medium");
            passwordStrengthLabel.setTextFill(Color.ORANGE);
            passwordStrengthBar.setStyle("-fx-accent: #F39C12;");
        } else {
            passwordStrengthLabel.setText("Strong");
            passwordStrengthLabel.setTextFill(Color.GREEN);
            passwordStrengthBar.setStyle("-fx-accent: #27AE60;");
        }
    }

    @FXML
    private void handleBack() {
        // Switch the view back to the login screen
        switchToView("/com/demo/game/fxml/login.fxml");
    }

    private void switchToView(String fxmlFile) {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlFile));
            FXGL.getSceneService().getCurrentScene().getRoot().getChildren().setAll(newRoot);
        } catch (IOException e) {
            System.err.println("Failed to switch view: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
}