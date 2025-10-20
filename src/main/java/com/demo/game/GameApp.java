// File: com/demo/game/GameApp.java
package com.demo.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.demo.game.components.AIComponent;
import com.demo.game.components.BombComponent;
import com.demo.game.components.PlayerComponent;
import com.demo.game.components.PortalComponent;
import com.demo.game.controllers.MultiplayerMenuController; // To get client instance
import com.demo.game.database.DatabaseConnection;
import com.demo.game.database.UserDAO;
import com.demo.game.events.BombExplodedEvent;
import com.demo.game.factories.*;
import com.demo.game.models.User;
import com.demo.game.network.GameClient;
import com.demo.game.network.messages.*; // Import all messages
import com.demo.game.scenes.LoginScene;
import com.demo.game.ui.MultiplayerManager;
import com.demo.game.ui.SceneManager;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.demo.game.Config.*;

public class GameApp extends GameApplication {

    // --- Core Properties ---
    private UserDAO userDAO;
    private User currentUser;
    private GameMode gameMode;

    // --- Entity Properties ---
    private Entity playerEntity; // Represents the local player's entity (in SP)
    private PlayerComponent playerComponent; // Component for the local player (in SP)

    // --- Multiplayer Client Properties ---
    private GameClient gameClient;
    // Map server client ID to local FXGL entity
    private Map<Integer, Entity> clientIdToEntity = new HashMap<>();
    private Entity bombEntity; // Reference to the single bomb entity
    // Our own client ID assigned by the server (0 for host)
    private int myClientId = -1;
    // **FIX**: Track the bomb holder ID locally
    private int currentBombHolderId = -1;

    // --- Interpolation (For smooth movement) ---
    private Map<Integer, Point2D> targetPositions = new HashMap<>();
    // Adjust for smoothness
    private static final double INTERPOLATION_FACTOR = 0.1;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(SCREEN_WIDTH);
        settings.setHeight(SCREEN_HEIGHT);
        settings.setTitle(GAME_TITLE);
        settings.setVersion(GAME_VERSION);

        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                // Start at the LoginScene, which then handles moving to the main menu
                return new LoginScene();
            }
        });
        settings.setMainMenuEnabled(true);
    }

    @Override
    protected void onPreInit() {
        // Get game mode and user from our singleton managers
        gameMode = MultiplayerManager.getInstance().getGameMode();
        currentUser = MultiplayerManager.getInstance().getLocalUser();

        // Fallback for guest
        if (currentUser == null) {
            currentUser = new User(0, "Guest", "guest@example.com");
            SceneManager.getInstance().setCurrentUser(currentUser);
        }

        userDAO = new UserDAO();

        // --- Clean up networking on window close ---
        FXGL.getPrimaryStage().setOnCloseRequest(e -> {
            System.out.println("Window closed, disconnecting...");
            DatabaseConnection.getInstance().disconnect();
            // Stop server/client threads
            MultiplayerMenuController.stopExistingConnections();
            Platform.exit();
        });
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        // Single Player Vars
        vars.put("score", SCORE);
        vars.put("lives", STARTING_LIVES);
        vars.put("level", 1);
        // Shared Var for UI
        // UI variable
        vars.put("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    @Override
    protected void initGame() {
        // Factories needed for both modes
        FXGL.getGameWorld().addEntityFactory(new PlayerFactory());
        FXGL.getGameWorld().addEntityFactory(new AIFactory()); // Only used in SP
        FXGL.getGameWorld().addEntityFactory(new BombFactory());
        FXGL.getGameWorld().addEntityFactory(new WallFactory());
        FXGL.getGameWorld().addEntityFactory(new PortalFactory()); // Only used in SP

        // Route to the correct game mode logic
        if (gameMode == GameMode.SINGLE_PLAYER) {
            initSinglePlayer();
        } else {
            // Both MULTIPLAYER_HOST and MULTIPLAYER_CLIENT run this
            initMultiplayerClient();
        }
    }

    // =================================================================
    //                   SINGLE PLAYER LOGIC
    // =================================================================

    private void initSinglePlayer() {
        loadSinglePlayerArena();
        // Listen for local bomb explosion events
        FXGL.getEventBus().addEventHandler(BombExplodedEvent.ANY, this::onSinglePlayerBombExploded);
    }

    private void loadSinglePlayerArena() {
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        spawnWalls();
        playerEntity = FXGL.spawn("player", new SpawnData(SCREEN_WIDTH / 2.0 - PLAYER_SIZE, SCREEN_HEIGHT / 2.0).put("username", currentUser.getUsername()));
        playerComponent = playerEntity.getComponent(PlayerComponent.class);

        int level = FXGL.getip("level").get();
        double aiSpeed = AI_SPEEDS.get(Math.min(level - 1, AI_SPEEDS.size() - 1));
        for (int i = 0; i < level; i++) {
            FXGL.spawn("ai", new SpawnData(100 + i * 80, 100).put("speed", aiSpeed));
        }

        Entity portal1 = FXGL.spawn("portal", 100, 300);
        Entity portal2 = FXGL.spawn("portal", 650, 300);
        portal1.getComponent(PortalComponent.class).setTarget(portal2);
        portal2.getComponent(PortalComponent.class).setTarget(portal1);

        startNewRound();
    }

    private void startNewRound() {
        playerComponent.respawn();
        List<Entity> ais = FXGL.getGameWorld().getEntitiesByType(EntityType.AI);
        for (int i = 0; i < ais.size(); i++) {
            ais.get(i).getComponent(PhysicsComponent.class).overwritePosition(
                    new Vec2(100 + i * 80, 100).toPoint2D()
            );
        }

        Entity bomb = FXGL.spawn("bomb", -100, -100);
        List<Entity> characters = new ArrayList<>(ais);
        characters.add(playerEntity);
        Entity holder = characters.get(FXGL.random(0, characters.size() - 1));

        if (holder.isType(EntityType.PLAYER)) {
            playerComponent.receiveBomb(bomb);
        } else {
            holder.getComponent(AIComponent.class).receiveBomb(bomb);
        }
    }

    private void onSinglePlayerBombExploded(BombExplodedEvent event) {
        Entity eliminated = event.getEliminatedEntity();
        if (eliminated == null) {
            startNewRound();
            return;
        }

        if (eliminated.isType(EntityType.PLAYER)) {
            FXGL.inc("lives", -1);
            if (FXGL.getip("lives").get() <= 0) {
                endGame(false);
            } else {
                startNewRound();
            }
        } else if (eliminated.isType(EntityType.AI)) {
            eliminated.removeFromWorld();
            FXGL.inc("score", 100);
            if (FXGL.getGameWorld().getEntitiesByType(EntityType.AI).isEmpty()) {
                nextLevel();
            } else {
                startNewRound();
            }
        }
    }

    private void nextLevel() {
        int currentLevel = FXGL.getip("level").get();
        if (currentLevel >= MAX_LEVELS) {
            endGame(true);
        } else {
            FXGL.inc("level", 1);
            FXGL.inc("lives", 1);
            FXGL.getNotificationService().pushNotification("Level " + FXGL.getip("level") + "!");
            loadSinglePlayerArena();
        }
    }

    private void endGame(boolean playerWon) {
        if (playerWon) {
            int finalScore = FXGL.getip("score").get();
            if (currentUser != null && currentUser.getId() != 0) {
                userDAO.updateHighScore(currentUser.getId(), finalScore);
            }
            FXGL.getDialogService().showMessageBox("You Win!", () -> {
                MultiplayerManager.getInstance().reset();
                FXGL.getGameController().gotoMainMenu();
            });
        } else {
            FXGL.getDialogService().showMessageBox("Game Over!", () -> {
                MultiplayerManager.getInstance().reset();
                FXGL.getGameController().gotoMainMenu();
            });
        }
    }

    // =================================================================
    //                   MULTIPLAYER CLIENT LOGIC
    // =================================================================

    private void initMultiplayerClient() {
        gameClient = MultiplayerMenuController.getGameClientInstance();
        if (gameClient == null || !gameClient.isRunning()) {
            System.err.println("Multiplayer Error: Client not connected. Returning to menu.");
            FXGL.getDialogService().showMessageBox("Connection Error. Returning to menu.", () -> {
                MultiplayerMenuController.stopExistingConnections(); // Ensure cleanup
                FXGL.getGameController().gotoMainMenu();
            });
            return;
        }

        // Host is always client ID 0
        myClientId = (gameMode == GameMode.MULTIPLAYER_HOST) ? 0 : -1;

        spawnWalls(); // Walls are static and local

        // Set the message handler for GameApp
        // This is the most important part!
        gameClient.setOnMessageReceived(this::handleNetworkMessage);

        // Don't spawn entities yet, wait for GameStartMessage
        System.out.println("Multiplayer initialized. Waiting for GameStartMessage...");
    }

    /**
     * Central handler for ALL messages received from the server.
     * This runs on the JavaFX/FXGL thread.
     */
    private void handleNetworkMessage(NetworkMessage message) {
        if (message instanceof GameStartMessage) {
            handleGameStart((GameStartMessage) message);
        } else if (message instanceof GameStateUpdateMessage) {
            handleGameStateUpdate((GameStateUpdateMessage) message);
        } else if (message instanceof BombPassMessage) {
            handleBombPass((BombPassMessage) message);
        } else if (message instanceof PlayerEliminatedMessage) {
            handlePlayerEliminated((PlayerEliminatedMessage) message);
        } else if (message instanceof GameOverMessage) {
            handleGameOver((GameOverMessage) message);
        }
        // We ignore LobbyUpdateMessage here, as it's handled by LobbyController
    }

    /**
     * Spawns all players and the bomb when the game starts.
     */
    private void handleGameStart(GameStartMessage msg) {
        System.out.println("Received GameStartMessage. Spawning entities...");
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        spawnWalls();
        clientIdToEntity.clear();
        targetPositions.clear();

        // Spawn all players based on server data
        List<String> usernames = msg.usernames;
        int userIndex = 0;
        for (Map.Entry<Integer, Point2D> entry : msg.initialPositions.entrySet()) {
            int clientId = entry.getKey();
            Point2D position = entry.getValue();
            String username = (userIndex < usernames.size()) ? usernames.get(userIndex) : "Player " + clientId;

            Entity pEntity = FXGL.spawn("player", new SpawnData(position).put("username", username));
            clientIdToEntity.put(clientId, pEntity);
            // Initialize target position for interpolation
            targetPositions.put(clientId, position);

            // Identify the local player
            if (username.startsWith(currentUser.getUsername())) {
                playerEntity = pEntity; // This is our local entity
                // We don't need playerComponent in MP, server controls it
                // playerComponent = pEntity.getComponent(PlayerComponent.class);
                myClientId = clientId; // Assign our ID
                System.out.println("Identified local player: ID=" + myClientId + ", Entity=" + playerEntity);
            }
            userIndex++;
        }

        // Spawn the single bomb entity (initially off-screen)
        bombEntity = FXGL.spawn("bomb", -100, -100);

        System.out.println("Multiplayer game started locally.");
    }

    /**
     * Receives frequent updates on player/bomb positions from the server.
     */
    private void handleGameStateUpdate(GameStateUpdateMessage msg) {
        if (bombEntity == null) return; // Not initialized yet

        // Update target positions for interpolation
        // This just stores the *goal* position. onUpdate() handles the smooth move.
        targetPositions.putAll(msg.playerPositions);

        // --- Bomb State ---
        // **FIX**: Store the current bomb holder ID
        this.currentBombHolderId = msg.bombHolderId;
        Point2D currentBombPos = msg.bombPosition;

        // Update bomb visual position
        if (currentBombHolderId == -1) {
            // Unbind if it was bound
            if (bombEntity.xProperty().isBound()) {
                bombEntity.xProperty().unbind();
                bombEntity.yProperty().unbind();
            }
            bombEntity.setPosition(currentBombPos);
        } else {
            Entity holder = clientIdToEntity.get(currentBombHolderId);
            if (holder != null) {
                // Bind bomb to the new holder visually
                // We unbind first to ensure it's not bound to an old player
                bombEntity.xProperty().unbind();
                bombEntity.yProperty().unbind();
                bombEntity.xProperty().bind(holder.xProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
                bombEntity.yProperty().bind(holder.yProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
            } else {
                // Holder doesn't exist locally? May happen briefly. Position directly.
                bombEntity.setPosition(currentBombPos);
            }
        }

        // Update UI bomb timer
        double time = msg.bombTimerRemaining;
        FXGL.set("bombTime", time >= 0 ? time : BOMB_TIMER_DURATION.toSeconds());
    }

    /**
     * Handles the bomb pass event (plays sound).
     */
    private void handleBombPass(BombPassMessage msg) {
        // The GameStateUpdate will handle the actual binding/positioning.
        // This message is for immediate feedback (sound).
        System.out.println("Received BombPassMessage: New holder = " + msg.newHolderClientId);
        FXGL.play("pass.wav");

        // **FIX**: Update the local bomb holder ID immediately
        this.currentBombHolderId = msg.newHolderClientId;

        // Reset UI timer immediately for responsiveness
        FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    /**
     * Hides a player when they are eliminated.
     */
    private void handlePlayerEliminated(PlayerEliminatedMessage msg) {
        System.out.println("Received PlayerEliminatedMessage: ID = " + msg.eliminatedClientId);
        Entity eliminated = clientIdToEntity.get(msg.eliminatedClientId);
        if (eliminated != null) {
            // Visually eliminate the player
            eliminated.getViewComponent().setVisible(false);
            PhysicsComponent physics = eliminated.getComponent(PhysicsComponent.class);
            // Move off-screen reliably
            physics.overwritePosition(new Point2D(-200, -200));

            // **FIX**: Use the cached bomb holder ID to check for unbinding
            // Unbind bomb if they were holding it (visual cleanup)
            if (bombEntity != null && msg.eliminatedClientId == this.currentBombHolderId) {
                bombEntity.xProperty().unbind();
                bombEntity.yProperty().unbind();
                bombEntity.setPosition(new Point2D(-100,-100)); // Move bomb off-screen
                this.currentBombHolderId = -1; // Bomb is now un-held
            }

            // Check if it was us
            if (msg.eliminatedClientId == myClientId) {
                FXGL.getNotificationService().pushNotification("You have been eliminated!");
                // Input is still sent, but server will ignore it
            }
        }
    }

    /**
     * Shows the game over dialog and returns to the menu.
     */
    private void handleGameOver(GameOverMessage msg) {
        System.out.println("Received GameOverMessage: Winner = " + msg.winnerUsername);
        String message = msg.winnerUsername.equals("No one") ? "Game Over! It's a draw!"
                : msg.winnerUsername.equals(currentUser.getUsername()) ? "You Win!"
                : msg.winnerUsername + " wins!";

        // Ensure cleanup happens *before* showing dialog
        MultiplayerManager.getInstance().reset();
        MultiplayerMenuController.stopExistingConnections(); // Stop client thread

        FXGL.getDialogService().showMessageBox(message, () -> {
            // Already cleaned up, just go to menu
            FXGL.getGameController().gotoMainMenu();
        });
    }

    /**
     * Helper method to spawn the boundary walls.
     */
    private void spawnWalls() {
        for (int x = 0; x < SCREEN_WIDTH; x += WALL_SIZE) {
            FXGL.spawn("wall", x, 0);
            FXGL.spawn("wall", x, SCREEN_HEIGHT - WALL_SIZE);
        }
        for (int y = WALL_SIZE; y < SCREEN_HEIGHT - WALL_SIZE; y += WALL_SIZE) {
            FXGL.spawn("wall", 0, y);
            FXGL.spawn("wall", SCREEN_WIDTH - WALL_SIZE, y);
        }
    }

    // =================================================================
    //                   COMMON INITIALIZATION
    // =================================================================

    @Override
    protected void initInput() {
        Input input = FXGL.getInput();

        // --- WASD Movement Actions ---

        // A - Move Left
        input.addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.moveLeft();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_LEFT);
                }
            }
            @Override
            protected void onActionEnd() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.stopMovingX();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_X);
                }
            }
        }, KeyCode.A);

        // D - Move Right
        input.addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.moveRight();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_RIGHT);
                }
            }
            @Override
            protected void onActionEnd() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.stopMovingX();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_X);
                }
            }
        }, KeyCode.D);

        // W - Move Up
        input.addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.moveUp();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_UP);
                }
            }
            @Override
            protected void onActionEnd() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.stopMovingY();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_Y);
                }
            }
        }, KeyCode.W);

        // S - Move Down
        input.addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.moveDown();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_DOWN);
                }
            }
            @Override
            protected void onActionEnd() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.stopMovingY();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_Y);
                }
            }
        }, KeyCode.S);

        // SPACE - Pass Bomb
        input.addAction(new UserAction("Pass Bomb") {
            @Override
            protected void onActionBegin() {
                if (gameMode == GameMode.SINGLE_PLAYER) {
                    playerComponent.passBomb();
                } else {
                    sendInput(PlayerInputMessage.InputType.PASS_BOMB);
                }
            }
        }, KeyCode.SPACE);
    }

    /**
     * Helper method to send an input message to the server.
     */
    private void sendInput(PlayerInputMessage.InputType inputType) {
        if (gameClient != null && gameClient.isRunning()) {
            gameClient.sendMessage(new PlayerInputMessage(inputType));
        }
    }

    // This is no longer used, as our custom server handles physics
    @Override
    protected void initPhysics() {
        FXGL.getPhysicsWorld().setGravity(0, 0);

        // Wall collisions are still useful for SP
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.WALL) {});
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.AI, EntityType.WALL) {});

        if (gameMode == GameMode.SINGLE_PLAYER) {
            FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.PORTAL) {
                @Override
                protected void onCollisionBegin(Entity player, Entity portal) {
                    portal.getComponent(PortalComponent.class).teleport(player);
                }
            });
            FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.AI, EntityType.PORTAL) {
                @Override
                protected void onCollisionBegin(Entity ai, Entity portal) {
                    portal.getComponent(PortalComponent.class).teleport(ai);
                }
            });
        }
    }

    @Override
    protected void initUI() {
        // Bomb timer UI is common
        Text bombTimerText = new Text();
        bombTimerText.setTranslateX(SCREEN_WIDTH - 150);
        bombTimerText.setTranslateY(40);
        bombTimerText.setFill(Color.BLACK);
        bombTimerText.setFont(FXGL.getUIFactoryService().newFont(16));
        // Bind to the "bombTime" game variable
        bombTimerText.textProperty().bind(FXGL.getWorldProperties().doubleProperty("bombTime").asString("Bomb Time: %.1f"));
        FXGL.getGameScene().addUINode(bombTimerText);

        if (gameMode == GameMode.SINGLE_PLAYER) {
            // Single player UI (Score, Lives)
            Text scoreText = new Text();
            scoreText.setTranslateX(20);
            scoreText.setTranslateY(40);
            scoreText.setFill(Color.BLACK);
            scoreText.setFont(FXGL.getUIFactoryService().newFont(16));
            scoreText.textProperty().bind(FXGL.getWorldProperties().intProperty("score").asString("Score: %d"));

            Text livesText = new Text();
            livesText.setTranslateX(20);
            livesText.setTranslateY(70);
            livesText.setFill(Color.BLACK);
            livesText.setFont(FXGL.getUIFactoryService().newFont(16));
            livesText.textProperty().bind(FXGL.getWorldProperties().intProperty("lives").asString("Lives: %d"));

            FXGL.getGameScene().addUINode(scoreText);
            FXGL.getGameScene().addUINode(livesText);
        } else {
            // Multiplayer UI (Could show player list, etc.)
            // For now, it just shows the bomb timer.
        }
    }

    @Override
    protected void onUpdate(double tpf) {
        if (gameMode == GameMode.SINGLE_PLAYER) {
            // In SP, BombComponent updates its timer internally
            // We just need to read it for the UI
            FXGL.getGameWorld().getSingletonOptional(EntityType.BOMB).ifPresent(bomb -> {
                double elapsed = bomb.getComponent(BombComponent.class).getElapsedTime();
                if (elapsed > 0) {
                    double remaining = BOMB_TIMER_DURATION.toSeconds() - elapsed;
                    FXGL.set("bombTime", Math.max(0, remaining));
                } else {
                    FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
                }
            });

        } else {
            // --- Multiplayer Update: Interpolate positions ---
            for (Map.Entry<Integer, Entity> entry : clientIdToEntity.entrySet()) {
                int clientId = entry.getKey();
                Entity entity = entry.getValue();
                Point2D targetPos = targetPositions.get(clientId);

                if (targetPos != null && entity.isActive()) {
                    Point2D currentPos = entity.getPosition();
                    // Smoothly move from current to target position
                    Point2D interpolatedPos = currentPos.interpolate(targetPos, INTERPOLATION_FACTOR);

                    // Update position directly. Physics is disabled for MP entities
                    // as the server is authoritative.
                    entity.setPosition(interpolatedPos);
                }
            }
        }
    }
}