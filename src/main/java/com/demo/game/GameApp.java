package com.demo.game;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.demo.game.components.AIComponent;
import com.demo.game.components.PlayerComponent;
import com.demo.game.database.DatabaseConnection;
import com.demo.game.database.UserDAO;
import com.demo.game.events.BombExplodedEvent;
import com.demo.game.factories.AIFactory;
import com.demo.game.factories.BombFactory;
import com.demo.game.factories.PlayerFactory;
import com.demo.game.factories.WallFactory;
import com.demo.game.models.User;
import com.demo.game.scenes.LoginScene;
import com.demo.game.ui.SceneManager;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.demo.game.scenes.MainMenuScene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.demo.game.Config.*;

public class GameApp extends GameApplication {

    private Entity player;
    private PlayerComponent playerComponent;
    private Entity bomb;
    private List<Entity> activeAIs = new ArrayList<>();




    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle(GAME_TITLE);
        settings.setWidth(SCREEN_WIDTH);
        settings.setHeight(SCREEN_HEIGHT);
        settings.setVersion(GAME_VERSION);
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setMainMenuEnabled(true);

        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() {
                // When FXGL needs a main menu, it will now create ours.
                return new LoginScene();
            }
        });
    }

//    private void returnToMainMenu() {
//        // Save score before returning
//        if (SceneManager.getInstance().getCurrentUser() != null) {
//            UserDAO userDAO = new UserDAO();
//            userDAO.updateHighScore(
//                    SceneManager.getInstance().getCurrentUser().getId(),
//                    FXGL.geti("score")
//            );
//        }
//
//        // Close FXGL and return to JavaFX menu
//        FXGL.getGameController().exit();
//        SceneManager.getInstance().setGameRunning(false);  // Update game state
//
//        // Show the main menu stage
//        Platform.runLater(() -> {
//            Stage mainStage = SceneManager.getInstance().getStage();
//            mainStage.show();
//            SceneManager.getInstance().showMainMenu();
//        });
//    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("level", 0);
        vars.put("lives", STARTING_LIVES);
        vars.put("score", SCORE);
        vars.put("bombTime", 0.0);
    }

    @Override
    protected void initInput() {
        Input input = FXGL.getInput();

        FXGL.getInput().addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                playerComponent.moveLeft();
            }

            @Override
            protected void onActionEnd() {
                playerComponent.stopMovingX();
            }
        }, KeyCode.A);

        FXGL.getInput().addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                playerComponent.moveRight();
            }

            @Override
            protected void onActionEnd() {
                playerComponent.stopMovingX();
            }
        }, KeyCode.D);

        FXGL.getInput().addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                playerComponent.moveUp();
            }

            @Override
            protected void onActionEnd() {
                playerComponent.stopMovingY();
            }
        }, KeyCode.W);

        FXGL.getInput().addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                playerComponent.moveDown();
            }

            @Override
            protected void onActionEnd() {
                playerComponent.stopMovingY();
            }
        }, KeyCode.S);

        FXGL.getInput().addAction(new UserAction("Pass Bomb") {
            @Override
            protected void onActionBegin() {
                playerComponent.passBomb();
            }
        }, KeyCode.SPACE);
    }

    @Override
    protected void initGame() {
        // Get the main application window (the Stage)
        Stage stage = FXGL.getPrimaryStage();

        // This is the core of the solution.
        // We are adding an event handler that runs when the user
        // clicks the window's 'X' (close) button.
        stage.setOnCloseRequest(event -> {
            System.out.println("Window close request received. Disconnecting database...");
            DatabaseConnection.getInstance().disconnect();
        });

        FXGL.getGameWorld().addEntityFactory(new PlayerFactory());
        FXGL.getGameWorld().addEntityFactory(new AIFactory());
        FXGL.getGameWorld().addEntityFactory(new BombFactory());
        FXGL.getGameWorld().addEntityFactory(new WallFactory());

        //FXGL.loopBGM("bgm.mp3");

        loadArena(1);

        FXGL.getEventBus().addEventHandler(BombExplodedEvent.ANY, this::onBombExploded);

    }

    private void onBombExploded(BombExplodedEvent event) {
        Entity eliminated = event.getEliminatedEntity();
        if (eliminated == null) return;

        if (eliminated.isType(EntityType.AI)) {
            handleAIElimination(eliminated); // Pass the AI entity
        } else if (eliminated.isType(EntityType.PLAYER)) {
            handlePlayerElimination();
        }
    }

    private void handlePlayerElimination() {
        FXGL.inc("lives", -1);
        if (FXGL.geti("lives") <= 0) {
            endGame(false); // Player loses the game
        } else {
            FXGL.getDialogService().showMessageBox("You lost a life! Get ready...", this::startNewRound);
        }
    }

    private void handleAIElimination(Entity eliminatedAI) {
        FXGL.inc("score", 100 * FXGL.geti("level"));
        activeAIs.remove(eliminatedAI); // Remove the defeated AI from our list

        // Check if the level is cleared
        if (activeAIs.isEmpty()) {
            nextLevel();
        } else {
            // If there are still AIs left, start a new round in the same level
            startNewRound();
        }
    }

    private void nextLevel() {
        int currentLevel = FXGL.geti("level");
        int nextLevelNum = currentLevel + 1;

        if (nextLevelNum > MAX_LEVELS) {
            endGame(true); // Player has won the entire game
        } else {
            FXGL.getDialogService().showMessageBox("Level " + currentLevel + " Cleared!", () -> {
                FXGL.set("level", nextLevelNum);
                loadArena(nextLevelNum);
            });
        }
    }

    private void endGame(boolean playerWon) {
        String message;
        if (playerWon) {
            message = String.format("Congratulations! You beat all levels!\n\nFinal Score: %d", FXGL.geti("score"));

            // Save the score to the database
            User currentUser = SceneManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                UserDAO userDAO = new UserDAO();
                userDAO.updateHighScore(currentUser.getId(), FXGL.geti("score"));
                System.out.println("High score updated for user: " + currentUser.getUsername());
            }

        } else {
            message = String.format("Game Over!\n\nFinal Score: %d", FXGL.geti("score"));
        }

        FXGL.getDialogService().showMessageBox(message, () -> {
            // Go back to the main menu
            FXGL.getGameController().gotoMainMenu();
        });
    }

    private void loadArena(int levelNum) {
        FXGL.getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);
        activeAIs.clear(); // Clear the list of AIs from the previous level

        // Load the arena from the tiled map

        // Spawn walls all around the screen
        for (int x = 0; x < SCREEN_WIDTH; x += WALL_SIZE) {
            FXGL.spawn("wall", new SpawnData(x, 0).put("width", WALL_SIZE)); // Top wall
            FXGL.spawn("wall", new SpawnData(x, SCREEN_HEIGHT - WALL_SIZE).put("width", WALL_SIZE)); // Bottom wall
        }
        for (int y = WALL_SIZE; y < SCREEN_HEIGHT - WALL_SIZE; y += WALL_SIZE) {
            FXGL.spawn("wall", new SpawnData(0, y).put("width", WALL_SIZE)); // Left wall
            FXGL.spawn("wall", new SpawnData(SCREEN_WIDTH - WALL_SIZE, y).put("width", WALL_SIZE)); // Right wall
        }

        // Spawn player and AI
        player = FXGL.spawn("player", FXGL.random(WALL_SIZE, SCREEN_WIDTH - WALL_SIZE * 2), FXGL.random(WALL_SIZE, SCREEN_HEIGHT - WALL_SIZE * 2));
        playerComponent = player.getComponent(PlayerComponent.class);

        int numAIs;
        double aiSpeed;

        switch (levelNum) {
            case 1:
                numAIs = 3;
                aiSpeed = AI_SPEEDS.get(0); // Easy speed
                break;
            case 2:
                numAIs = 4;
                aiSpeed = AI_SPEEDS.get(1); // Medium speed
                break;
            case 3:
                numAIs = 5;
                aiSpeed = AI_SPEEDS.get(2); // Hard speed
                break;
            default:
                // This case handles winning the game
                endGame(true);
                return;
        }

        for (int i = 0; i < numAIs; i++) {
            Entity ai = FXGL.spawn("ai",
                    new SpawnData(FXGL.random(WALL_SIZE, SCREEN_WIDTH - WALL_SIZE * 2), FXGL.random(WALL_SIZE, SCREEN_HEIGHT - WALL_SIZE * 2))
                            .put("speed", aiSpeed)
            );
            activeAIs.add(ai);
        }

        startNewRound();
    }

    private void startNewRound() {
        playerComponent.respawn();

        // ✅ Create a list of all potential bomb holders
        List<Entity> potentialHolders = new ArrayList<>(activeAIs);
        potentialHolders.add(player);

        // ✅ Pick a random entity to receive the bomb
        Entity bombHolder = potentialHolders.get(FXGL.random(0, potentialHolders.size() - 1));
        Entity bomb = FXGL.spawn("bomb", 0, 0);


        if (bombHolder.isType(EntityType.PLAYER)) {
            bombHolder.getComponent(PlayerComponent.class).receiveBomb(bomb);
        } else {
            bombHolder.getComponent(AIComponent.class).receiveBomb(bomb);
        }
    }

    @Override
    protected void initPhysics() {
        // Set gravity to zero for a top-down game
        FXGL.getPhysicsWorld().setGravity(0, 0);

        // This handler can be used for bounce logic or other effects in the future
        FXGL.getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.PLAYER, EntityType.WALL) {
            @Override
            protected void onCollisionBegin(Entity player, Entity wall) {
                // The physics engine already prevents movement through the wall
            }
        });
    }

    @Override
    protected void initUI() {
        Text scoreText = new Text();
        scoreText.setTranslateX(20);
        scoreText.setTranslateY(40);
        scoreText.setFill(Color.BLACK);
        scoreText.textProperty().bind(FXGL.getWorldProperties().intProperty("score").asString("Score: %d"));

        Text livesText = new Text();
        livesText.setTranslateX(20);
        livesText.setTranslateY(70);
        livesText.setFill(Color.BLACK);
        livesText.textProperty().bind(FXGL.getWorldProperties().intProperty("lives").asString("Lives: %d"));

        Text bombTimerText = new Text();
        bombTimerText.setTranslateX(SCREEN_WIDTH - 150);
        bombTimerText.setTranslateY(40);
        bombTimerText.setFill(Color.BLACK);
        bombTimerText.textProperty().bind(FXGL.getWorldProperties().doubleProperty("bombTime").asString("Bomb Timer: %.1f"));


        FXGL.getGameScene().addUINode(scoreText);
        FXGL.getGameScene().addUINode(livesText);
        FXGL.getGameScene().addUINode(bombTimerText);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (playerComponent.hasBomb()) {
            // Find the bomb and update the HUD timer
            FXGL.inc("bombTime", -tpf);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
