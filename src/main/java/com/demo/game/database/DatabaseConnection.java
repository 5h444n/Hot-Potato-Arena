package com.demo.game.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    // Load from system properties for security
    private static final String URL = "jdbc:postgresql://localhost:5432/games?hot_potato";
    private static final String USER = System.getProperty("db.user", "postgres");
    private static final String PASSWORD = System.getProperty("db.pass", "1234");

    private DatabaseConnection() {
        // private constructor for singleton
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public void connect() {
        if (USER == null || PASSWORD == null) {
            System.err.println("FATAL ERROR: Database username (db.user) and password (db.pass) are not set.");
            System.err.println("Please run with: -Ddb.user=YOUR_USER -Ddb.pass=YOUR_PASS");
            System.exit(1); // Exit if credentials are not provided
        }

        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            // It's good practice to exit if the DB connection fails at launch
            System.exit(1);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.err.println("Database connection was closed. Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Failed to check database connection: " + e.getMessage());
        }
        return connection;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database disconnected");
            }
        } catch (SQLException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
}