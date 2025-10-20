package com.demo.game.factories;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.multiplayer.NetworkComponent; // Import NetworkComponent
import com.demo.game.EntityType;
import com.demo.game.components.BombComponent;

import static com.demo.game.Config.BOMB_SIZE;

public class BombFactory implements EntityFactory {

    @Spawns("bomb")
    public Entity newBomb(SpawnData data) {
        return FXGL.entityBuilder(data)
                .type(EntityType.BOMB)
                .viewWithBBox(FXGL.texture("bomb.png", BOMB_SIZE, BOMB_SIZE))
                .with(new BombComponent())
                .with(new NetworkComponent()) // ADD NETWORK COMPONENT
                .build();
    }
}