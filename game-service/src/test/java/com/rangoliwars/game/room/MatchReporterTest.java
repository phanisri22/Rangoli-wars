package com.rangoliwars.game.room;

import com.rangoliwars.engine.Game;
import com.rangoliwars.engine.GameSettings;
import com.rangoliwars.engine.Move;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MatchReporterTest {

    @Test
    void sendsReportWithInternalKeyAndStoresMatchId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RoomBroadcaster broadcaster = mock(RoomBroadcaster.class);
        MatchReporter reporter = new MatchReporter(builder, broadcaster, "http://leaderboard", "secret-key");

        Game game = new Game("ROOM1", GameSettings.of(5, 1, 2), new Random(1));
        GameRoom room = new GameRoom(game, "asha");
        game.join("asha", "asha");
        game.addBot("bot:1", "Diya", com.rangoliwars.engine.bot.BotLevel.EASY);
        game.start();
        game.play("asha", Move.place(0, 0));
        game.play("bot:1", Move.place(4, 4));

        server.expect(requestTo("http://leaderboard/internal/matches"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Key", "secret-key"))
                .andExpect(jsonPath("$.roomCode").value("ROOM1"))
                .andExpect(jsonPath("$.players[0].username").value("asha"))
                .andExpect(jsonPath("$.players[1].bot").value(true))
                .andExpect(jsonPath("$.moves.length()").value(2))
                .andRespond(withSuccess("{\"matchId\": 42}", MediaType.APPLICATION_JSON));

        reporter.send(room, RoomViews.report(room));

        server.verify();
        assertEquals(42L, room.matchId());
        verify(broadcaster).broadcast(room);
    }

    @Test
    void placementsShareTies() {
        Game game = new Game("ROOM2", GameSettings.of(5, 2, 3), new Random(1));
        GameRoom room = new GameRoom(game, "a");
        game.join("a", "a");
        game.join("b", "b");
        game.join("c", "c");
        game.start();
        game.play("a", Move.place(0, 0));
        game.play("b", Move.place(4, 4));
        game.play("c", Move.place(4, 0));
        game.play("a", Move.place(0, 1));
        var players = RoomViews.report(room).players();
        assertEquals(1, players.get(0).placement());
        assertEquals(2, players.get(1).placement());
        assertEquals(2, players.get(2).placement());
    }
}
