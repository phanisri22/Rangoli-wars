package com.rangoliwars.engine;

public enum CardType {
    SPLASH("Colors every unshielded tile in a 3x3 area", 30),
    SHIELD("Protects one of your tiles from capture, splash and swap", 40),
    SWAP("Swaps one of your tiles with an adjacent opponent tile", 30);

    private final String description;
    private final int weight;

    CardType(String description, int weight) {
        this.description = description;
        this.weight = weight;
    }

    public String description() {
        return description;
    }

    public int weight() {
        return weight;
    }
}
