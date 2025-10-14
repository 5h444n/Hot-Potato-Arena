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

        if (hasBomb) {
            // STATE: ATTACKING - Find the nearest target and move towards it.
            findNearestTarget().ifPresent(target -> {
                Point2D moveDirection = target.getCenter().subtract(entity.getCenter()).normalize();
                physics.setLinearVelocity(moveDirection.multiply(speed));

                if (entity.distance(target) <= PASS_RANGE && passCoolDownTimer.elapsed(Config.PASS_COOLDOWN)) {
                    passBombTo(target);
                }
            });

        } else {
            // AI does NOT have the bomb. Decide whether to evade or wander.
            Optional<Entity> bombHolderOpt = findBombHolder();

            if (bombHolderOpt.isPresent()) {
                // STATE: EVADING - Someone has the bomb, run away!
                Entity bombHolder = bombHolderOpt.get();
                Point2D moveDirection = entity.getCenter().subtract(bombHolder.getCenter()).normalize();
                physics.setLinearVelocity(moveDirection.multiply(speed));
            } else {
                // STATE: WANDERING - No one has the bomb (it's between rounds). Move around.
                if (wanderTimer.elapsed(Duration.seconds(2))) {
                    changeWanderDirection();
                }
                physics.setLinearVelocity(wanderDirection.mul(speed).toPoint2D());
            }
        }
    }

    // ✅ NEW HELPER METHOD
    private Optional<Entity> findNearestTarget() {
        return FXGL.getGameWorld().getEntitiesFiltered(e -> !e.equals(entity) && (e.isType(EntityType.PLAYER) || e.isType(EntityType.AI)))
                .stream()
                .min((e1, e2) -> Double.compare(entity.distance(e1), entity.distance(e2)));
    }

    // ✅ NEW HELPER METHOD
    private Optional<Entity> findBombHolder() {
        return FXGL.getGameWorld().getEntitiesFiltered(e -> {
                    if (e.isType(EntityType.PLAYER)) return e.getComponent(PlayerComponent.class).hasBomb();
                    if (e.isType(EntityType.AI)) return e.getComponent(AIComponent.class).hasBomb();
                    return false;
                })
                .stream()
                .findFirst();
    }

    /**
     * NEW: Contains the logic for the AI to pass the bomb to a target.
     * @param target The entity to receive the bomb (the player).
     */
    private void passBombTo(Entity target) {
        bombEntity.xProperty().unbind();
        bombEntity.yProperty().unbind();

        if (target.isType(EntityType.PLAYER)) {
            target.getComponent(PlayerComponent.class).receiveBomb(bombEntity);
        } else { // It's an AI
            target.getComponent(AIComponent.class).receiveBomb(bombEntity);
        }

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
