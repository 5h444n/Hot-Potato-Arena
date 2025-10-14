package com.demo.game;

import javafx.util.Duration;

import java.util.List;

public class Config {

    // Screen dimensions
    public static final int SCREEN_WIDTH = 800;
    public static final int SCREEN_HEIGHT = 600;
    public static final String GAME_TITLE = "Hot Potato Arena";
    public static final String GAME_VERSION = "0.1.0";

    // match rules
    public static final int STARTING_LIVES = 3;
    public static final int TOTAL_ROUNDS = 3;
    public static final int SCORE = 0;

    // properties
    public static final double PLAYER_SPEED = 200.0; // pixels per second
    public static final int PLAYER_SIZE = 40;
    public static final int BOMB_SIZE = 20;
    public static final int WALL_SIZE = 20;

    public static final Duration PASS_COOLDOWN= Duration.seconds(1.5);
    public static final double PASS_RANGE = 50.0; // The distance the pass effect travels
    public static final List<Double> AI_SPEEDS = List.of(150.0, 180.0, 210.0); // Speeds for levels 1, 2, 3
    public static final Duration BOMB_TIMER_DURATION = Duration.seconds(5);
    public static final int MAX_LEVELS = 3;




    // Private constructor to prevent instantiation
    private Config() {}
}
