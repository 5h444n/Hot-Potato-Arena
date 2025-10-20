package com.demo.game.database;

import java.sql.Connection;
import com.demo.game.models.User;

import java.sql.*;
import java.util.List;

public class MatchDAO {
    private final Connection connection;

    public MatchDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Creates a new match entry and returns its generated ID.
     * @return The ID of the newly created match, or -1 on failure.
     */
    public int createMatch() {
        String sql = "INSERT INTO hot_potato.matches (start_time) VALUES (CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to create match: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Adds a list of users as participants in a specific match.
     * @param matchId The ID of the match.
     * @param users The list of users participating.
     */
    public void addParticipants(int matchId, List<User> users) {
        String sql = "INSERT INTO hot_potato.match_participants (match_id, user_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (User user : users) {
                stmt.setInt(1, matchId);
                stmt.setInt(2, user.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("Failed to add participants: " + e.getMessage());
        }
    }

    /**
     * Updates a match to mark it as ended and sets the winner.
     * @param matchId The ID of the match.
     * @param winnerId The user ID of the winner.
     */
    public void concludeMatch(int matchId, int winnerId) {
        String updateWinnerSql = "UPDATE hot_potato.match_participants SET is_winner = TRUE WHERE match_id = ? AND user_id = ?";
        String updateEndTimeSql = "UPDATE hot_potato.matches SET end_time = CURRENT_TIMESTAMP WHERE match_id = ?";

        try (PreparedStatement winnerStmt = connection.prepareStatement(updateWinnerSql);
             PreparedStatement endTimeStmt = connection.prepareStatement(updateEndTimeSql)) {

            // Set winner
            winnerStmt.setInt(1, matchId);
            winnerStmt.setInt(2, winnerId);
            winnerStmt.executeUpdate();

            // Set end time
            endTimeStmt.setInt(1, matchId);
            endTimeStmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Failed to conclude match: " + e.getMessage());
        }
    }
}
