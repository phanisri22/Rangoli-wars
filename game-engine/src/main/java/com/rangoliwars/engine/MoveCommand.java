package com.rangoliwars.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Command pattern: each move type validates and applies itself, records every cell it
 * changes, and can be undone. Bots use undo to try moves without copying the board.
 */
public abstract class MoveCommand {

    private final Move move;
    private final Deque<CellChange> changes = new ArrayDeque<>();
    private boolean executed;

    protected MoveCommand(Move move) {
        this.move = move;
    }

    public Move move() {
        return move;
    }

    public final MoveResult execute(Board board, int player) {
        if (executed) {
            throw new IllegalStateException("Command already executed");
        }
        validate(board, player);
        int before = board.countOwnedBy(player);
        apply(board, player);
        List<Position> enclosed = CaptureRule.findEnclosed(board, player);
        for (Position p : enclosed) {
            setOwner(board, p, player);
        }
        executed = true;
        return new MoveResult(board.countOwnedBy(player) - before, enclosed.size());
    }

    public final void undo(Board board) {
        if (!executed) {
            throw new IllegalStateException("Command has not been executed");
        }
        while (!changes.isEmpty()) {
            CellChange change = changes.pop();
            board.setOwner(change.position(), change.owner());
            board.setShielded(change.position(), change.shielded());
        }
        executed = false;
    }

    protected final void setOwner(Board board, Position p, int owner) {
        changes.push(new CellChange(p, board.ownerAt(p), board.isShielded(p)));
        board.setOwner(p, owner);
    }

    protected final void setShielded(Board board, Position p, boolean shielded) {
        changes.push(new CellChange(p, board.ownerAt(p), board.isShielded(p)));
        board.setShielded(p, shielded);
    }

    protected abstract void validate(Board board, int player);

    protected abstract void apply(Board board, int player);
}
