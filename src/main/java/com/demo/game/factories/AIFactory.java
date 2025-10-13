package com.demo.game.factories;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.demo.game.Config;
import com.demo.game.EntityType;
import com.demo.game.components.AIComponent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class AIFactory implements EntityFactory {

    @Spawns("ai")
    public Entity newAI(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        //physics.setFixedRotation(true);

        return FXGL.entityBuilder(data)
                .type(EntityType.AI)
                .viewWithBBox(new Rectangle(Config.PLAYER_SIZE, Config.PLAYER_SIZE, Color.GREEN))
                .with(physics)
                .with(new AIComponent())
                .collidable()
                .build();
    }
}
