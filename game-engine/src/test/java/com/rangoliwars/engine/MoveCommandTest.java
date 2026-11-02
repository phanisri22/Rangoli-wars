package com.rangoliwars.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveCommandTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(5);
    }

    private MoveResult run(Move move, int player) {
        return MoveCommandFactory.create(move).execute(board, player);
    }

    @Test
    void placeClaimsEmptyCell() {
        MoveResult result = run(Move.place(0, 0), 0);
        assertEquals(0, board.ownerAt(new Position(0, 0)));
        assertEquals(new MoveResult(1, 0), result);
    }

    @Test
    void placeOnTakenCellFails() {
        run(Move.place(0, 0), 0);
        assertThrows(GameRuleException.class, () -> run(Move.place(0, 0), 1));
    }

    @Test
    void placeThatClosesARingCaptures() {
        run(Move.place(1, 2), 0);
        run(Move.place(3, 2), 0);
        run(Move.place(2, 1), 0);
        MoveResult result = run(Move.place(2, 3), 0);
        assertEquals(new MoveResult(2, 1), result);
        assertEquals(0, board.ownerAt(new Position(2, 2)));
    }

    @Test
    void splashColorsThreeByThreeExceptShields() {
        run(Move.place(1, 1), 1);
        run(Move.shield(1, 1), 1);
        run(Move.place(2, 2), 1);
        MoveResult result = run(Move.splash(2, 2), 0);
        assertEquals(8, board.countOwnedBy(0));
        assertEquals(1, board.countOwnedBy(1));
        assertEquals(8, result.gained());
    }

    @Test
    void splashInCornerOnlyCoversCellsOnBoard() {
        run(Move.splash(0, 0), 0);
        assertEquals(4, board.countOwnedBy(0));
    }

    @Test
    void shieldOnlyOwnTile() {
        run(Move.place(0, 0), 1);
        assertThrows(GameRuleException.class, () -> run(Move.shield(0, 0), 0));
        run(Move.shield(0, 0), 1);
        assertTrue(board.isShielded(new Position(0, 0)));
        assertThrows(GameRuleException.class, () -> run(Move.shield(0, 0), 1));
    }

    @Test
    void swapExchangesAdjacentTiles() {
        run(Move.place(0, 0), 0);
        run(Move.place(0, 1), 1);
        run(Move.swap(0, 0, 0, 1), 0);
        assertEquals(1, board.ownerAt(new Position(0, 0)));
        assertEquals(0, board.ownerAt(new Position(0, 1)));
    }

    @Test
    void swapRules() {
        run(Move.place(0, 0), 0);
        run(Move.place(0, 1), 1);
        run(Move.place(2, 2), 1);
        assertThrows(GameRuleException.class, () -> run(Move.swap(0, 1, 0, 0), 0));
        assertThrows(GameRuleException.class, () -> run(Move.swap(0, 0, 2, 2), 0));
        assertThrows(GameRuleException.class, () -> run(Move.swap(0, 0, 1, 0), 0));
        run(Move.shield(0, 1), 1);
        assertThrows(GameRuleException.class, () -> run(Move.swap(0, 0, 0, 1), 0));
    }

    @Test
    void swapRequiresSecondTile() {
        assertThrows(GameRuleException.class, () -> new Move(MoveType.SWAP, new Position(0, 0), null));
    }

    @Test
    void undoRestoresBoardExactly() {
        run(Move.place(1, 2), 0);
        run(Move.place(3, 2), 0);
        run(Move.place(2, 1), 0);
        run(Move.place(2, 2), 1);
        int[][] before = board.ownersSnapshot();

        MoveCommand closing = MoveCommandFactory.create(Move.place(2, 3));
        closing.execute(board, 0);
        closing.undo(board);

        assertArrayEquals(before, board.ownersSnapshot());
    }

    @Test
    void undoRestoresShield() {
        run(Move.place(0, 0), 0);
        MoveCommand shield = MoveCommandFactory.create(Move.shield(0, 0));
        shield.execute(board, 0);
        shield.undo(board);
        assertEquals(false, board.isShielded(new Position(0, 0)));
    }

    @Test
    void commandCannotRunTwiceOrUndoBeforeRunning() {
        MoveCommand command = MoveCommandFactory.create(Move.place(0, 0));
        assertThrows(IllegalStateException.class, () -> command.undo(board));
        command.execute(board, 0);
        assertThrows(IllegalStateException.class, () -> command.execute(board, 0));
    }

    @Test
    void failedValidationChangesNothing() {
        run(Move.place(0, 0), 1);
        int[][] before = board.ownersSnapshot();
        assertThrows(GameRuleException.class, () -> run(Move.place(0, 0), 0));
        assertArrayEquals(before, board.ownersSnapshot());
    }
}
