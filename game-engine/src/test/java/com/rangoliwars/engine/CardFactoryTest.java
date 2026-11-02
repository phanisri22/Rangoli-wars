package com.rangoliwars.engine;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardFactoryTest {

    @Test
    void dealsRequestedCount() {
        assertEquals(3, new CardFactory(new Random(1)).deal(3).size());
    }

    @Test
    void drawsFollowWeights() {
        CardFactory factory = new CardFactory(new Random(42));
        Map<CardType, Integer> counts = new EnumMap<>(CardType.class);
        for (int i = 0; i < 10_000; i++) {
            counts.merge(factory.draw(), 1, Integer::sum);
        }
        int shields = counts.get(CardType.SHIELD);
        assertTrue(shields > 3700 && shields < 4300, "shield share was " + shields);
    }
}
