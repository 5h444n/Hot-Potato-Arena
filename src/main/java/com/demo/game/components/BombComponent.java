package com.demo.game.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity; // Keep Entity import
import com.almasb.fxgl.entity.component.Component;
// Remove MultiplayerService import if not used
// import com.almasb.fxgl.multiplayer.MultiplayerService;
import com.almasb.fxgl.time.LocalTimer;
import com.demo.game.GameMode;
import com.demo.game.events.BombExplodedEvent;
// Remove network message import
// import com.demo.game.network.PlayerEliminatedMessage;
import com.demo.game.ui.MultiplayerManager; // Keep this
import javafx.util.Duration;

import static com.demo.game.Config.BOMB_TIMER_DURATION;

public class BombComponent extends Component {
    private LocalTimer countdownTimer;
    private boolean isTicking = false;
    private GameMode gameMode;
    private double startTimeSeconds = -1; // For getElapsedTime

    @Override
    public void onAdded() {
        countdownTimer = FXGL.newLocalTimer();
        this.gameMode = MultiplayerManager.getInstance().getGameMode();
    }

    public void startTimer() {
        countdownTimer.capture();
        startTimeSeconds = FXGL.getGameTimer().getNow(); // Record start time
        isTicking = true;
        // Don't set the UI var here, GameApp/GameStateUpdate handles it
        // FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    // Helper method for single player UI update?
    public double getElapsedTime() {
        if (!isTicking || startTimeSeconds < 0) return 0;
        return FXGL.getGameTimer().getNow() - startTimeSeconds;
    }


    @Override
    public void onUpdate(double tpf) {
        if (!isTicking) return;

        // --- Explosion check ONLY needed for Single Player ---
        if (gameMode == GameMode.SINGLE_PLAYER) {
            if (countdownTimer.elapsed(BOMB_TIMER_DURATION)) {
                explodeSinglePlayer();
            }
        }
        // Multiplayer explosion is handled entirely by the server message
    }

    // Renamed to clarify it's only for SP
    private void explodeSinglePlayer() {
        isTicking = false;
        startTimeSeconds = -1;
        // FXGL.play("explosion.wav");

        var holderOpt = FXGL.getGameWorld().getEntities().stream()
                .filter(e -> {
                    // Only check SP components
                    if (e.hasComponent(PlayerComponent.class) && e.getComponent(PlayerComponent.class).hasBomb()) {
                        return true;
                    }
                    return e.hasComponent(AIComponent.class) && e.getComponent(AIComponent.class).hasBomb();
                })
                .findFirst();

        Entity eliminatedEntity = holderOpt.orElse(null);

        if (eliminatedEntity != null) {
            // SP uses local events
            if (eliminatedEntity.hasComponent(PlayerComponent.class)) {
                eliminatedEntity.getComponent(PlayerComponent.class).eliminate();
            } else if (eliminatedEntity.hasComponent(AIComponent.class)) {
                eliminatedEntity.getComponent(AIComponent.class).eliminate();
            }
            FXGL.getEventBus().fireEvent(new BombExplodedEvent(eliminatedEntity));
        }

        // Only remove bomb locally in SP
        entity.removeFromWorld();
    }
}