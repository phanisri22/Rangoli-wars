package com.rangoliwars.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A region of cells not owned by the player is captured when the player's tiles
 * fully enclose it, i.e. the region cannot reach the board edge. Shielded cells are kept.
 */
public final class CaptureRule {

    private CaptureRule() {
    }

    public static List<Position> findEnclosed(Board board, int player) {
        int n = board.size();
        boolean[][] seen = new boolean[n][n];
        List<Position> captured = new ArrayList<>();

        for (Position start : board.cells()) {
            if (seen[start.row()][start.col()] || board.ownerAt(start) == player) {
                continue;
            }
            List<Position> region = new ArrayList<>();
            boolean reachesEdge = false;
            Deque<Position> queue = new ArrayDeque<>();
            queue.add(start);
            seen[start.row()][start.col()] = true;

            while (!queue.isEmpty()) {
                Position p = queue.poll();
                region.add(p);
                if (board.isOnEdge(p)) {
                    reachesEdge = true;
                }
                for (Position next : board.neighbors(p)) {
                    if (!seen[next.row()][next.col()] && board.ownerAt(next) != player) {
                        seen[next.row()][next.col()] = true;
                        queue.add(next);
                    }
                }
            }

            if (!reachesEdge) {
                for (Position p : region) {
                    if (!board.isShielded(p)) {
                        captured.add(p);
                    }
                }
            }
        }
        return captured;
    }
}
