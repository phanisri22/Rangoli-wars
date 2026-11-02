package com.rangoliwars.engine;

final class SwapCommand extends MoveCommand {

    SwapCommand(Move move) {
        super(move);
    }

    @Override
    protected void validate(Board board, int player) {
        Position own = move().target();
        Position other = move().secondary();
        if (board.ownerAt(own) != player) {
            throw new GameRuleException("The first tile must be yours");
        }
        int otherOwner = board.ownerAt(other);
        if (otherOwner == Board.EMPTY || otherOwner == player) {
            throw new GameRuleException("The second tile must belong to an opponent");
        }
        if (!own.isAdjacentTo(other)) {
            throw new GameRuleException("Swapped tiles must be next to each other");
        }
        if (board.isShielded(own) || board.isShielded(other)) {
            throw new GameRuleException("Shielded tiles cannot be swapped");
        }
    }

    @Override
    protected void apply(Board board, int player) {
        Position own = move().target();
        Position other = move().secondary();
        int otherOwner = board.ownerAt(other);
        setOwner(board, own, otherOwner);
        setOwner(board, other, player);
    }
}
