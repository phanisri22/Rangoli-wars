package com.rangoliwars.engine.bot;

import com.rangoliwars.engine.Board;
import com.rangoliwars.engine.CardType;
import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.MoveCommand;
import com.rangoliwars.engine.MoveCommandFactory;

import java.util.List;
import java.util.Random;

/**
 * Hard: takes the greedy bot's best candidates and checks how the next player could
 * answer each one (tiles only, since their cards are unknown).
 */
public final class LookaheadBot implements BotStrategy {

    private static final int CANDIDATES = 60;

    private final GreedyBot greedy;

    public LookaheadBot(Random random) {
        this.greedy = new GreedyBot(random);
    }

    @Override
    public Move chooseMove(Board board, int player, List<CardType> hand, int playerCount) {
        List<GreedyBot.ScoredMove> candidates = greedy.rank(board, player, hand, playerCount);
        if (candidates.isEmpty()) {
            throw new GameRuleException("No legal moves");
        }
        int next = (player + 1) % playerCount;
        Move best = candidates.get(0).move();
        int bestValue = Integer.MIN_VALUE;

        for (GreedyBot.ScoredMove candidate : candidates.subList(0, Math.min(CANDIDATES, candidates.size()))) {
            MoveCommand mine = MoveCommandFactory.create(candidate.move());
            mine.execute(board, player);
            int value = valueAfterReply(board, player, next, playerCount);
            mine.undo(board);
            if (value > bestValue) {
                bestValue = value;
                best = candidate.move();
            }
        }
        return best;
    }

    private int valueAfterReply(Board board, int player, int next, int playerCount) {
        List<GreedyBot.ScoredMove> replies = greedy.rank(board, next, List.of(), playerCount);
        if (replies.isEmpty()) {
            return BotStrategies.evaluate(board, player, playerCount);
        }
        MoveCommand reply = MoveCommandFactory.create(replies.get(0).move());
        reply.execute(board, next);
        int value = BotStrategies.evaluate(board, player, playerCount);
        reply.undo(board);
        return value;
    }
}
