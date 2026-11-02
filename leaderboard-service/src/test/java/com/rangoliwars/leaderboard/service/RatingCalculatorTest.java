package com.rangoliwars.leaderboard.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingCalculatorTest {

    @Test
    void winnerGainsLastLosesMiddleGainsLittle() {
        assertEquals(25, RatingCalculator.delta(1, 3));
        assertEquals(5, RatingCalculator.delta(2, 3));
        assertEquals(-10, RatingCalculator.delta(3, 3));
    }

    @Test
    void fullDrawGivesEveryoneFive() {
        assertEquals(5, RatingCalculator.delta(1, 1));
        assertFalse(RatingCalculator.isWin(1, 1));
    }

    @Test
    void sharedLastPlaceAllLose() {
        assertEquals(-10, RatingCalculator.delta(2, 2));
        assertTrue(RatingCalculator.isWin(1, 2));
    }
}
