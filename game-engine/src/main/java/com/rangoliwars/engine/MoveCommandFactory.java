package com.rangoliwars.engine;

/** Factory that turns a player's move into the command that runs it. */
public final class MoveCommandFactory {

    private MoveCommandFactory() {
    }

    public static MoveCommand create(Move move) {
        return switch (move.type()) {
            case PLACE -> new PlaceTileCommand(move);
            case SPLASH -> new SplashCommand(move);
            case SHIELD -> new ShieldCommand(move);
            case SWAP -> new SwapCommand(move);
        };
    }
}
