package com.rangoliwars.game.room;

import com.rangoliwars.engine.Game;
import com.rangoliwars.engine.Player;
import com.rangoliwars.game.dto.Dtos.MatchPlayer;
import com.rangoliwars.game.dto.Dtos.MatchReport;
import com.rangoliwars.game.dto.Dtos.MoveView;
import com.rangoliwars.game.dto.Dtos.PlayerView;
import com.rangoliwars.game.dto.Dtos.RoomSummary;
import com.rangoliwars.game.dto.Dtos.RoomView;

import java.util.List;
import java.util.Map;

/** Builds API views from a room. Each view is taken under the game lock so it is consistent. */
public final class RoomViews {

    private RoomViews() {
    }

    public static RoomView view(GameRoom room) {
        Game game = room.game();
        synchronized (game) {
            Map<String, Integer> scores = game.scores();
            List<PlayerView> players = game.players().stream()
                    .map(p -> new PlayerView(p.id(), p.name(), p.color(), p.isBot(), p.botLevel(),
                            p.isConnected(), scores.get(p.id()), p.hand()))
                    .toList();
            return new RoomView(
                    game.code(), game.phase(), room.hostId(),
                    game.settings().boardSize(), game.settings().maxRounds(), game.settings().maxPlayers(),
                    game.round(), game.turnNumber(),
                    game.currentPlayer().map(Player::id).orElse(null), room.turnDeadline(),
                    players, game.ownersSnapshot(), game.shieldsSnapshot(),
                    game.lastMove().map(MoveView::of).orElse(null),
                    game.winnerIds(), game.endedEarly(), room.matchId());
        }
    }

    public static RoomSummary summary(GameRoom room) {
        Game game = room.game();
        synchronized (game) {
            return new RoomSummary(game.code(), game.phase(), room.hostId(), game.players().size(),
                    game.settings().maxPlayers(), game.settings().boardSize(), game.round(),
                    game.settings().maxRounds(), room.createdAt());
        }
    }

    public static MatchReport report(GameRoom room) {
        Game game = room.game();
        synchronized (game) {
            Map<String, Integer> scores = game.scores();
            List<MatchPlayer> players = game.players().stream()
                    .map(p -> {
                        int score = scores.get(p.id());
                        int placement = 1 + (int) scores.values().stream().filter(s -> s > score).count();
                        return new MatchPlayer(p.isBot() ? null : p.id(), p.name(), p.color(), score,
                                p.isBot(), p.botLevel(), placement);
                    })
                    .toList();
            List<MoveView> moves = game.history().stream().map(MoveView::of).toList();
            return new MatchReport(game.code(), game.settings().boardSize(), game.settings().maxRounds(),
                    game.startedAt(), game.finishedAt(), game.endedEarly(), players, moves);
        }
    }
}
