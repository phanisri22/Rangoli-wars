package com.rangoliwars.engine;

import java.util.ArrayList;
import java.util.List;

final class SplashCommand extends MoveCommand {

    SplashCommand(Move move) {
        super(move);
    }

    @Override
    protected void validate(Board board, int player) {
        board.ownerAt(move().target());
    }

    @Override
    protected void apply(Board board, int player) {
        for (Position p : area(board, move().target())) {
            if (board.ownerAt(p) != player && !board.isShielded(p)) {
                setOwner(board, p, player);
            }
        }
    }

    static List<Position> area(Board board, Position center) {
        List<Position> cells = new ArrayList<>(9);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                Position p = new Position(center.row() + dr, center.col() + dc);
                if (board.inBounds(p)) {
                    cells.add(p);
                }
            }
        }
        return cells;
    }
}
