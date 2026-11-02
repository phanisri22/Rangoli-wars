package com.rangoliwars.engine;

import java.util.List;

public sealed interface GameEvent {

    record PlayerJoined(String playerId) implements GameEvent {
    }

    record PlayerLeft(String playerId) implements GameEvent {
    }

    record ConnectionChanged(String playerId, boolean connected) implements GameEvent {
    }

    record GameStarted() implements GameEvent {
    }

    record MovePlayed(MoveRecord record) implements GameEvent {
    }

    record TurnChanged(String playerId, int turnNumber) implements GameEvent {
    }

    record GameFinished(List<String> winnerIds, boolean endedEarly) implements GameEvent {
    }
}
