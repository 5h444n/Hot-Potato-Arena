package com.demo.game.network.messages;
public class PlayerInputMessage extends NetworkMessage {
    private static final long serialVersionUID = 1L;
    public enum InputType { MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN, STOP_X, STOP_Y, PASS_BOMB }
    public final InputType inputType;
    public PlayerInputMessage(InputType inputType) { this.inputType = inputType; }
}