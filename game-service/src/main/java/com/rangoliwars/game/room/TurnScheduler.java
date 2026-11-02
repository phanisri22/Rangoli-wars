package com.rangoliwars.game.room;

import com.rangoliwars.engine.Game;
import com.rangoliwars.engine.GameEvent;
import com.rangoliwars.engine.GameEventListener;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.Phase;
import com.rangoliwars.engine.Player;
import com.rangoliwars.engine.bot.BotLevel;
import com.rangoliwars.engine.bot.BotStrategies;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs the turn clock. Bots and disconnected players move after a short delay;
 * a human who runs out of time gets a MEDIUM bot move played for them.
 */
@Component
public class TurnScheduler {

    private static final Logger log = LoggerFactory.getLogger(TurnScheduler.class);

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "turn-timer");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final long turnMillis;
    private final long botDelayMillis;

    public TurnScheduler(@Value("${app.turn-seconds}") long turnSeconds,
                         @Value("${app.bot-delay-ms}") long botDelayMillis) {
        this.turnMillis = TimeUnit.SECONDS.toMillis(turnSeconds);
        this.botDelayMillis = botDelayMillis;
    }

    public GameEventListener listenerFor(GameRoom room) {
        return (game, event) -> {
            if (event instanceof GameEvent.TurnChanged) {
                schedule(room);
            } else if (event instanceof GameEvent.ConnectionChanged changed && isCurrent(game, changed.playerId())) {
                schedule(room);
            } else if (event instanceof GameEvent.GameFinished) {
                cancel(room.code());
                room.setTurnDeadline(0);
            }
        };
    }

    void schedule(GameRoom room) {
        cancel(room.code());
        Game game = room.game();
        Player current = game.currentPlayer().orElse(null);
        if (current == null) {
            return;
        }
        boolean automatic = current.isBot() || !current.isConnected();
        long delay = automatic ? botDelayMillis : turnMillis;
        room.setTurnDeadline(automatic ? 0 : System.currentTimeMillis() + delay);
        int expectedTurn = game.turnNumber();
        timers.put(room.code(), executor.schedule(
                () -> autoPlay(room, current.id(), expectedTurn), delay, TimeUnit.MILLISECONDS));
    }

    void cancel(String code) {
        ScheduledFuture<?> timer = timers.remove(code);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    private void autoPlay(GameRoom room, String playerId, int expectedTurn) {
        Game game = room.game();
        try {
            synchronized (game) {
                if (game.phase() != Phase.PLAYING || game.turnNumber() != expectedTurn) {
                    return;
                }
                Player p = game.player(playerId).orElseThrow();
                BotLevel level = p.isBot() ? p.botLevel() : BotLevel.MEDIUM;
                Move move = BotStrategies.forLevel(level, random)
                        .chooseMove(game.boardCopy(), p.color(), p.hand(), game.players().size());
                game.play(playerId, move);
            }
        } catch (RuntimeException e) {
            log.error("Auto move failed in room {}", room.code(), e);
        }
    }

    private static boolean isCurrent(Game game, String playerId) {
        return game.currentPlayer().map(p -> p.id().equals(playerId)).orElse(false);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
