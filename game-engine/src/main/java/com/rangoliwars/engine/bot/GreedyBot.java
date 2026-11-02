package com.rangoliwars.engine.bot;

import com.rangoliwars.engine.Board;
import com.rangoliwars.engine.CardType;
import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.MoveCommand;
import com.rangoliwars.engine.MoveCommandFactory;
import com.rangoliwars.engine.MoveGenerator;
import com.rangoliwars.engine.MoveType;
import com.rangoliwars.engine.Position;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Medium: tries every move with execute/undo and keeps the best immediate result. */
public final class GreedyBot implements BotStrategy {

    private static final int CARD_COST = 3;

    private final Random random;

    public GreedyBot(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(Board board, int player, List<CardType> hand, int playerCount) {
        List<ScoredMove> ranked = rank(board, player, hand, playerCount);
        if (ranked.isEmpty()) {
            throw new GameRuleException("No legal moves");
        }
        return ranked.get(0).move();
    }

    /** All legal moves, best first. */
    List<ScoredMove> rank(Board board, int player, List<CardType> hand, int playerCount) {
        List<Move> moves = MoveGenerator.legalMoves(board, player, hand);
        return moves.stream()
                .map(m -> new ScoredMove(m, score(board, player, playerCount, m), random.nextInt(1000)))
                .sorted(Comparator.comparingInt(ScoredMove::score).reversed()
                        .thenComparingInt(ScoredMove::tieBreak))
                .toList();
    }

    private int score(Board board, int player, int playerCount, Move move) {
        int positional = move.type() == MoveType.PLACE ? ownNeighbors(board, player, move.target()) : -CARD_COST;
        MoveCommand command = MoveCommandFactory.create(move);
        command.execute(board, player);
        int value = BotStrategies.evaluate(board, player, playerCount) * 10;
        command.undo(board);
        return value + positional;
    }

    private static int ownNeighbors(Board board, int player, Position p) {
        int count = 0;
        for (Position n : board.neighbors(p)) {
            if (board.ownerAt(n) == player) {
                count++;
            }
        }
        return count;
    }

    record ScoredMove(Move move, int score, int tieBreak) {
    }
}
