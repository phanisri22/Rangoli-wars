package com.rangoliwars.game.room;

import com.rangoliwars.engine.Game;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** A game plus the room data the engine does not care about. */
public final class GameRoom {

    private final Game game;
    private final Instant createdAt = Instant.now();
    private final AtomicInteger botCounter = new AtomicInteger();
    private volatile String hostId;
    private volatile long turnDeadline;
    private volatile Long matchId;

    public GameRoom(Game game, String hostId) {
        this.game = game;
        this.hostId = hostId;
    }

    public String code() {
        return game.code();
    }

    public Game game() {
        return game;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String hostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    /** Epoch millis when the current human turn times out; 0 when a bot is moving. */
    public long turnDeadline() {
        return turnDeadline;
    }

    public void setTurnDeadline(long turnDeadline) {
        this.turnDeadline = turnDeadline;
    }

    public Long matchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public int nextBotNumber() {
        return botCounter.incrementAndGet();
    }
}
