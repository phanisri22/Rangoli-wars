package com.rangoliwars.leaderboard.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class Models {

    private Models() {
    }

    // ---- sent by game-service ----

    public record MatchReport(
            @NotBlank String roomCode,
            @Min(5) @Max(15) int boardSize,
            @Min(1) @Max(50) int maxRounds,
            Instant startedAt,
            @NotNull Instant finishedAt,
            boolean endedEarly,
            @NotNull @Size(min = 2, max = 4) List<@Valid ReportPlayer> players,
            @NotNull List<@Valid ReportMove> moves) {
    }

    public record ReportPlayer(
            String username, @NotBlank String displayName, int color, int score,
            boolean bot, String botLevel, @Min(1) @Max(4) int placement) {
    }

    public record ReportMove(
            int turn, int round, String playerId, int color, @NotBlank String type,
            int row, int col, Integer row2, Integer col2, int gained, int captured) {
    }

    public record MatchSaved(long matchId) {
    }

    // ---- read side ----

    public record PlayerStats(
            int rank, String username, int gamesPlayed, int wins, int totalTiles, int rating, Instant lastPlayed) {
    }

    public record PlayerResult(
            int color, String username, String displayName, boolean bot, String botLevel, int score, int placement) {
    }

    public record MatchSummary(
            long id, String roomCode, int boardSize, int maxRounds, Instant startedAt, Instant finishedAt,
            boolean endedEarly, int totalMoves, List<PlayerResult> players) {
    }

    public record MoveEntry(
            int turn, int round, int color, String type, int row, int col, Integer row2, Integer col2,
            int gained, int captured) {
    }

    public record MatchDetail(MatchSummary match, List<MoveEntry> moves) {
    }

    public record ErrorResponse(String message) {
    }
}
