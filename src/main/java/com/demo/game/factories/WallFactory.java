package com.demo.game.factories;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.demo.game.EntityType;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class WallFactory implements EntityFactory {

    @Spawns("wall")
    public Entity newWall(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.WALL)
                .bbox(new HitBox(BoundingShape.box(30, 30)))
                .with(new PhysicsComponent())
                .viewWithBBox(new Rectangle(30, 30, Color.GRAY))
                .build();
    }
}
