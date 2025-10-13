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
        // POLISHED: Initialize physics component here to prevent NullPointerException
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
        lastDirection = new Point2D(1, 0); // Update facing direction
    }
    public void moveUp() {
        physicsComponent.setVelocityY(-Config.PLAYER_SPEED);
        lastDirection = new Point2D(0, -1); // Update facing direction
    }
    public void moveDown() {
        physicsComponent.setVelocityY(Config.PLAYER_SPEED);
        lastDirection = new Point2D(0, 1); // Update facing direction
    }
    public void stopMovingX() {
        physicsComponent.setVelocityX(0);
    }
    public void stopMovingY() {
        physicsComponent.setVelocityY(0);
    }

    public void receiveBomb(Entity bomb)
    {
        this.hasBomb = true;
        this.bombEntity = bomb;
        bomb.getComponent(BombComponent.class).startTimer();
        passCoolDownTimer.capture();

        // Attach bomb to player
        bomb.setPosition(entity.getCenter().subtract(bomb.getWidth() / 2, bomb.getHeight() / 2));
        bomb.xProperty().bind(entity.xProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
        bomb.yProperty().bind(entity.yProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
    }

    public void passBomb() {
        if(hasBomb && passCoolDownTimer.elapsed(PASS_COOLDOWN)) {
            // bomb passing logic
            FXGL.getGameWorld().getSingletonOptional(EntityType.AI).ifPresent(ai -> {
                // Check if the AI is within passing range
                if (entity.distance(ai) <= Config.PASS_RANGE) {
                    // Transfer the bomb
                    this.hasBomb = false;
                    //un- attach the bomb
                    bombEntity.xProperty().unbind();

                    // Give bomb to AI
                    ai.getComponent(AIComponent.class).receiveBomb(bombEntity);

                    //FXGL.play("pass.wav");
                    passCoolDownTimer.capture();
                }
            });
        }
    }

    public void eliminate() {
        this.hasBomb = false;
        this.bombEntity = null;
        entity.getViewComponent().setVisible(false); // Hide player
        entity.getComponent(PhysicsComponent.class).overwritePosition(new Vec2(-100, -100).toPoint2D()); // Move off-screen
    }

    public void respawn() {
        entity.getViewComponent().setVisible(true);
        entity.getComponent(PhysicsComponent.class).overwritePosition(
                new Vec2(
                        Config.SCREEN_WIDTH / 2.0,
                        Config.SCREEN_HEIGHT / 2.0
                ).toPoint2D()
        );
    }
}
