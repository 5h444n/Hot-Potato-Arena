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
import com.demo.game.components.PlayerComponent;

public class PlayerFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        // Prevent player from rotating on collision
        //physics.setFixedRotation(true);

        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(FXGL.texture("player.png", Config.PLAYER_SIZE, Config.PLAYER_SIZE))
                .with(physics)
                .with(new PlayerComponent())
                .collidable()
                .build();
    }
}
