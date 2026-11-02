package com.rangoliwars.game.room;

import com.rangoliwars.engine.Game;
import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.engine.GameSettings;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.Phase;
import com.rangoliwars.engine.Player;
import com.rangoliwars.engine.bot.BotLevel;
import com.rangoliwars.game.dto.Dtos.CreateRoomRequest;
import com.rangoliwars.game.dto.Dtos.RoomSummary;
import com.rangoliwars.game.dto.Dtos.RoomView;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class GameRoomService {

    private static final List<String> BOT_NAMES = List.of("Diya", "Tara", "Chotu", "Rangu", "Mithai", "Gulal");
    private static final Duration FINISHED_TTL = Duration.ofMinutes(10);
    private static final Duration IDLE_LOBBY_TTL = Duration.ofMinutes(30);

    private final RoomRegistry registry;
    private final RoomBroadcaster broadcaster;
    private final TurnScheduler scheduler;
    private final MatchReporter reporter;

    public GameRoomService(RoomRegistry registry, RoomBroadcaster broadcaster,
                           TurnScheduler scheduler, MatchReporter reporter) {
        this.registry = registry;
        this.broadcaster = broadcaster;
        this.scheduler = scheduler;
        this.reporter = reporter;
    }

    public RoomView create(String username, CreateRoomRequest request) {
        CreateRoomRequest req = request == null ? new CreateRoomRequest(null, null, null, null) : request;
        GameSettings defaults = GameSettings.DEFAULT;
        GameSettings settings = GameSettings.of(
                orDefault(req.boardSize(), defaults.boardSize()),
                orDefault(req.maxRounds(), defaults.maxRounds()),
                orDefault(req.maxPlayers(), defaults.maxPlayers()));
        List<BotLevel> bots = req.bots() == null ? List.of() : req.bots();
        if (bots.size() >= settings.maxPlayers()) {
            throw new GameRuleException("Too many bots for this room size");
        }

        GameRoom room = registry.create(settings, username);
        Game game = room.game();
        // the scheduler must run first so broadcasts carry the new turn deadline
        game.addListener(scheduler.listenerFor(room));
        game.addListener(reporter.listenerFor(room));
        game.addListener(broadcaster.listenerFor(room));
        game.join(username, username);
        for (BotLevel level : bots) {
            addBotTo(room, level);
        }
        return RoomViews.view(room);
    }

    public RoomView join(String code, String username) {
        GameRoom room = registry.get(code);
        Game game = room.game();
        synchronized (game) {
            if (game.player(username).isEmpty()) {
                game.join(username, username);
            }
        }
        return RoomViews.view(room);
    }

    public RoomView leave(String code, String username) {
        GameRoom room = registry.get(code);
        Game game = room.game();
        synchronized (game) {
            game.leave(username);
            List<Player> humans = game.players().stream().filter(p -> !p.isBot()).toList();
            if (humans.isEmpty()) {
                game.end();
                registry.remove(room.code());
            } else if (username.equals(room.hostId())) {
                room.setHostId(humans.get(0).id());
                broadcaster.broadcast(room);
            }
        }
        return RoomViews.view(room);
    }

    public RoomView addBot(String code, String username, BotLevel level) {
        GameRoom room = registry.get(code);
        requireHost(room, username);
        addBotTo(room, level == null ? BotLevel.MEDIUM : level);
        return RoomViews.view(room);
    }

    public RoomView start(String code, String username) {
        GameRoom room = registry.get(code);
        requireHost(room, username);
        room.game().start();
        return RoomViews.view(room);
    }

    public RoomView play(String code, String username, Move move) {
        GameRoom room = registry.get(code);
        room.game().play(username, move);
        return RoomViews.view(room);
    }

    public RoomView view(String code) {
        return RoomViews.view(registry.get(code));
    }

    public void end(String code) {
        registry.get(code).game().end();
    }

    public List<RoomSummary> openRooms() {
        return registry.all().stream()
                .map(RoomViews::summary)
                .filter(s -> s.phase() == Phase.LOBBY && s.players() < s.maxPlayers())
                .sorted(Comparator.comparing(RoomSummary::createdAt).reversed())
                .toList();
    }

    public List<RoomSummary> allRooms() {
        return registry.all().stream()
                .map(RoomViews::summary)
                .sorted(Comparator.comparing(RoomSummary::createdAt).reversed())
                .toList();
    }

    public void connectionChanged(String code, String username, boolean connected) {
        GameRoom room;
        try {
            room = registry.get(code);
        } catch (RoomNotFoundException e) {
            return;
        }
        Game game = room.game();
        synchronized (game) {
            if (game.phase() != Phase.FINISHED && game.player(username).isPresent()) {
                game.setConnected(username, connected);
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void removeStaleRooms() {
        Instant now = Instant.now();
        for (GameRoom room : registry.all()) {
            Game game = room.game();
            boolean finishedLongAgo = game.phase() == Phase.FINISHED
                    && game.finishedAt() != null && game.finishedAt().plus(FINISHED_TTL).isBefore(now);
            boolean idleLobby = game.phase() == Phase.LOBBY && room.createdAt().plus(IDLE_LOBBY_TTL).isBefore(now);
            if (finishedLongAgo || idleLobby) {
                game.end();
                registry.remove(room.code());
            }
        }
    }

    private void addBotTo(GameRoom room, BotLevel level) {
        int n = room.nextBotNumber();
        String name = BOT_NAMES.get((n - 1) % BOT_NAMES.size()) + " ("
                + level.name().charAt(0) + level.name().substring(1).toLowerCase(Locale.ROOT) + " bot)";
        room.game().addBot("bot:" + n, name, level);
    }

    private static void requireHost(GameRoom room, String username) {
        if (!username.equals(room.hostId())) {
            throw new NotHostException();
        }
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
