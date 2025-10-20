package com.demo.game.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Map;

import com.demo.game.models.User;
// Import our centralized password utility
import com.demo.game.utils.PasswordUtils;
// Remove the direct BCrypt import
// import org.mindrot.jbcrypt.BCrypt;


public class UserDAO {
    private Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public boolean registerUser(String username, String email, String password) {
        String sql = "INSERT INTO hot_potato.users (username, email, password_hash) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Use PasswordUtils for consistent, secure hashing
            String hashedPassword = PasswordUtils.hashPassword(password);

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);

            int rowsAffected = stmt.executeUpdate();

            // Create default settings for new user
            if (rowsAffected > 0) {
                // Get the newly generated user ID
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newUserId = generatedKeys.getInt(1);
                        createDefaultSettings(newUserId);
                    }
                }
            }

            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    // Updated to take user ID directly for reliability
    private void createDefaultSettings(int userId) {
        String sql = "INSERT INTO hot_potato.user_settings (user_id) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to create default settings: " + e.getMessage());
        }
    }

    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM hot_potato.users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // Use PasswordUtils to securely verify
                    if (PasswordUtils.verifyPassword(password, storedHash)) {
                        // Password is correct
                        User user = new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("email")
                        );
                        user.setHighScore(rs.getInt("high_score"));
                        user.setSinglePlayerPlayed(rs.getInt("single_player_played"));

                        // Update last login
                        updateLastLogin(user.getId());

                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Login failed: " + e.getMessage());
        }

        return null;
    }

    private void updateLastLogin(int userId) {
        String sql = "UPDATE hot_potato.users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update last login: " + e.getMessage());
        }
    }

    public void updateHighScore(int userId, int score) {
        String sql = "UPDATE hot_potato.users SET high_score = GREATEST(high_score, ?) WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, score);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update high score: " + e.getMessage());
        }
    }

    // --- NEWLY ADDED METHODS ---

    /**
     * This method was being called by LeaderboardController but was missing.
     * It retrieves the top 10 players by high_score.
     */
    public List<Map.Entry<String, Integer>> getDeathmatchLeaderboard() {
        List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>();
        // For now, "Deathmatch" leaderboard will just be the 'high_score' column.
        // We can change this later to a 'deathmatch_wins' column if we add one.
        String sql = "SELECT username, high_score FROM hot_potato.users ORDER BY high_score DESC LIMIT 10";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                int score = rs.getInt("high_score");
                leaderboard.add(new AbstractMap.SimpleEntry<>(username, score));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }

    /**
     * Updates the user's password using PasswordUtils.
     */
    public boolean updatePassword(int userId, String newPassword) {
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        String sql = "UPDATE hot_potato.users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the user's username.
     */
    public boolean updateUsername(int userId, String newUsername) {
        String sql = "UPDATE hot_potato.users SET username = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newUsername);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update username: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the user's profile picture.
     */
    public boolean updateProfilePicture(int userId, InputStream photoStream, long photoLength) {
        String sql = "UPDATE hot_potato.users SET profile_picture = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBinaryStream(1, photoStream, photoLength);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update profile picture: " + e.getMessage());
            return false;
        }
    }
}