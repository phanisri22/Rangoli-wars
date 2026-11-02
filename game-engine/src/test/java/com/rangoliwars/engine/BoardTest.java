package com.rangoliwars.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {

    @Test
    void newBoardIsEmpty() {
        Board board = new Board(5);
        assertEquals(25, board.countOwnedBy(Board.EMPTY));
        assertFalse(board.isFull());
    }

    @Test
    void rejectsSizeOutsideLimits() {
        assertThrows(GameRuleException.class, () -> new Board(4));
        assertThrows(GameRuleException.class, () -> new Board(16));
    }

    @Test
    void cornerHasTwoNeighborsAndCenterHasFour() {
        Board board = new Board(5);
        assertEquals(2, board.neighbors(new Position(0, 0)).size());
        assertEquals(4, board.neighbors(new Position(2, 2)).size());
    }

    @Test
    void detectsEdges() {
        Board board = new Board(5);
        assertTrue(board.isOnEdge(new Position(0, 3)));
        assertTrue(board.isOnEdge(new Position(4, 4)));
        assertFalse(board.isOnEdge(new Position(2, 2)));
    }

    @Test
    void outOfBoundsAccessIsARuleError() {
        Board board = new Board(5);
        assertThrows(GameRuleException.class, () -> board.ownerAt(new Position(5, 0)));
    }

    @Test
    void copyIsIndependent() {
        Board board = new Board(5);
        Board copy = board.copy();
        copy.setOwner(new Position(1, 1), 0);
        assertTrue(board.isEmpty(new Position(1, 1)));
        assertEquals(1, copy.countOwnedBy(0));
    }

    @Test
    void snapshotsDoNotLeakInternalState() {
        Board board = new Board(5);
        board.ownersSnapshot()[0][0] = 3;
        board.shieldsSnapshot()[0][0] = true;
        assertTrue(board.isEmpty(new Position(0, 0)));
        assertFalse(board.isShielded(new Position(0, 0)));
    }
}
