package com.rangoliwars.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Lists the moves a player can make. Splashes that would change nothing are skipped. */
public final class MoveGenerator {

    private MoveGenerator() {
    }

    public static List<Move> legalMoves(Board board, int player, Collection<CardType> hand) {
        List<Move> moves = new ArrayList<>();
        List<Position> cells = board.cells();

        for (Position p : cells) {
            if (board.isEmpty(p)) {
                moves.add(new Move(MoveType.PLACE, p, null));
            }
        }
        if (hand.contains(CardType.SPLASH)) {
            for (Position p : cells) {
                if (splashChangesSomething(board, player, p)) {
                    moves.add(new Move(MoveType.SPLASH, p, null));
                }
            }
        }
        if (hand.contains(CardType.SHIELD)) {
            for (Position p : cells) {
                if (board.ownerAt(p) == player && !board.isShielded(p)) {
                    moves.add(new Move(MoveType.SHIELD, p, null));
                }
            }
        }
        if (hand.contains(CardType.SWAP)) {
            for (Position own : cells) {
                if (board.ownerAt(own) != player || board.isShielded(own)) {
                    continue;
                }
                for (Position other : board.neighbors(own)) {
                    int owner = board.ownerAt(other);
                    if (owner != Board.EMPTY && owner != player && !board.isShielded(other)) {
                        moves.add(new Move(MoveType.SWAP, own, other));
                    }
                }
            }
        }
        return moves;
    }

    private static boolean splashChangesSomething(Board board, int player, Position center) {
        for (Position p : SplashCommand.area(board, center)) {
            if (board.ownerAt(p) != player && !board.isShielded(p)) {
                return true;
            }
        }
        return false;
    }
}
