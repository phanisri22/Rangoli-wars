package com.rangoliwars.leaderboard.repository;

import com.rangoliwars.leaderboard.model.Models.MatchReport;
import com.rangoliwars.leaderboard.model.Models.MatchSummary;
import com.rangoliwars.leaderboard.model.Models.MoveEntry;
import com.rangoliwars.leaderboard.model.Models.PlayerResult;
import com.rangoliwars.leaderboard.model.Models.ReportMove;
import com.rangoliwars.leaderboard.model.Models.ReportPlayer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MatchRepository {

    private static final String MATCH_WITH_PLAYERS = """
            SELECT m.id, m.room_code, m.board_size, m.max_rounds, m.started_at, m.finished_at,
                   m.ended_early, m.total_moves,
                   p.color, p.username, p.display_name, p.is_bot, p.bot_level, p.score, p.placement
            FROM matches m
            JOIN match_players p ON p.match_id = m.id
            """;

    private final JdbcTemplate jdbc;

    public MatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(MatchReport report) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO matches (room_code, board_size, max_rounds, started_at, finished_at,
                                         ended_early, total_moves)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, new String[] {"id"});
            ps.setString(1, report.roomCode());
            ps.setInt(2, report.boardSize());
            ps.setInt(3, report.maxRounds());
            ps.setTimestamp(4, report.startedAt() == null ? null : Timestamp.from(report.startedAt()));
            ps.setTimestamp(5, Timestamp.from(report.finishedAt()));
            ps.setInt(6, report.endedEarly() ? 1 : 0);
            ps.setInt(7, report.moves().size());
            return ps;
        }, keys);
        long matchId = keys.getKey().longValue();

        jdbc.batchUpdate("""
                INSERT INTO match_players (match_id, color, username, display_name, is_bot, bot_level, score, placement)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, report.players(), report.players().size(), (ps, p) -> bindPlayer(ps, matchId, p));

        jdbc.batchUpdate("""
                INSERT INTO match_moves (match_id, turn_no, round_no, color, move_type, r1, c1, r2, c2, gained, captured)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, report.moves(), 200, (ps, m) -> bindMove(ps, matchId, m));
        return matchId;
    }

    public Optional<MatchSummary> find(long id) {
        return group(jdbc.query(MATCH_WITH_PLAYERS + " WHERE m.id = ? ORDER BY p.color", this::row, id))
                .stream().findFirst();
    }

    public List<MatchSummary> recent(int limit) {
        return group(jdbc.query(MATCH_WITH_PLAYERS + """
                WHERE m.id IN (SELECT id FROM matches ORDER BY finished_at DESC FETCH FIRST ? ROWS ONLY)
                ORDER BY m.finished_at DESC, m.id DESC, p.color
                """, this::row, limit));
    }

    public List<MatchSummary> forPlayer(String username, int limit) {
        return group(jdbc.query(MATCH_WITH_PLAYERS + """
                WHERE m.id IN (
                    SELECT mp.match_id FROM match_players mp JOIN matches mm ON mm.id = mp.match_id
                    WHERE mp.username = ? ORDER BY mm.finished_at DESC FETCH FIRST ? ROWS ONLY)
                ORDER BY m.finished_at DESC, m.id DESC, p.color
                """, this::row, username, limit));
    }

    public List<MoveEntry> moves(long matchId) {
        return jdbc.query("""
                SELECT turn_no, round_no, color, move_type, r1, c1, r2, c2, gained, captured
                FROM match_moves WHERE match_id = ? ORDER BY turn_no
                """, (rs, i) -> new MoveEntry(rs.getInt("turn_no"), rs.getInt("round_no"), rs.getInt("color"),
                rs.getString("move_type"), rs.getInt("r1"), rs.getInt("c1"),
                nullableInt(rs, "r2"), nullableInt(rs, "c2"), rs.getInt("gained"), rs.getInt("captured")), matchId);
    }

    // ---- mapping ----

    private record Row(MatchSummary match, PlayerResult player) {
    }

    private Row row(ResultSet rs, int i) throws SQLException {
        Timestamp started = rs.getTimestamp("started_at");
        MatchSummary match = new MatchSummary(rs.getLong("id"), rs.getString("room_code"), rs.getInt("board_size"),
                rs.getInt("max_rounds"), started == null ? null : started.toInstant(),
                rs.getTimestamp("finished_at").toInstant(), rs.getInt("ended_early") == 1,
                rs.getInt("total_moves"), List.of());
        PlayerResult player = new PlayerResult(rs.getInt("color"), rs.getString("username"),
                rs.getString("display_name"), rs.getInt("is_bot") == 1, rs.getString("bot_level"),
                rs.getInt("score"), rs.getInt("placement"));
        return new Row(match, player);
    }

    /** Folds one row per player back into one summary per match, keeping query order. */
    private static List<MatchSummary> group(List<Row> rows) {
        Map<Long, MatchSummary> matches = new LinkedHashMap<>();
        Map<Long, List<PlayerResult>> players = new LinkedHashMap<>();
        for (Row r : rows) {
            matches.putIfAbsent(r.match().id(), r.match());
            players.computeIfAbsent(r.match().id(), k -> new ArrayList<>()).add(r.player());
        }
        return matches.values().stream()
                .map(m -> new MatchSummary(m.id(), m.roomCode(), m.boardSize(), m.maxRounds(), m.startedAt(),
                        m.finishedAt(), m.endedEarly(), m.totalMoves(), List.copyOf(players.get(m.id()))))
                .toList();
    }

    private static void bindPlayer(PreparedStatement ps, long matchId, ReportPlayer p) throws SQLException {
        ps.setLong(1, matchId);
        ps.setInt(2, p.color());
        ps.setString(3, p.username());
        ps.setString(4, p.displayName());
        ps.setInt(5, p.bot() ? 1 : 0);
        ps.setString(6, p.botLevel());
        ps.setInt(7, p.score());
        ps.setInt(8, p.placement());
    }

    private static void bindMove(PreparedStatement ps, long matchId, ReportMove m) throws SQLException {
        ps.setLong(1, matchId);
        ps.setInt(2, m.turn());
        ps.setInt(3, m.round());
        ps.setInt(4, m.color());
        ps.setString(5, m.type());
        ps.setInt(6, m.row());
        ps.setInt(7, m.col());
        setNullableInt(ps, 8, m.row2());
        setNullableInt(ps, 9, m.col2());
        ps.setInt(10, m.gained());
        ps.setInt(11, m.captured());
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
