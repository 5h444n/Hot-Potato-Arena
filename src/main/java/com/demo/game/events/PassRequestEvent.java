package com.demo.game.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * A LOCAL event fired from PlayerComponent when the player wants to pass the bomb.
 * This is heard by GameApp, which then sends the network message.
 */
public class PassRequestEvent extends Event {

    public static final EventType<PassRequestEvent> ANY = new EventType<>(Event.ANY, "PASS_REQUEST_EVENT");

    private final int targetConnectionID;

    public PassRequestEvent(int targetConnectionID) {
        super(ANY);
        this.targetConnectionID = targetConnectionID;
    }

    public int getTargetConnectionID() {
        return targetConnectionID;
    }
}