package com.rangoliwars.engine;

final class ShieldCommand extends MoveCommand {

    ShieldCommand(Move move) {
        super(move);
    }

    @Override
    protected void validate(Board board, int player) {
        Position target = move().target();
        if (board.ownerAt(target) != player) {
            throw new GameRuleException("You can only shield your own tile");
        }
        if (board.isShielded(target)) {
            throw new GameRuleException("That tile is already shielded");
        }
    }

    @Override
    protected void apply(Board board, int player) {
        setShielded(board, move().target(), true);
    }
}
