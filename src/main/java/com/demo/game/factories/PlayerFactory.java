package com.demo.game.factories;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.multiplayer.NetworkComponent; // Import NetworkComponent
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.demo.game.Config;
import com.demo.game.EntityType;
import com.demo.game.components.PlayerComponent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class PlayerFactory implements EntityFactory {

    @Spawns("player")
    public Entity newPlayer(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        //physics.setFixedRotation(true); // Prevent player from spinning

        // Get username from spawn data, default to "Player"
        String username = data.get("username");
        if (username == null) {
            username = "Player";
        }

        // Remove "(Host)" tag for in-game display
        username = username.replace(" (Host)", "");

        Text usernameText = new Text(username);
        usernameText.setFill(Color.BLACK);
        // Position text above the player
        usernameText.setTranslateY(-10);

        return FXGL.entityBuilder(data)
                .type(EntityType.PLAYER)
                .viewWithBBox(FXGL.texture("player.png", Config.PLAYER_SIZE, Config.PLAYER_SIZE))
                .view(usernameText) // Add username text
                .with(physics)
                .with(new PlayerComponent())
                .with(new NetworkComponent()) // ADD NETWORK COMPONENT
                .collidable()
                .build();
    }
}