package com.rangoliwars.game.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    MockMvc mvc;

    @Value("${app.jwt.secret}")
    String secret;

    @Value("${app.internal-api-key}")
    String internalKey;

    private String bearer(String username) {
        String token = Jwts.builder().subject(username)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        return "Bearer " + token;
    }

    private String createRoom(String username) throws Exception {
        String json = mvc.perform(post("/api/game/rooms").header("Authorization", bearer(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"boardSize\":7,\"maxPlayers\":2,\"bots\":[\"EASY\"]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.replaceAll(".*\"code\":\"([A-Z0-9]+)\".*", "$1");
    }

    @Test
    void requiresToken() throws Exception {
        mvc.perform(get("/api/game/rooms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/game/rooms").header("Authorization", "Bearer nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAndViewRoom() throws Exception {
        String code = createRoom("asha");
        mvc.perform(get("/api/game/rooms/" + code).header("Authorization", bearer("ravi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostId").value("asha"))
                .andExpect(jsonPath("$.players.length()").value(2))
                .andExpect(jsonPath("$.owners.length()").value(7));
    }

    @Test
    void ruleErrorsComeBackAsMessages() throws Exception {
        String code = createRoom("asha");
        mvc.perform(post("/api/game/rooms/" + code + "/moves").header("Authorization", bearer("asha"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PLACE\",\"row\":0,\"col\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The game has not started yet"));
        mvc.perform(post("/api/game/rooms/" + code + "/start").header("Authorization", bearer("ravi")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the host can do that"));
        mvc.perform(get("/api/game/rooms/QQQQQ").header("Authorization", bearer("asha")))
                .andExpect(status().isNotFound());
    }

    @Test
    void playOverRest() throws Exception {
        String code = createRoom("asha");
        mvc.perform(post("/api/game/rooms/" + code + "/start").header("Authorization", bearer("asha")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("PLAYING"));
        mvc.perform(post("/api/game/rooms/" + code + "/moves").header("Authorization", bearer("asha"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PLACE\",\"row\":3,\"col\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners[3][3]").value(0))
                .andExpect(jsonPath("$.lastMove.playerId").value("asha"));
    }

    @Test
    void internalEndpointsNeedServiceKey() throws Exception {
        mvc.perform(get("/internal/games").header("Authorization", bearer("asha")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/internal/games").header("X-Internal-Key", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/games").header("X-Internal-Key", internalKey))
                .andExpect(status().isOk());
    }
}
