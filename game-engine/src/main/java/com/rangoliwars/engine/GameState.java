package com.rangoliwars.engine;

import com.rangoliwars.engine.bot.BotLevel;

/** State pattern: each phase decides which actions are allowed. */
interface GameState {

    Phase phase();

    /** Message shown when an action is not allowed in this phase. */
    String rejection();

    default Player join(Game game, String id, String name, BotLevel botLevel) {
        throw new GameRuleException(rejection());
    }

    default void leave(Game game, String id) {
        throw new GameRuleException(rejection());
    }

    default void start(Game game) {
        throw new GameRuleException(rejection());
    }

    default MoveResult play(Game game, String playerId, Move move) {
        throw new GameRuleException(rejection());
    }

    void end(Game game);
}

final class LobbyState implements GameState {

    @Override
    public Phase phase() {
        return Phase.LOBBY;
    }

    @Override
    public String rejection() {
        return "The game has not started yet";
    }

    @Override
    public Player join(Game game, String id, String name, BotLevel botLevel) {
        return game.doJoin(id, name, botLevel);
    }

    @Override
    public void leave(Game game, String id) {
        game.doLeave(id);
    }

    @Override
    public void start(Game game) {
        game.doStart();
    }

    @Override
    public void end(Game game) {
        game.close();
    }
}

final class PlayingState implements GameState {

    @Override
    public Phase phase() {
        return Phase.PLAYING;
    }

    @Override
    public String rejection() {
        return "The game has already started";
    }

    @Override
    public MoveResult play(Game game, String playerId, Move move) {
        return game.doPlay(playerId, move);
    }

    @Override
    public void end(Game game) {
        game.finish(true);
    }
}

final class FinishedState implements GameState {

    @Override
    public Phase phase() {
        return Phase.FINISHED;
    }

    @Override
    public String rejection() {
        return "The game is over";
    }

    @Override
    public void end(Game game) {
        // already finished
    }
}
