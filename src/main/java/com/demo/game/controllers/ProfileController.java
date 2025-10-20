package com.demo.game.controllers;

import com.almasb.fxgl.dsl.FXGL;
import com.demo.game.database.UserDAO;
import com.demo.game.models.User;
import com.demo.game.ui.SceneManager;
// Import the password utils for verification
import com.demo.game.utils.PasswordUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
// We don't need to import BCrypt directly anymore
// import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

public class ProfileController {

    @FXML private ImageView profileImageView;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private PasswordField currentPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private UserDAO userDAO = new UserDAO();
    private User currentUser;
    private File selectedPhoto;

    @FXML
    public void initialize() {
        currentUser = SceneManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            handleBack(); // Should not be here if not logged in
            return;
        }

        usernameField.setText(currentUser.getUsername());
        loadProfileImage();
    }

    private void loadProfileImage() {
        // This method would need to be implemented
        // For now, it just loads a default
        try (InputStream is = getClass().getResourceAsStream("/assets/textures/player.png")) {
            if (is != null) {
                profileImageView.setImage(new Image(is));
            }
        } catch (IOException e) {
            System.err.println("Failed to load default profile image: " + e.getMessage());
        }
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file != null) {
            selectedPhoto = file;
            try (InputStream is = new FileInputStream(selectedPhoto)) {
                profileImageView.setImage(new Image(is));
            } catch (IOException e) {
                showError("Failed to load image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpdateProfile() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        boolean changesMade = false;

        // 1. Update username
        String newUsername = usernameField.getText().trim();
        if (!newUsername.isEmpty() && !newUsername.equals(currentUser.getUsername())) {
            if (userDAO.updateUsername(currentUser.getId(), newUsername)) {
                currentUser.setUsername(newUsername); // Update local user object
                changesMade = true;
            } else {
                showError("Username update failed (it might be taken).");
                return;
            }
        }

        // 2. Update password
        String newPassword = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String currentPassword = currentPasswordField.getText();

        if (!newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                showError("New passwords do not match.");
                return;
            }

            // We must verify their *current* password
            // We need to get the user's hash from the DB
            User freshUserData = userDAO.loginUser(currentUser.getUsername(), currentPassword);
            if (freshUserData == null) {
                showError("Current password incorrect.");
                return;
            }

            // Validate new password
            PasswordUtils.ValidationResult validation = PasswordUtils.validatePassword(newPassword);
            if (!validation.isValid()) {
                showError(validation.getMessage());
                return;
            }

            // All checks passed, update the password
            if (userDAO.updatePassword(currentUser.getId(), newPassword)) {
                changesMade = true;
                // Clear password fields
                passwordField.clear();
                confirmPasswordField.clear();
                currentPasswordField.clear();
            } else {
                showError("Password update failed.");
                return;
            }
        }

        // 3. Try to update photo
        if (selectedPhoto != null) {
            try (FileInputStream fis = new FileInputStream(selectedPhoto)) {
                if (userDAO.updateProfilePicture(currentUser.getId(), fis, selectedPhoto.length())) {
                    changesMade = true;
                }
            } catch (IOException e) {
                showError("Failed to save photo: " + e.getMessage());
                return;
            }
        }

        if (changesMade) {
            showSuccess("Profile updated successfully!");
        } else {
            showError("No changes were made.");
        }
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