package com.rangoliwars.engine;

public record Position(int row, int col) {

    public boolean isAdjacentTo(Position other) {
        return Math.abs(row - other.row) + Math.abs(col - other.col) == 1;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
