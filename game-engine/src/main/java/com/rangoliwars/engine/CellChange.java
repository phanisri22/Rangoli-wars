package com.rangoliwars.engine;

/** The state of a cell before a command changed it, kept for undo. */
record CellChange(Position position, int owner, boolean shielded) {
}
