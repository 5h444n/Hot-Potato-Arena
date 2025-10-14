package com.demo.game.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.demo.game.models.User;
import org.mindrot.jbcrypt.BCrypt;


public class UserDAO {
    private Connection connection;

    public UserDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public boolean registerUser(String username, String email, String password) {
        String sql = "INSERT INTO hot_potato.users (username, email, password_hash) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);

            int rowsAffected = stmt.executeUpdate();

            // Create default settings for new user
            if (rowsAffected > 0) {
                createDefaultSettings(username);
            }

            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    private void createDefaultSettings(String username) {
        String sql = "INSERT INTO hot_potato.user_settings (user_id) SELECT id FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to create default settings: " + e.getMessage());
        }
    }

    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM hot_potato.users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (BCrypt.checkpw(password, storedHash)) {
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
}
