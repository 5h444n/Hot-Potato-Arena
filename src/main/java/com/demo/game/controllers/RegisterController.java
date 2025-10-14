package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.UserDAO;
import com.demo.game.ui.SceneManager;
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

        // Enable register button only when terms are accepted
        registerButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Reset messages
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        // Validation
        if (!validateInput(username, email, password, confirmPassword)) {
            return;
        }

        // Attempt registration
        boolean registered = userDAO.registerUser(username, email, password);

        if (registered) {
            showSuccess("Registration successful! Redirecting to login...");
            // Delay before redirecting to log in
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2)
            );
            pause.setOnFinished(e -> switchToView("/com/demo/game/fxml/login.fxml"));
            pause.play();
        } else {
            showError("Registration failed. Username or email may already exist.");
        }
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        // Check empty fields
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required");
            return false;
        }

        // Validate username length
        if (username.length() < 3 || username.length() > 20) {
            showError("Username must be between 3 and 20 characters");
            return false;
        }

        // Validate username format (alphanumeric and underscore only)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showError("Username can only contain letters, numbers, and underscores");
            return false;
        }

        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Invalid email format");
            return false;
        }

        // Validate password strength
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }

        // Check password match
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);
        double progress = strength / 4.0;
        passwordStrengthBar.setProgress(progress);

        // Update color and label based on strength
        if (strength == 0) {
            passwordStrengthLabel.setText("");
            passwordStrengthBar.setStyle("-fx-accent: #E74C3C;");
        } else if (strength == 1) {
            passwordStrengthLabel.setText("Weak");
            passwordStrengthLabel.setTextFill(Color.RED);
            passwordStrengthBar.setStyle("-fx-accent: #E74C3C;");
        } else if (strength == 2) {
            passwordStrengthLabel.setText("Fair");
            passwordStrengthLabel.setTextFill(Color.ORANGE);
            passwordStrengthBar.setStyle("-fx-accent: #F39C12;");
        } else if (strength == 3) {
            passwordStrengthLabel.setText("Good");
            passwordStrengthLabel.setTextFill(Color.YELLOWGREEN);
            passwordStrengthBar.setStyle("-fx-accent: #F1C40F;");
        } else {
            passwordStrengthLabel.setText("Strong");
            passwordStrengthLabel.setTextFill(Color.GREEN);
            passwordStrengthBar.setStyle("-fx-accent: #27AE60;");
        }
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;
        if (password.length() >= 6) strength++;
        if (password.length() >= 10) strength++;
        if (password.matches(".*[A-Z].*")) strength++; // Has uppercase
        if (password.matches(".*[0-9].*")) strength++; // Has number
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength++; // Has special char
        return Math.min(strength, 4);
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
