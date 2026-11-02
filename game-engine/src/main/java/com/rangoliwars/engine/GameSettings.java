package com.rangoliwars.engine;

public record GameSettings(
        int boardSize,
        int maxRounds,
        int maxPlayers,
        int startingCards,
        int cardEveryTurns,
        int maxHandSize) {

    public static final GameSettings DEFAULT = of(9, 15, 4);

    public GameSettings {
        if (boardSize < Board.MIN_SIZE || boardSize > Board.MAX_SIZE) {
            throw new GameRuleException("Board size must be between " + Board.MIN_SIZE + " and " + Board.MAX_SIZE);
        }
        if (maxRounds < 1 || maxRounds > 50) {
            throw new GameRuleException("Rounds must be between 1 and 50");
        }
        if (maxPlayers < 2 || maxPlayers > 4) {
            throw new GameRuleException("A game needs 2 to 4 players");
        }
        if (maxHandSize < 0 || startingCards < 0 || startingCards > maxHandSize) {
            throw new GameRuleException("Starting cards cannot exceed the hand size");
        }
        if (cardEveryTurns < 1) {
            throw new GameRuleException("Card interval must be at least 1 turn");
        }
    }

    public static GameSettings of(int boardSize, int maxRounds, int maxPlayers) {
        return new GameSettings(boardSize, maxRounds, maxPlayers, 2, 4, 3);
    }
}
