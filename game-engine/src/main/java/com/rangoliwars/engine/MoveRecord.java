package com.rangoliwars.engine;

public record MoveRecord(
        int turn,
        int round,
        String playerId,
        int playerColor,
        Move move,
        int gained,
        int captured) {
}
