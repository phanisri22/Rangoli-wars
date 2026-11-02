package com.rangoliwars.engine;

/** {@code gained} is the net change in the mover's tile count. */
public record MoveResult(int gained, int captured) {
}
