package com.rangoliwars.game.room;

import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.Phase;
import com.rangoliwars.engine.bot.BotLevel;
import com.rangoliwars.game.dto.Dtos.CreateRoomRequest;
import com.rangoliwars.game.dto.Dtos.RoomView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameRoomServiceTest {

    private SimpMessagingTemplate messaging;
    private RoomRegistry registry;
    private GameRoomService service;

    @BeforeEach
    void setUp() {
        messaging = mock(SimpMessagingTemplate.class);
        registry = new RoomRegistry();
        MatchReporter reporter = mock(MatchReporter.class);
        when(reporter.listenerFor(any())).thenReturn((g, e) -> { });
        service = new GameRoomService(registry, new RoomBroadcaster(messaging),
                new TurnScheduler(30, 10), reporter);
    }

    private static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition not met in time");
            }
            Thread.sleep(10);
        }
    }

    private RoomView createWithBot() {
        return service.create("asha", new CreateRoomRequest(7, 5, 2, List.of(BotLevel.EASY)));
    }

    @Test
    void createAddsHostAndBots() {
        RoomView view = createWithBot();
        assertEquals("asha", view.hostId());
        assertEquals(2, view.players().size());
        assertTrue(view.players().get(1).bot());
        assertEquals("Diya (Easy bot)", view.players().get(1).name());
        assertEquals(Phase.LOBBY, view.phase());
    }

    @Test
    void defaultsApplyWhenBodyMissing() {
        RoomView view = service.create("asha", null);
        assertEquals(9, view.boardSize());
        assertEquals(4, view.maxPlayers());
    }

    @Test
    void rejectsTooManyBots() {
        assertThrows(GameRuleException.class, () -> service.create("asha",
                new CreateRoomRequest(7, 5, 2, List.of(BotLevel.EASY, BotLevel.HARD))));
    }

    @Test
    void onlyHostCanStartOrAddBots() {
        RoomView view = service.create("asha", new CreateRoomRequest(7, 5, 3, null));
        service.join(view.code(), "ravi");
        assertThrows(NotHostException.class, () -> service.start(view.code(), "ravi"));
        assertThrows(NotHostException.class, () -> service.addBot(view.code(), "ravi", BotLevel.EASY));
    }

    @Test
    void joiningTwiceIsHarmless() {
        RoomView view = service.create("asha", null);
        service.join(view.code(), "ravi");
        assertEquals(2, service.join(view.code(), "ravi").players().size());
    }

    @Test
    void codesAreCaseInsensitive() {
        RoomView view = service.create("asha", null);
        assertEquals(view.code(), service.view(view.code().toLowerCase()).code());
    }

    @Test
    void unknownRoomIsNotFound() {
        assertThrows(RoomNotFoundException.class, () -> service.view("ZZZZZ"));
    }

    @Test
    void botAnswersAfterHumanMove() throws InterruptedException {
        RoomView view = createWithBot();
        service.start(view.code(), "asha");
        service.play(view.code(), "asha", Move.place(0, 0));
        waitFor(() -> "asha".equals(service.view(view.code()).currentPlayerId()));
        RoomView after = service.view(view.code());
        assertEquals(2, after.turnNumber());
        assertTrue(after.turnDeadline() > System.currentTimeMillis());
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/rooms/" + view.code()), any(RoomView.class));
    }

    @Test
    void outOfTurnMoveIsRejected() {
        RoomView view = createWithBot();
        service.start(view.code(), "asha");
        service.play(view.code(), "asha", Move.place(0, 0));
        assertThrows(GameRuleException.class, () -> service.play(view.code(), "asha", Move.place(1, 1)));
    }

    @Test
    void disconnectedPlayerIsPlayedForAutomatically() throws InterruptedException {
        RoomView view = service.create("asha", new CreateRoomRequest(7, 5, 2, null));
        service.join(view.code(), "ravi");
        service.start(view.code(), "asha");
        service.connectionChanged(view.code(), "asha", false);
        waitFor(() -> "ravi".equals(service.view(view.code()).currentPlayerId()));
        assertFalse(service.view(view.code()).players().get(0).connected());
    }

    @Test
    void botOnlyGameRunsToTheEnd() throws InterruptedException {
        RoomView view = service.create("asha", new CreateRoomRequest(5, 3, 2, List.of(BotLevel.HARD)));
        service.start(view.code(), "asha");
        service.connectionChanged(view.code(), "asha", false);
        waitFor(() -> service.view(view.code()).phase() == Phase.FINISHED);
        RoomView done = service.view(view.code());
        assertFalse(done.winnerIds().isEmpty());
        assertEquals(0, done.turnDeadline());
    }

    @Test
    void hostLeavingHandsOverHost() {
        RoomView view = service.create("asha", null);
        service.join(view.code(), "ravi");
        assertEquals("ravi", service.leave(view.code(), "asha").hostId());
    }

    @Test
    void lastHumanLeavingClosesRoom() {
        RoomView view = createWithBot();
        service.leave(view.code(), "asha");
        assertThrows(RoomNotFoundException.class, () -> service.view(view.code()));
    }

    @Test
    void openRoomsListsOnlyJoinableLobbies() {
        RoomView open = service.create("asha", null);
        RoomView full = createWithBot();
        RoomView started = service.create("ravi", new CreateRoomRequest(7, 5, 3, List.of(BotLevel.EASY)));
        service.start(started.code(), "ravi");
        List<String> codes = service.openRooms().stream().map(r -> r.code()).toList();
        assertEquals(List.of(open.code()), codes);
        assertEquals(3, service.allRooms().size());
        assertFalse(codes.contains(full.code()));
    }

    @Test
    void endingGameFinishesIt() {
        RoomView view = createWithBot();
        service.start(view.code(), "asha");
        service.end(view.code());
        assertEquals(Phase.FINISHED, service.view(view.code()).phase());
    }
}
