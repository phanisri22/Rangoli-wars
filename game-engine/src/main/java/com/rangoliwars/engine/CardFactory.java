package com.rangoliwars.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Factory that deals power cards using each card type's weight. */
public final class CardFactory {

    private final Random random;
    private final int totalWeight;

    public CardFactory(Random random) {
        this.random = random;
        int sum = 0;
        for (CardType type : CardType.values()) {
            sum += type.weight();
        }
        this.totalWeight = sum;
    }

    public CardType draw() {
        int roll = random.nextInt(totalWeight);
        for (CardType type : CardType.values()) {
            roll -= type.weight();
            if (roll < 0) {
                return type;
            }
        }
        throw new IllegalStateException("Card weights are inconsistent");
    }

    public List<CardType> deal(int count) {
        List<CardType> hand = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            hand.add(draw());
        }
        return hand;
    }
}
