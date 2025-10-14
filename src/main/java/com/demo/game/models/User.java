package com.demo.game.models;

import java.sql.Timestamp;

public class User {
    private int id;
    private String username;
    private String email;
    private int highScore;
    private int singlePlayerPlayed;
    private Timestamp createdAt;
    private Timestamp lastLogin;

    // Constructor
    public User(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }

    public int getSinglePlayerPlayed() { return singlePlayerPlayed; }
    public void setSinglePlayerPlayed(int singlePlayerPlayed) { this.singlePlayerPlayed = singlePlayerPlayed; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
}