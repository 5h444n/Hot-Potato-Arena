package com.demo.game.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.time.LocalTimer;
import com.demo.game.events.BombExplodedEvent;
import javafx.util.Duration;

import static com.demo.game.Config.BOMB_TIMER_DURATION;

public class BombComponent extends Component {
    private LocalTimer countdownTimer;

    private boolean isTicking = false;

    @Override
    public void onAdded() {
        // This is called once when the component is attached to an entity
        countdownTimer = FXGL.newLocalTimer();
    }


    public void startTimer() {
        countdownTimer.capture();
        System.out.println(countdownTimer);
        isTicking = true;
        FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    public void resetTimer() {
        // Reset the internal timer for the explosion check
        countdownTimer.capture();
        // Also reset the public game variable for the UI
        FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds());
    }

    @Override
    public void onUpdate(double tpf) {
        // This is called every frame
        if (!isTicking) return;

        //FXGL.set("bombTime", BOMB_TIMER_DURATION.toSeconds() - countdownTimer.capture());

        if (countdownTimer.elapsed(BOMB_TIMER_DURATION)) {
            explode();
        }
    }

    private void explode() {
        isTicking = false;
//        FXGL.play("explosion.wav");
//        FXGL.spawn("explosion", entity.getCenter().subtract(32, 32));

        // Find the holder of the bomb
        var holderOpt = FXGL.getGameWorld().getEntities().stream()
                .filter(e -> {
                    if (e.hasComponent(PlayerComponent.class) && e.getComponent(PlayerComponent.class).hasBomb()) {
                        return true;
                    }
                    return e.hasComponent(AIComponent.class) && e.getComponent(AIComponent.class).hasBomb();
                })
                .findFirst();

        // Eliminate the holder
        holderOpt.ifPresent(holder -> {
            if (holder.hasComponent(PlayerComponent.class)) {
                holder.getComponent(PlayerComponent.class).eliminate();
            } else if (holder.hasComponent(AIComponent.class)) {
                holder.getComponent(AIComponent.class).eliminate();
            }
        });

        entity.removeFromWorld();

        // This event now signals that *someone* was eliminated.
        FXGL.getEventBus().fireEvent(new BombExplodedEvent(holderOpt.orElse(null)));
    }
}
