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

import java.util.Optional;

import static com.demo.game.Config.PASS_RANGE;

public class AIComponent extends Component {
    private PhysicsComponent physics;
    private boolean hasBomb = false;
    private double speed;
    private Entity bombEntity = null;
    private LocalTimer wanderTimer;
    private LocalTimer passCoolDownTimer;
    private Vec2 wanderDirection;

    @Override
    public void onAdded() {
        physics = entity.getComponent(PhysicsComponent.class);
        this.speed = entity.getDouble("speed");

        // Initialize timers
        wanderTimer = FXGL.newLocalTimer();
        passCoolDownTimer = FXGL.newLocalTimer();
        wanderTimer.capture();
        passCoolDownTimer.capture();
        changeWanderDirection();
    }

    @Override
    public void onUpdate(double tpf) {
        Optional<Entity> playerOpt = FXGL.getGameWorld().getSingletonOptional(EntityType.PLAYER);

        if (playerOpt.isEmpty()) {
            physics.setLinearVelocity(0, 0);
            return;
        }

        Entity player = playerOpt.get();
        boolean playerHasBomb = player.getComponent(PlayerComponent.class).hasBomb();

        if (this.hasBomb) {
            // STATE: ATTACKING (Move towards player AND try to pass)
           // Vec2 moveDirection = player.getCenter().subtract(entity.getCenter()).normalize();
            Vec2 moveDirection = new Vec2(player.getCenter().getX() - entity.getCenter().getX(), player.getCenter().getY() - entity.getCenter().getY()).normalize();
            physics.setLinearVelocity(moveDirection.mul(speed).toPoint2D());

            // --- NEW PASS LOGIC ---
            // Check if player is in range and cooldown is over
            if (entity.distance(player) <= Config.PASS_RANGE && passCoolDownTimer.elapsed(Config.PASS_COOLDOWN)) {
                passBombTo(player);
            }
            // --- END NEW ---

        } else if (playerHasBomb) {
            // STATE: EVADING
            //Vec2 moveDirection = entity.getCenter().subtract(player.getCenter()).normalize();
            Vec2 moveDirection = new Vec2(entity.getCenter().getX() - player.getCenter().getX(), entity.getCenter().getY() - player.getCenter().getY()).normalize();
            physics.setLinearVelocity(moveDirection.mul(speed).toPoint2D());

        } else {
            // STATE: WANDERING
            if (wanderTimer.elapsed(Duration.seconds(2))) {
                changeWanderDirection();
            }
            physics.setLinearVelocity(wanderDirection.mul(speed).toPoint2D());
        }
    }

    /**
     * NEW: Contains the logic for the AI to pass the bomb to a target.
     * @param target The entity to receive the bomb (the player).
     */
    private void passBombTo(Entity target) {
        // Unbind the bomb from the AI's position
        bombEntity.xProperty().unbind();
        bombEntity.yProperty().unbind();

        // Give the bomb to the player
        target.getComponent(PlayerComponent.class).receiveBomb(bombEntity);

        // Update the AI's state
        this.hasBomb = false;
        this.bombEntity = null;

        // Play sound and restart the cooldown
        FXGL.play("pass.wav");
        passCoolDownTimer.capture();
    }

    private void changeWanderDirection() {
        double angle = FXGL.random() * 360.0;
        wanderDirection = Vec2.fromAngle(angle);
        wanderTimer.capture();
    }

    public boolean hasBomb() {
        return hasBomb;
    }

    public void setHasBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
    }

    public void receiveBomb(Entity bomb) {
        if (!hasBomb) {
            this.bombEntity = bomb; // Keep track of the bomb entity
            this.hasBomb = true;
            bomb.getComponent(BombComponent.class).startTimer();
            bomb.xProperty().bind(entity.xProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));
            bomb.yProperty().bind(entity.yProperty().add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0));

            // Start cooldown as soon as AI gets the bomb
            passCoolDownTimer.capture();
        }
    }

    public void eliminate() {
        this.hasBomb = false;
        this.bombEntity = null; // Clear bomb reference
        entity.getViewComponent().setVisible(false);
        physics.overwritePosition(new Vec2(-100, -100).toPoint2D());
    }



}
