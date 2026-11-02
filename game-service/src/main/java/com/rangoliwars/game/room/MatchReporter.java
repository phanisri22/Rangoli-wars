package com.rangoliwars.game.room;

import com.rangoliwars.engine.GameEvent;
import com.rangoliwars.engine.GameEventListener;
import com.rangoliwars.game.dto.Dtos.MatchReport;
import com.rangoliwars.game.dto.Dtos.MatchSaved;
import com.rangoliwars.game.security.SecurityConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends finished matches to leaderboard-service, retrying a few times. */
@Component
public class MatchReporter {

    private static final Logger log = LoggerFactory.getLogger(MatchReporter.class);
    private static final int ATTEMPTS = 3;

    private final RestClient client;
    private final RoomBroadcaster broadcaster;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "match-reporter");
        t.setDaemon(true);
        return t;
    });

    public MatchReporter(RestClient.Builder builder, RoomBroadcaster broadcaster,
                         @Value("${app.leaderboard-url}") String leaderboardUrl,
                         @Value("${app.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(leaderboardUrl)
                .defaultHeader(SecurityConfig.INTERNAL_KEY_HEADER, internalKey)
                .build();
        this.broadcaster = broadcaster;
    }

    public GameEventListener listenerFor(GameRoom room) {
        return (game, event) -> {
            if (event instanceof GameEvent.GameFinished && !game.history().isEmpty()) {
                MatchReport report = RoomViews.report(room);
                executor.submit(() -> send(room, report));
            }
        };
    }

    void send(GameRoom room, MatchReport report) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                MatchSaved saved = client.post().uri("/internal/matches").body(report).retrieve().body(MatchSaved.class);
                if (saved != null) {
                    room.setMatchId(saved.matchId());
                    broadcaster.broadcast(room);
                }
                return;
            } catch (RestClientException e) {
                log.warn("Saving match {} failed (attempt {}/{}): {}", room.code(), attempt, ATTEMPTS, e.getMessage());
                sleep(attempt * 2000L);
            }
        }
        log.error("Giving up on saving match {}", room.code());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
