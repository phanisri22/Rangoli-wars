package com.rangoliwars.leaderboard.admin;

import com.rangoliwars.leaderboard.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

/** Reads live rooms from game-service for the admin page. */
@Component
public class GameServiceClient {

    public record LiveRoom(String code, String phase, String hostId, int players, int maxPlayers,
                           int boardSize, int round, int maxRounds, Instant createdAt) {
    }

    private final RestClient client;

    public GameServiceClient(RestClient.Builder builder,
                             @Value("${app.game-service-url}") String gameServiceUrl,
                             @Value("${app.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(gameServiceUrl)
                .defaultHeader(SecurityConfig.INTERNAL_KEY_HEADER, internalKey)
                .build();
    }

    public List<LiveRoom> rooms() {
        List<LiveRoom> rooms = client.get().uri("/internal/games").retrieve()
                .body(new ParameterizedTypeReference<List<LiveRoom>>() { });
        return rooms == null ? List.of() : rooms;
    }

    public void endRoom(String code) {
        client.post().uri("/internal/games/{code}/end", code).retrieve().toBodilessEntity();
    }
}
