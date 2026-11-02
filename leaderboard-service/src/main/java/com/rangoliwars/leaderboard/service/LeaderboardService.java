package com.rangoliwars.leaderboard.service;

import com.rangoliwars.leaderboard.model.Models.MatchDetail;
import com.rangoliwars.leaderboard.model.Models.MatchReport;
import com.rangoliwars.leaderboard.model.Models.MatchSummary;
import com.rangoliwars.leaderboard.model.Models.PlayerStats;
import com.rangoliwars.leaderboard.model.Models.ReportPlayer;
import com.rangoliwars.leaderboard.repository.MatchRepository;
import com.rangoliwars.leaderboard.repository.StatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class LeaderboardService {

    private static final int MAX_LIMIT = 100;

    private final MatchRepository matches;
    private final StatsRepository stats;

    public LeaderboardService(MatchRepository matches, StatsRepository stats) {
        this.matches = matches;
        this.stats = stats;
    }

    /** Saves the match, its players and moves, and updates human players' stats in one transaction. */
    @Transactional
    public long recordMatch(MatchReport report) {
        long matchId = matches.insert(report);
        int worst = report.players().stream().mapToInt(ReportPlayer::placement).max().orElse(1);
        for (ReportPlayer p : report.players()) {
            if (!p.bot() && p.username() != null) {
                stats.recordGame(p.username(), RatingCalculator.isWin(p.placement(), worst), p.score(),
                        RatingCalculator.delta(p.placement(), worst), report.finishedAt());
            }
        }
        return matchId;
    }

    public List<PlayerStats> top(int limit) {
        return stats.top(clamp(limit));
    }

    public Optional<PlayerStats> player(String username) {
        return stats.find(username.toLowerCase(Locale.ROOT));
    }

    public List<MatchSummary> recentMatches(int limit) {
        return matches.recent(clamp(limit));
    }

    public List<MatchSummary> playerMatches(String username, int limit) {
        return matches.forPlayer(username.toLowerCase(Locale.ROOT), clamp(limit));
    }

    public MatchDetail match(long id) {
        MatchSummary summary = matches.find(id).orElseThrow(() -> new NoSuchElementException("Match " + id + " was not found"));
        return new MatchDetail(summary, matches.moves(id));
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
