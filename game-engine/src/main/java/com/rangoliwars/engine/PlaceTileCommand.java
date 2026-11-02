package com.rangoliwars.engine;

final class PlaceTileCommand extends MoveCommand {

    PlaceTileCommand(Move move) {
        super(move);
    }

    @Override
    protected void validate(Board board, int player) {
        if (!board.isEmpty(move().target())) {
            throw new GameRuleException("That cell is already taken");
        }
    }

    @Override
    protected void apply(Board board, int player) {
        setOwner(board, move().target(), player);
    }
}
