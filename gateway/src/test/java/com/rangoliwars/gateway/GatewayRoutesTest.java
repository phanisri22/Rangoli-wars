package com.rangoliwars.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutesTest {

    @Autowired
    RouteLocator routes;

    @Autowired
    WebTestClient client;

    @Test
    void allServicesAreRouted() {
        List<String> ids = routes.getRoutes().map(Route::getId).collectList().block();
        assertEquals(List.of("auth", "game-socket", "game", "leaderboard"), ids);
    }

    @Test
    void internalEndpointsAreNotExposed() {
        client.get().uri("/internal/games").exchange().expectStatus().isNotFound();
    }

    @Test
    void healthIsUp() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
