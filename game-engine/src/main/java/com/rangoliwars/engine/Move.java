package com.rangoliwars.engine;

/** A move as sent by a player. {@code secondary} is only used by SWAP. */
public record Move(MoveType type, Position target, Position secondary) {

    public Move {
        if (type == null) {
            throw new GameRuleException("Move type is required");
        }
        if (target == null) {
            throw new GameRuleException("Move target is required");
        }
        if (type == MoveType.SWAP && secondary == null) {
            throw new GameRuleException("Swap needs a second tile");
        }
    }

    public static Move place(int row, int col) {
        return new Move(MoveType.PLACE, new Position(row, col), null);
    }

    public static Move splash(int row, int col) {
        return new Move(MoveType.SPLASH, new Position(row, col), null);
    }

    public static Move shield(int row, int col) {
        return new Move(MoveType.SHIELD, new Position(row, col), null);
    }

    public static Move swap(int ownRow, int ownCol, int otherRow, int otherCol) {
        return new Move(MoveType.SWAP, new Position(ownRow, ownCol), new Position(otherRow, otherCol));
    }
}
