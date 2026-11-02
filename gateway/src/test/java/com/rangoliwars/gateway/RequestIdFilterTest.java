package com.rangoliwars.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void addsRequestIdToRequestAndResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/game/rooms"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, e -> {
            forwarded.set(e);
            return Mono.empty();
        }).block();

        String id = exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER);
        assertNotNull(id);
        assertEquals(id, forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.HEADER));
    }

    @Test
    void keepsIncomingRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/me").header(RequestIdFilter.HEADER, "abc-123"));
        filter.filter(exchange, e -> Mono.empty()).block();
        assertEquals("abc-123", exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER));
    }
}
