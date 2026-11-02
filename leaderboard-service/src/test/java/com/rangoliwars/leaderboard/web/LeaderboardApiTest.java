package com.rangoliwars.leaderboard.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rangoliwars.leaderboard.TestReports;
import com.rangoliwars.leaderboard.admin.GameServiceClient;
import com.rangoliwars.leaderboard.model.Models.MatchReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeaderboardApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    GameServiceClient games;

    private long record(MatchReport report) throws Exception {
        String json = mvc.perform(post("/internal/matches").header("X-Internal-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(report)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("matchId").asLong();
    }

    @Test
    void recordingAMatchUpdatesStats() throws Exception {
        record(TestReports.threePlayer("api_win", "api_lose"));
        record(TestReports.threePlayer("api_win", "api_lose"));

        mvc.perform(get("/api/leaderboard/players/API_WIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed").value(2))
                .andExpect(jsonPath("$.wins").value(2))
                .andExpect(jsonPath("$.totalTiles").value(24))
                .andExpect(jsonPath("$.rating").value(1050));
        mvc.perform(get("/api/leaderboard/players/api_lose"))
                .andExpect(jsonPath("$.rating").value(1010))
                .andExpect(jsonPath("$.wins").value(0));
    }

    @Test
    void topListIsRankedByRating() throws Exception {
        record(TestReports.threePlayer("top_a", "top_b"));
        mvc.perform(get("/api/leaderboard/top?limit=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[?(@.username == 'top_a')].rating").value(1025));
    }

    @Test
    void matchDetailAndReplay() throws Exception {
        long id = record(TestReports.threePlayer("rep_a", "rep_b"));
        mvc.perform(get("/api/leaderboard/matches/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.players.length()").value(3))
                .andExpect(jsonPath("$.match.players[2].bot").value(true))
                .andExpect(jsonPath("$.moves[1].type").value("SWAP"))
                .andExpect(jsonPath("$.moves[1].row2").value(0))
                .andExpect(jsonPath("$.moves[0].row2").doesNotExist());
        mvc.perform(get("/api/leaderboard/matches/" + id + "/replay.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(header().string("Content-Disposition", containsString("rangoli-replay-" + id)))
                .andExpect(content().string(containsString("<replay matchId=\"" + id + "\"")));
        mvc.perform(get("/api/leaderboard/players/rep_a/matches"))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/leaderboard/matches/recent"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownThingsAreNotFound() throws Exception {
        mvc.perform(get("/api/leaderboard/matches/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/leaderboard/players/nobody_here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No games recorded for nobody_here"));
    }

    @Test
    void internalEndpointNeedsKeyAndValidBody() throws Exception {
        String body = mapper.writeValueAsString(TestReports.threePlayer("k_a", "k_b"));
        mvc.perform(post("/internal/matches").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/matches").header("X-Internal-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/matches").header("X-Internal-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roomCode\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminPageNeedsLoginAndRendersJsp() throws Exception {
        when(games.rooms()).thenReturn(List.of());
        mvc.perform(get("/admin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin").with(httpBasic("admin", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin").with(httpBasic("admin", "test-pass")))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/jsp/admin.jsp"))
                .andExpect(model().attributeExists("rooms", "players", "matches"));
    }

    @Test
    void adminPageSurvivesGameServiceOutage() throws Exception {
        when(games.rooms()).thenThrow(new ResourceAccessException("down"));
        mvc.perform(get("/admin").with(httpBasic("admin", "test-pass")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("gameServiceDown", true));
    }

    @Test
    void adminCanEndARoom() throws Exception {
        mvc.perform(post("/admin/rooms/ABCDE/end").with(httpBasic("admin", "test-pass")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/rooms/ABCDE/end").with(httpBasic("admin", "test-pass")).with(csrf()))
                .andExpect(redirectedUrl("/admin"));
        verify(games).endRoom("ABCDE");
    }
}
