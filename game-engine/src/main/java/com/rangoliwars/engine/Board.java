package com.rangoliwars.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Board {

    public static final int EMPTY = -1;
    public static final int MIN_SIZE = 5;
    public static final int MAX_SIZE = 15;

    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final int size;
    private final int[][] owners;
    private final boolean[][] shields;

    public Board(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new GameRuleException("Board size must be between " + MIN_SIZE + " and " + MAX_SIZE);
        }
        this.size = size;
        this.owners = new int[size][size];
        this.shields = new boolean[size][size];
        for (int[] row : owners) {
            Arrays.fill(row, EMPTY);
        }
    }

    private Board(Board other) {
        this.size = other.size;
        this.owners = other.ownersSnapshot();
        this.shields = other.shieldsSnapshot();
    }

    public Board copy() {
        return new Board(this);
    }

    public int size() {
        return size;
    }

    public boolean inBounds(Position p) {
        return p.row() >= 0 && p.row() < size && p.col() >= 0 && p.col() < size;
    }

    public int ownerAt(Position p) {
        requireInBounds(p);
        return owners[p.row()][p.col()];
    }

    public boolean isShielded(Position p) {
        requireInBounds(p);
        return shields[p.row()][p.col()];
    }

    public boolean isEmpty(Position p) {
        return ownerAt(p) == EMPTY;
    }

    public boolean isOnEdge(Position p) {
        return p.row() == 0 || p.col() == 0 || p.row() == size - 1 || p.col() == size - 1;
    }

    public List<Position> neighbors(Position p) {
        List<Position> result = new ArrayList<>(4);
        for (int[] d : DIRECTIONS) {
            Position n = new Position(p.row() + d[0], p.col() + d[1]);
            if (inBounds(n)) {
                result.add(n);
            }
        }
        return result;
    }

    public List<Position> cells() {
        List<Position> result = new ArrayList<>(size * size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                result.add(new Position(r, c));
            }
        }
        return result;
    }

    public int countOwnedBy(int player) {
        int count = 0;
        for (int[] row : owners) {
            for (int owner : row) {
                if (owner == player) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isFull() {
        return countOwnedBy(EMPTY) == 0;
    }

    public int[][] ownersSnapshot() {
        int[][] copy = new int[size][];
        for (int r = 0; r < size; r++) {
            copy[r] = owners[r].clone();
        }
        return copy;
    }

    public boolean[][] shieldsSnapshot() {
        boolean[][] copy = new boolean[size][];
        for (int r = 0; r < size; r++) {
            copy[r] = shields[r].clone();
        }
        return copy;
    }

    void setOwner(Position p, int owner) {
        requireInBounds(p);
        owners[p.row()][p.col()] = owner;
    }

    void setShielded(Position p, boolean shielded) {
        requireInBounds(p);
        shields[p.row()][p.col()] = shielded;
    }

    private void requireInBounds(Position p) {
        if (!inBounds(p)) {
            throw new GameRuleException("Position " + p + " is outside the board");
        }
    }
}
