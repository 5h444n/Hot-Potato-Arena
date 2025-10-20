// File: com/demo/game/components/PortalComponent.java
package com.demo.game.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.time.LocalTimer;
import javafx.util.Duration;

public class PortalComponent extends Component {
    private Entity targetPortal;
    private LocalTimer cooldownTimer;
    private boolean onCooldown = false;
    private static final Duration TELEPORT_COOLDOWN = Duration.seconds(2);

    @Override
    public void onAdded() {
        cooldownTimer = FXGL.newLocalTimer();
    }

    @Override
    public void onUpdate(double tpf) {
        if (onCooldown && cooldownTimer.elapsed(TELEPORT_COOLDOWN)) {
            onCooldown = false;
            // Optional: add a visual effect to show it's active
            entity.getViewComponent().setOpacity(1.0);
        }
    }

    public void setTarget(Entity target) {
        this.targetPortal = target;
    }

    public void teleport(Entity entityToTeleport) {
        if (onCooldown || targetPortal == null) {
            return; // Portal is waiting or has no target
        }

        // Get the target's PortalComponent
        PortalComponent targetComponent = targetPortal.getComponent(PortalComponent.class);

        // --- FIX ---
        // We must apply cooldown and sound *immediately* to prevent
        // multiple collision events before the teleport happens.
        this.startCooldown();
        targetComponent.startCooldown();
        FXGL.play("teleport.wav");

        // --- FIX ---
        // Defer the physics-modifying code (teleport) to the next game tick.
        // This runs the code *after* the physics world is unlocked.
        FXGL.runOnce(() -> {
            if (entityToTeleport.isActive()) {
                entityToTeleport.getComponent(PhysicsComponent.class)
                        .overwritePosition(targetPortal.getCenter());
            }
        }, Duration.seconds(0));
    }

    public void startCooldown() {
        onCooldown = true;
        cooldownTimer.capture();
        // Optional: add a visual effect to show it's on cooldown
        entity.getViewComponent().setOpacity(0.3);
    }
}