package com.rangoliwars.leaderboard.service;

/** Winners gain 25, the last place loses 10, everyone else gains 5. A full draw gives 5 to all. */
public final class RatingCalculator {

    public static final int START_RATING = 1000;
    public static final int MIN_RATING = 100;

    private RatingCalculator() {
    }

    public static int delta(int placement, int worstPlacement) {
        if (worstPlacement == 1) {
            return 5;
        }
        if (placement == 1) {
            return 25;
        }
        if (placement == worstPlacement) {
            return -10;
        }
        return 5;
    }

    public static boolean isWin(int placement, int worstPlacement) {
        return placement == 1 && worstPlacement > 1;
    }
}
