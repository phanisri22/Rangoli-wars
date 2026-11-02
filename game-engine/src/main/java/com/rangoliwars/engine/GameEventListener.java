package com.rangoliwars.engine;

/** Observer pattern: notified of every change to a game. */
@FunctionalInterface
public interface GameEventListener {

    void onEvent(Game game, GameEvent event);
}
