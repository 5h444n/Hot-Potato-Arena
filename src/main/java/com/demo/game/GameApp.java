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
import com.demo.game.controllers.MultiplayerMenuController;
import com.demo.game.database.DatabaseConnection;
import com.demo.game.database.UserDAO;
import com.demo.game.events.BombExplodedEvent;
import com.demo.game.factories.*;
import com.demo.game.models.User;
import com.demo.game.network.GameClient;
import com.demo.game.network.messages.*;
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

/**
 * Main FXGL Game Application class for Hot Potato Arena.
 * Handles game initialization, input, updates, UI, physics, and networking (client-side).
 */
public class GameApp extends GameApplication {

    // --- Core Properties ---
    private UserDAO userDAO;
    private User currentUser;

    // --- Entity Properties ---
    /** The FXGL entity representing the locally controlled player (only used in Single Player). */
    private Entity playerEntity;
    /** The component managing the locally controlled player's state (only used in Single Player). */
    private PlayerComponent playerComponent;

    // --- Multiplayer Client Properties ---
    private GameClient gameClient;
    private Map<Integer, Entity> clientIdToEntity = new HashMap<>();
    private Entity bombEntity;
    private int myClientId = -1;
    private int currentBombHolderId = -1;

    // --- Interpolation ---
    private Map<Integer, Point2D> targetPositions = new HashMap<>();
    private static final double INTERPOLATION_FACTOR = 0.3;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(SCREEN_WIDTH);
        settings.setHeight(SCREEN_HEIGHT);
        settings.setTitle(GAME_TITLE);
        settings.setVersion(GAME_VERSION);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                return new LoginScene();
            }
        });
        settings.setMainMenuEnabled(true);
    }

    @Override
    protected void onPreInit() {
        currentUser = MultiplayerManager.getInstance().getLocalUser();
        if (currentUser == null) {
            currentUser = new User(0, "Guest", "guest@example.com");
            SceneManager.getInstance().setCurrentUser(currentUser);
        }
        userDAO = new UserDAO();

        FXGL.getPrimaryStage().setOnCloseRequest(e -> {
            System.out.println("Window closed, disconnecting...");
            DatabaseConnection.getInstance().disconnect();
            MultiplayerMenuController.stopExistingConnections();
            Platform.exit();
        });
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("score", SCORE);
        vars.put("lives", STARTING_LIVES);
        vars.put("level", 1);
        vars.put("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    @Override
    protected void initGame() {
        GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
        System.out.println("initGame: Current Mode is " + currentMode);

        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);

        FXGL.getGameWorld().addEntityFactory(new PlayerFactory());
        FXGL.getGameWorld().addEntityFactory(new AIFactory());
        FXGL.getGameWorld().addEntityFactory(new BombFactory());
        FXGL.getGameWorld().addEntityFactory(new WallFactory());
        FXGL.getGameWorld().addEntityFactory(new PortalFactory());

        if (currentMode == GameMode.SINGLE_PLAYER) {
            initSinglePlayer();
        } else {
            initMultiplayerClient();
        }
    }

    // =================================================================
    //                   SINGLE PLAYER LOGIC
    // =================================================================

    private void initSinglePlayer() {
        loadSinglePlayerArena();
        FXGL.getEventBus().addEventHandler(BombExplodedEvent.ANY, this::onSinglePlayerBombExploded);
    }

    private void loadSinglePlayerArena() {
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
            System.err.println("Multiplayer Error: Client not connected.");
            Platform.runLater(() -> {
                FXGL.getDialogService().showMessageBox("Connection Error.", () -> {
                    FXGL.getGameController().gotoMainMenu();
                });
            });
            return;
        }

        myClientId = -1;
        spawnWalls();

        gameClient.setOnMessageReceived(this::handleNetworkMessage);
        System.out.println("Multiplayer initialized. Message handler set.");

        MultiplayerManager manager = MultiplayerManager.getInstance();
        if (manager.isGameStartDataAvailable()) {
            System.out.println("initMultiplayerClient: GameStart data found. Processing immediately.");
            handleGameStart(manager.getInitialPositions(), manager.getInitialUsernames());
            manager.resetGameStartData();
        } else {
            System.out.println("initMultiplayerClient: No GameStart data found. Waiting for message...");
        }
    }

    private void handleNetworkMessage(NetworkMessage message) {
        if (message instanceof GameStartMessage) {
            handleGameStart(((GameStartMessage) message).initialPositions, ((GameStartMessage) message).usernames);
        } else if (message instanceof GameStateUpdateMessage) {
            handleGameStateUpdate((GameStateUpdateMessage) message);
        } else if (message instanceof BombPassMessage) {
            handleBombPass((BombPassMessage) message);
        } else if (message instanceof PlayerEliminatedMessage) {
            handlePlayerEliminated((PlayerEliminatedMessage) message);
        } else if (message instanceof GameOverMessage) {
            handleGameOver((GameOverMessage) message);
        }
    }

    private void handleGameStart(Map<Integer, SPoint2D> initialPositions, List<String> usernames) {
        System.out.println("handleGameStart: Processing game start data...");

        clientIdToEntity.clear();
        targetPositions.clear();

        if (initialPositions == null || usernames == null) {
            System.err.println("handleGameStart: ERROR - Received null start data!");
            return;
        }

        for (Map.Entry<Integer, SPoint2D> entry : initialPositions.entrySet()) {
            int clientId = entry.getKey();
            Point2D position = entry.getValue().toPoint2D();
            String username = (clientId < usernames.size()) ? usernames.get(clientId) : "Player " + clientId;

            Entity pEntity = FXGL.spawn("player", new SpawnData(position).put("username", username));

            if (pEntity == null || !pEntity.isActive()) {
                System.err.println("    Spawned entity FAILED for client ID: " + clientId);
                continue;
            }

            clientIdToEntity.put(clientId, pEntity);
            targetPositions.put(clientId, position);

            if (username.startsWith(currentUser.getUsername())) {
                myClientId = clientId;
                System.out.println("    Identified local player: ID=" + myClientId);
            }
        }

        if (bombEntity == null || !bombEntity.isActive()) {
            bombEntity = FXGL.spawn("bomb", -100, -100);
        }
        System.out.println("handleGameStart: Finished processing.");
    }

    private void handleGameStateUpdate(GameStateUpdateMessage msg) {
        if (bombEntity == null) return;

        targetPositions.clear();
        for (Map.Entry<Integer, SPoint2D> entry : msg.playerPositions.entrySet()) {
            targetPositions.put(entry.getKey(), entry.getValue().toPoint2D());
        }

        this.currentBombHolderId = msg.bombHolderId;
        Point2D currentBombPos = msg.bombPosition.toPoint2D();

        if (currentBombHolderId == -1) {
            if (bombEntity.xProperty().isBound()) {
                bombEntity.xProperty().unbind();
                bombEntity.yProperty().unbind();
            }
            bombEntity.setPosition(currentBombPos);
        } else {
            Entity holder = clientIdToEntity.get(currentBombHolderId);
            if (holder != null && holder.isActive()) {
                bombEntity.xProperty().unbind();
                bombEntity.yProperty().unbind();
                bombEntity.xProperty().bind(holder.xProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
                bombEntity.yProperty().bind(holder.yProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
            } else {
                if (bombEntity.xProperty().isBound()) {
                    bombEntity.xProperty().unbind();
                    bombEntity.yProperty().unbind();
                }
                bombEntity.setPosition(currentBombPos);
            }
        }

        double time = msg.bombTimerRemaining;
        FXGL.set("bombTime", time >= 0 ? time : BOMB_TIMER_DURATION.toSeconds());
    }

    private void handleBombPass(BombPassMessage msg) {
        FXGL.play("pass.wav");
        this.currentBombHolderId = msg.newHolderClientId;
        FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    private void handlePlayerEliminated(PlayerEliminatedMessage msg) {
        Entity eliminated = clientIdToEntity.get(msg.eliminatedClientId);
        if (eliminated != null) {
            eliminated.getViewComponent().setVisible(false);
            eliminated.getComponent(PhysicsComponent.class).overwritePosition(new Point2D(-200, -200));

            if (bombEntity != null && msg.eliminatedClientId == this.currentBombHolderId) {
                bombEntity.xProperty().unbind();
                bombEntity.yProperty().unbind();
                bombEntity.setPosition(new Point2D(-100,-100));
                this.currentBombHolderId = -1;
            }
            if (msg.eliminatedClientId == myClientId) {
                FXGL.getNotificationService().pushNotification("You have been eliminated!");
            }
        }
    }

    private void handleGameOver(GameOverMessage msg) {
        String message = msg.winnerUsername.equals("No one") ? "Game Over! It's a draw!"
                : msg.winnerUsername.startsWith(currentUser.getUsername()) ? "You Win!"
                : msg.winnerUsername + " wins!";

        MultiplayerManager.getInstance().reset();
        MultiplayerMenuController.stopExistingConnections();

        FXGL.getDialogService().showMessageBox(message, () -> FXGL.getGameController().gotoMainMenu());
    }

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

    /**
     * Initializes player input actions.
     * This method contains the fix for the NullPointerException.
     */
    @Override
    protected void initInput() {
        Input input = FXGL.getInput();

        input.addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                // Always get the latest game mode when the action occurs
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.moveLeft();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_LEFT);
                }
            }
            @Override
            protected void onActionEnd() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.stopMovingX();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_X);
                }
            }
        }, KeyCode.A);

        input.addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.moveRight();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_RIGHT);
                }
            }
            @Override
            protected void onActionEnd() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.stopMovingX();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_X);
                }
            }
        }, KeyCode.D);

        input.addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.moveUp();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_UP);
                }
            }
            @Override
            protected void onActionEnd() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.stopMovingY();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_Y);
                }
            }
        }, KeyCode.W);

        input.addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.moveDown();
                } else {
                    sendInput(PlayerInputMessage.InputType.MOVE_DOWN);
                }
            }
            @Override
            protected void onActionEnd() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.stopMovingY();
                } else {
                    sendInput(PlayerInputMessage.InputType.STOP_Y);
                }
            }
        }, KeyCode.S);

        input.addAction(new UserAction("Pass Bomb") {
            @Override
            protected void onActionBegin() {
                GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
                if (currentMode == GameMode.SINGLE_PLAYER) {
                    if (playerComponent != null) playerComponent.passBomb();
                } else {
                    sendInput(PlayerInputMessage.InputType.PASS_BOMB);
                }
            }
        }, KeyCode.SPACE);
    }

    private void sendInput(PlayerInputMessage.InputType inputType) {
        if (gameClient != null && gameClient.isRunning()) {
            gameClient.sendMessage(new PlayerInputMessage(inputType));
        }
    }

    @Override
    protected void initPhysics() {
        FXGL.getPhysicsWorld().setGravity(0, 0);
        GameMode currentMode = MultiplayerManager.getInstance().getGameMode();

        if (currentMode == GameMode.SINGLE_PLAYER) {
            FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.WALL) {});
            FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.AI, EntityType.WALL) {});
            FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.PORTAL) {
                @Override
                protected void onCollisionBegin(Entity p, Entity portal) {
                    portal.getComponent(PortalComponent.class).teleport(p);
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
        Text bombTimerText = new Text();
        bombTimerText.setTranslateX(SCREEN_WIDTH - 150);
        bombTimerText.setTranslateY(40);
        bombTimerText.setFill(Color.BLACK);
        bombTimerText.setFont(FXGL.getUIFactoryService().newFont(16));
        bombTimerText.textProperty().bind(FXGL.getWorldProperties().doubleProperty("bombTime").asString("Bomb Time: %.1f"));
        FXGL.getGameScene().addUINode(bombTimerText);

        GameMode currentMode = MultiplayerManager.getInstance().getGameMode();
        if (currentMode == GameMode.SINGLE_PLAYER) {
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
        }
    }

    @Override
    protected void onUpdate(double tpf) {
        GameMode currentMode = MultiplayerManager.getInstance().getGameMode();

        if (currentMode == GameMode.SINGLE_PLAYER) {
            FXGL.getGameWorld().getSingletonOptional(EntityType.BOMB).ifPresent(bomb -> {
                BombComponent bombComp = bomb.getComponent(BombComponent.class);
                double elapsed = bombComp.getElapsedTime();
                if (elapsed >= 0) {
                    double remaining = BOMB_TIMER_DURATION.toSeconds() - elapsed;
                    FXGL.set("bombTime", Math.max(0, remaining));
                } else {
                    FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
                }
            });
        } else {
            for (Map.Entry<Integer, Entity> entry : clientIdToEntity.entrySet()) {
                Entity entity = entry.getValue();
                Point2D targetPos = targetPositions.get(entry.getKey());
                if (targetPos != null && entity.isActive()) {
                    Point2D currentPos = entity.getPosition();
                    Point2D interpolatedPos = currentPos.interpolate(targetPos, INTERPOLATION_FACTOR);
                    entity.setPosition(interpolatedPos);
                }
            }
        }
    }
}

