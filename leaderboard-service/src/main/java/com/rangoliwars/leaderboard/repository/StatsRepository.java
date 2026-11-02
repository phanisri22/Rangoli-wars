package com.rangoliwars.leaderboard.repository;

import com.rangoliwars.leaderboard.model.Models.PlayerStats;
import com.rangoliwars.leaderboard.service.RatingCalculator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class StatsRepository {

    private static final String RANKED = """
            SELECT username, games_played, wins, total_tiles, rating, last_played,
                   RANK() OVER (ORDER BY rating DESC, wins DESC) AS player_rank
            FROM player_stats
            """;

    private static final RowMapper<PlayerStats> MAPPER = (rs, i) -> {
        Timestamp last = rs.getTimestamp("last_played");
        return new PlayerStats(rs.getInt("player_rank"), rs.getString("username"), rs.getInt("games_played"),
                rs.getInt("wins"), rs.getInt("total_tiles"), rs.getInt("rating"),
                last == null ? null : last.toInstant());
    };

    private final JdbcTemplate jdbc;

    public StatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PlayerStats> top(int limit) {
        return jdbc.query(RANKED + " ORDER BY rating DESC, wins DESC, username FETCH FIRST ? ROWS ONLY",
                MAPPER, limit);
    }

    public Optional<PlayerStats> find(String username) {
        return jdbc.query("SELECT * FROM (" + RANKED + ") ranked WHERE ranked.username = ?", MAPPER, username)
                .stream().findFirst();
    }

    /** Adds one game to a player's totals, creating the row on their first game. */
    public void recordGame(String username, boolean won, int tiles, int ratingDelta, Instant playedAt) {
        Timestamp at = Timestamp.from(playedAt);
        int wins = won ? 1 : 0;
        int updated = jdbc.update("""
                UPDATE player_stats
                SET games_played = games_played + 1,
                    wins = wins + ?,
                    total_tiles = total_tiles + ?,
                    rating = GREATEST(?, rating + ?),
                    last_played = ?
                WHERE username = ?
                """, wins, tiles, RatingCalculator.MIN_RATING, ratingDelta, at, username);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO player_stats (username, games_played, wins, total_tiles, rating, last_played)
                    VALUES (?, 1, ?, ?, ?, ?)
                    """, username, wins, tiles,
                    Math.max(RatingCalculator.MIN_RATING, RatingCalculator.START_RATING + ratingDelta), at);
        }
    }
}
