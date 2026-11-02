package com.rangoliwars.game.dto;

import com.rangoliwars.engine.CardType;
import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.MoveRecord;
import com.rangoliwars.engine.MoveType;
import com.rangoliwars.engine.Phase;
import com.rangoliwars.engine.Position;
import com.rangoliwars.engine.bot.BotLevel;

import java.time.Instant;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record CreateRoomRequest(Integer boardSize, Integer maxRounds, Integer maxPlayers, List<BotLevel> bots) {
    }

    public record BotRequest(BotLevel level) {
    }

    public record MoveRequest(MoveType type, Integer row, Integer col, Integer row2, Integer col2) {

        public Move toMove() {
            if (type == null || row == null || col == null) {
                throw new GameRuleException("Move needs a type, row and col");
            }
            Position secondary = row2 != null && col2 != null ? new Position(row2, col2) : null;
            return new Move(type, new Position(row, col), secondary);
        }
    }

    public record PlayerView(
            String id, String name, int color, boolean bot, BotLevel botLevel,
            boolean connected, int score, List<CardType> cards) {
    }

    public record MoveView(
            int turn, int round, String playerId, int color, MoveType type,
            int row, int col, Integer row2, Integer col2, int gained, int captured) {

        public static MoveView of(MoveRecord r) {
            Position s = r.move().secondary();
            return new MoveView(r.turn(), r.round(), r.playerId(), r.playerColor(), r.move().type(),
                    r.move().target().row(), r.move().target().col(),
                    s == null ? null : s.row(), s == null ? null : s.col(), r.gained(), r.captured());
        }
    }

    public record RoomView(
            String code, Phase phase, String hostId, int boardSize, int maxRounds, int maxPlayers,
            int round, int turnNumber, String currentPlayerId, long turnDeadline,
            List<PlayerView> players, int[][] owners, boolean[][] shields, MoveView lastMove,
            List<String> winnerIds, boolean endedEarly, Long matchId) {
    }

    public record RoomSummary(
            String code, Phase phase, String hostId, int players, int maxPlayers,
            int boardSize, int round, int maxRounds, Instant createdAt) {
    }

    public record MatchPlayer(
            String username, String displayName, int color, int score, boolean bot, BotLevel botLevel, int placement) {
    }

    public record MatchReport(
            String roomCode, int boardSize, int maxRounds, Instant startedAt, Instant finishedAt,
            boolean endedEarly, List<MatchPlayer> players, List<MoveView> moves) {
    }

    public record MatchSaved(long matchId) {
    }

    public record ErrorResponse(String message) {
    }
}
