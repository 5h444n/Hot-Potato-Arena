package com.demo.game.factories;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
// Import the component
import com.demo.game.components.PortalComponent;
import com.demo.game.EntityType;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.almasb.fxgl.entity.Spawns;

import static com.almasb.fxgl.dsl.FXGLForKtKt.entityBuilder;

public class PortalFactory implements EntityFactory {
    @Spawns("portal")
    public Entity newPortal(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.PORTAL)
                .bbox(new HitBox(BoundingShape.box(40, 40)))
                .with(new PhysicsComponent())
                // Add the component
                .with(new PortalComponent())
                .viewWithBBox(new Rectangle(40, 40, Color.PURPLE))
                .collidable() // Make it collidable
                .build();
    }
}