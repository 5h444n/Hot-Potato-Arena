package com.demo.game.components;

import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.time.LocalTimer;
import com.demo.game.Config;
import com.demo.game.EntityType;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import static com.demo.game.Config.PASS_COOLDOWN;

public class PlayerComponent extends Component {

    private PhysicsComponent physicsComponent;
    private boolean hasBomb = false;
    private Point2D lastDirection = new Point2D(1, 0);
    private LocalTimer passCoolDownTimer;
    private Entity bombEntity = null;

    public boolean hasBomb() {
        return hasBomb;
    }

    public void setHasBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
    }

    @Override
    public void onAdded() {
        // Initialize physics component here to prevent NullPointerException
        physicsComponent = entity.getComponent(PhysicsComponent.class);
        passCoolDownTimer = FXGL.newLocalTimer();
        passCoolDownTimer.capture();
    }

    public void moveLeft() {
        physicsComponent.setVelocityX(-Config.PLAYER_SPEED);
        lastDirection = new Point2D(-1, 0); // Update facing direction
    }

    public void moveRight() {
        physicsComponent.setVelocityX(Config.PLAYER_SPEED);
        lastDirection = new Point2D(1, 0);
    }

    public void moveUp() {
        physicsComponent.setVelocityY(-Config.PLAYER_SPEED);
        lastDirection = new Point2D(0, -1);
    }

    public void moveDown() {
        physicsComponent.setVelocityY(Config.PLAYER_SPEED);
        lastDirection = new Point2D(0, 1);
    }

    // --- FIX: Methods to STOP movement when key is released ---
    public void stopMovingX() {
        // Only stop if we are currently moving horizontally
        if (physicsComponent.getVelocityX() != 0) {
            physicsComponent.setVelocityX(0);
        }
    }

    public void stopMovingY() {
        // Only stop if we are currently moving vertically
        if (physicsComponent.getVelocityY() != 0) {
            physicsComponent.setVelocityY(0);
        }
    }

    /**
     * Called when the player receives the bomb (e.g., from an AI pass or game start).
     * @param bomb The bomb Entity being received.
     */
    public void receiveBomb(Entity bomb) {
        if (!hasBomb) {
            this.bombEntity = bomb; // Keep track of the bomb entity
            this.hasBomb = true;

            // Start the bomb timer
            bomb.getComponent(BombComponent.class).startTimer();

            // Bind the bomb's position to the player's position
            // This makes the bomb move with the player
            bomb.xProperty().bind(entity.xProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
            bomb.yProperty().bind(entity.yProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));

            // Reset the pass cooldown timer
            passCoolDownTimer.capture();
        }
    }


    // --- FIX: The missing passBomb() method ---
    public void passBomb() {
        if (hasBomb && passCoolDownTimer.elapsed(PASS_COOLDOWN)) {
            // Find the closest AI that is within passing range
            FXGL.getGameWorld()
                    .getClosestEntity(entity, e -> e.isType(EntityType.AI))
                    .ifPresent(closestAI -> {
                        if (entity.distance(closestAI) <= Config.PASS_RANGE) {
                            // Transfer the bomb
                            this.hasBomb = false;
                            // Unbind the bomb from the player's position properties
                            bombEntity.xProperty().unbind();
                            bombEntity.yProperty().unbind();


                            // Give bomb to the closest AI
                            closestAI.getComponent(AIComponent.class).receiveBomb(bombEntity);

                            FXGL.play("pass.wav");
                            passCoolDownTimer.capture();
                        }
                    });
        }
    }

    public void eliminate() {
        this.hasBomb = false;
        this.bombEntity = null;
        entity.getViewComponent().setVisible(false);
        entity.getComponent(PhysicsComponent.class).overwritePosition(new Vec2(-100, -100).toPoint2D());
    }

    public void respawn() {
        entity.getViewComponent().setVisible(true);

        double respawnX = Config.SCREEN_WIDTH / 2.0 - Config.PLAYER_SIZE / 2.0;
        double respawnY = Config.SCREEN_HEIGHT / 2.0 - Config.PLAYER_SIZE / 2.0;

        entity.setPosition(respawnX, respawnY);

        physicsComponent.setVelocityX(0);
        physicsComponent.setVelocityY(0);
    }
}