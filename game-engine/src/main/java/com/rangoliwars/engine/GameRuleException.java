package com.rangoliwars.engine;

/** Thrown when a player breaks a game rule. The message is safe to show to players. */
public class GameRuleException extends RuntimeException {

    public GameRuleException(String message) {
        super(message);
    }
}
