package com.rangoliwars.engine.bot;

import com.rangoliwars.engine.Board;

import java.util.Random;

public final class BotStrategies {

    private BotStrategies() {
    }

    public static BotStrategy forLevel(BotLevel level, Random random) {
        return switch (level) {
            case EASY -> new RandomBot(random);
            case MEDIUM -> new GreedyBot(random);
            case HARD -> new LookaheadBot(random);
        };
    }

    /** Own tiles minus the best opponent's tiles. */
    static int evaluate(Board board, int player, int playerCount) {
        int own = board.countOwnedBy(player);
        int bestOther = 0;
        for (int p = 0; p < playerCount; p++) {
            if (p != player) {
                bestOther = Math.max(bestOther, board.countOwnedBy(p));
            }
        }
        return own - bestOther;
    }
}
