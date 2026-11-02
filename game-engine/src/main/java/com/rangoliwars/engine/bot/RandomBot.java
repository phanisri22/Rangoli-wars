package com.rangoliwars.engine.bot;

import com.rangoliwars.engine.Board;
import com.rangoliwars.engine.CardType;
import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.MoveGenerator;
import com.rangoliwars.engine.MoveType;

import java.util.List;
import java.util.Random;

/** Easy: random tile, occasionally a random card. */
public final class RandomBot implements BotStrategy {

    private final Random random;

    public RandomBot(Random random) {
        this.random = random;
    }

    @Override
    public Move chooseMove(Board board, int player, List<CardType> hand, int playerCount) {
        List<Move> moves = MoveGenerator.legalMoves(board, player, hand);
        if (moves.isEmpty()) {
            throw new GameRuleException("No legal moves");
        }
        List<Move> places = moves.stream().filter(m -> m.type() == MoveType.PLACE).toList();
        List<Move> cardMoves = moves.stream().filter(m -> m.type() != MoveType.PLACE).toList();
        boolean useCard = !cardMoves.isEmpty() && (places.isEmpty() || random.nextInt(5) == 0);
        List<Move> pool = useCard ? cardMoves : places;
        return pool.get(random.nextInt(pool.size()));
    }
}
