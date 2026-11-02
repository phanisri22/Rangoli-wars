package com.rangoliwars.leaderboard;

import com.rangoliwars.leaderboard.model.Models.MatchReport;
import com.rangoliwars.leaderboard.model.Models.ReportMove;
import com.rangoliwars.leaderboard.model.Models.ReportPlayer;

import java.time.Instant;
import java.util.List;

public final class TestReports {

    private TestReports() {
    }

    /** {@code winner} beats {@code loser}; a bot comes last. */
    public static MatchReport threePlayer(String winner, String loser) {
        Instant now = Instant.now();
        return new MatchReport("ROOM1", 7, 5, now.minusSeconds(120), now, false,
                List.of(new ReportPlayer(winner, winner, 0, 12, false, null, 1),
                        new ReportPlayer(loser, loser, 1, 8, false, null, 2),
                        new ReportPlayer(null, "Diya (Easy bot)", 2, 3, true, "EASY", 3)),
                List.of(new ReportMove(1, 1, winner, 0, "PLACE", 0, 0, null, null, 1, 0),
                        new ReportMove(2, 1, loser, 1, "SWAP", 0, 1, 0, 0, 0, 0),
                        new ReportMove(3, 1, "bot:1", 2, "SPLASH", 3, 3, null, null, 9, 0)));
    }
}
