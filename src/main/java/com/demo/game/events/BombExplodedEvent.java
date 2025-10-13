package com.demo.game.events;

import com.almasb.fxgl.entity.Entity;
import javafx.event.Event;
import javafx.event.EventType;

public class BombExplodedEvent extends Event {
    public static final EventType<BombExplodedEvent> ANY = new EventType<>(Event.ANY, "BOMB_EXPLODED_EVENT");

    private final Entity eliminatedEntity;

    public BombExplodedEvent(Entity eliminatedEntity) {
        super(ANY);
        this.eliminatedEntity = eliminatedEntity;
    }

    public Entity getEliminatedEntity() {
        return eliminatedEntity;
    }
}
