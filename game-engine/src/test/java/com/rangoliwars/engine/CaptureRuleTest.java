package com.rangoliwars.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureRuleTest {

    private static void own(Board board, int player, int... coords) {
        for (int i = 0; i < coords.length; i += 2) {
            board.setOwner(new Position(coords[i], coords[i + 1]), player);
        }
    }

    @Test
    void capturesEnclosedEmptyCell() {
        Board board = new Board(5);
        own(board, 0, 1, 2, 3, 2, 2, 1, 2, 3);
        assertEquals(List.of(new Position(2, 2)), CaptureRule.findEnclosed(board, 0));
    }

    @Test
    void capturesEnclosedOpponentTile() {
        Board board = new Board(5);
        own(board, 0, 1, 2, 3, 2, 2, 1, 2, 3);
        own(board, 1, 2, 2);
        assertEquals(List.of(new Position(2, 2)), CaptureRule.findEnclosed(board, 0));
    }

    @Test
    void regionTouchingEdgeIsNotCaptured() {
        Board board = new Board(5);
        own(board, 0, 0, 1, 1, 0);
        assertTrue(CaptureRule.findEnclosed(board, 0).isEmpty());
    }

    @Test
    void openRingIsNotCaptured() {
        Board board = new Board(5);
        own(board, 0, 1, 2, 3, 2, 2, 1);
        assertTrue(CaptureRule.findEnclosed(board, 0).isEmpty());
    }

    @Test
    void shieldedCellsInsideARingAreKept() {
        Board board = new Board(5);
        own(board, 0, 1, 2, 3, 2, 2, 1, 2, 3);
        own(board, 1, 2, 2);
        board.setShielded(new Position(2, 2), true);
        assertTrue(CaptureRule.findEnclosed(board, 0).isEmpty());
    }

    @Test
    void capturesLargerRegion() {
        Board board = new Board(7);
        for (int i = 1; i <= 5; i++) {
            own(board, 0, 1, i, 5, i, i, 1, i, 5);
        }
        assertEquals(9, CaptureRule.findEnclosed(board, 0).size());
    }
}
