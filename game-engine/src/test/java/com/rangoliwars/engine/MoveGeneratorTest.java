package com.rangoliwars.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveGeneratorTest {

    @Test
    void emptyHandOnlyPlaces() {
        Board board = new Board(5);
        List<Move> moves = MoveGenerator.legalMoves(board, 0, List.of());
        assertEquals(25, moves.size());
        assertTrue(moves.stream().allMatch(m -> m.type() == MoveType.PLACE));
    }

    @Test
    void cardsAddTheirMoves() {
        Board board = new Board(5);
        board.setOwner(new Position(0, 0), 0);
        board.setOwner(new Position(0, 1), 1);
        List<Move> moves = MoveGenerator.legalMoves(board, 0,
                List.of(CardType.SPLASH, CardType.SHIELD, CardType.SWAP));

        assertEquals(23, count(moves, MoveType.PLACE));
        assertEquals(25, count(moves, MoveType.SPLASH));
        assertEquals(1, count(moves, MoveType.SHIELD));
        assertEquals(1, count(moves, MoveType.SWAP));
    }

    @Test
    void everyGeneratedMoveIsValid() {
        Board board = new Board(6);
        board.setOwner(new Position(2, 2), 0);
        board.setOwner(new Position(2, 3), 1);
        board.setOwner(new Position(3, 2), 1);
        for (Move move : MoveGenerator.legalMoves(board, 0, List.of(CardType.values()))) {
            MoveCommand command = MoveCommandFactory.create(move);
            command.execute(board, 0);
            command.undo(board);
        }
    }

    private static long count(List<Move> moves, MoveType type) {
        return moves.stream().filter(m -> m.type() == type).count();
    }
}
