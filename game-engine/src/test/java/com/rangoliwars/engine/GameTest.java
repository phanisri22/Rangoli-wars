package com.rangoliwars.engine;

import com.rangoliwars.engine.bot.BotLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTest {

    private Game game;
    private final List<GameEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        game = new Game("ABCDE", new GameSettings(5, 3, 3, 1, 2, 2), new Random(7));
        game.addListener((g, e) -> events.add(e));
    }

    private void startWithTwo() {
        game.join("asha", "Asha");
        game.join("ravi", "Ravi");
        game.start();
    }

    @Test
    void startsInLobby() {
        assertEquals(Phase.LOBBY, game.phase());
        assertTrue(game.currentPlayer().isEmpty());
    }

    @Test
    void joinAssignsColorsInOrder() {
        Player a = game.join("asha", "Asha");
        Player b = game.addBot("bot-1", "Bot", BotLevel.EASY);
        assertEquals(0, a.color());
        assertEquals(1, b.color());
        assertTrue(b.isBot());
    }

    @Test
    void rejectsDuplicateAndFullRoom() {
        game.join("asha", "Asha");
        assertThrows(GameRuleException.class, () -> game.join("asha", "Asha"));
        game.join("ravi", "Ravi");
        game.join("meera", "Meera");
        assertThrows(GameRuleException.class, () -> game.join("dev", "Dev"));
    }

    @Test
    void leavingRecolorsRemainingPlayers() {
        game.join("asha", "Asha");
        game.join("ravi", "Ravi");
        game.leave("asha");
        assertEquals(0, game.player("ravi").orElseThrow().color());
    }

    @Test
    void needsTwoPlayersToStart() {
        game.join("asha", "Asha");
        assertThrows(GameRuleException.class, game::start);
    }

    @Test
    void cannotPlayBeforeStartOrJoinAfter() {
        game.join("asha", "Asha");
        assertThrows(GameRuleException.class, () -> game.play("asha", Move.place(0, 0)));
        game.join("ravi", "Ravi");
        game.start();
        assertThrows(GameRuleException.class, () -> game.join("dev", "Dev"));
        assertThrows(GameRuleException.class, () -> game.leave("ravi"));
    }

    @Test
    void startDealsCardsAndFiresEvents() {
        startWithTwo();
        assertEquals(Phase.PLAYING, game.phase());
        assertEquals(1, game.player("asha").orElseThrow().hand().size());
        assertEquals("asha", game.currentPlayer().orElseThrow().id());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.GameStarted));
    }

    @Test
    void turnsAlternate() {
        startWithTwo();
        game.play("asha", Move.place(0, 0));
        assertEquals("ravi", game.currentPlayer().orElseThrow().id());
        assertThrows(GameRuleException.class, () -> game.play("asha", Move.place(0, 1)));
        game.play("ravi", Move.place(0, 1));
        assertEquals("asha", game.currentPlayer().orElseThrow().id());
        assertEquals(2, game.round());
    }

    @Test
    void cardMoveNeedsCardInHand() {
        startWithTwo();
        CardType held = game.player("asha").orElseThrow().hand().get(0);
        CardType missing = held == CardType.SPLASH ? CardType.SHIELD : CardType.SPLASH;
        Move move = missing == CardType.SPLASH ? Move.splash(2, 2) : Move.shield(0, 0);
        assertThrows(GameRuleException.class, () -> game.play("asha", move));
    }

    @Test
    void usingCardRemovesItAndCardsAreDrawnOnInterval() {
        game = new Game("ABCDE", new GameSettings(5, 5, 2, 1, 2, 2), new Random(3));
        game.join("asha", "Asha");
        game.join("ravi", "Ravi");
        game.start();
        Player asha = game.player("asha").orElseThrow();
        CardType card = asha.hand().get(0);

        game.play("asha", Move.place(1, 1));
        game.play("ravi", Move.place(4, 4));
        Move cardMove = switch (card) {
            case SPLASH -> Move.splash(2, 2);
            case SHIELD -> Move.shield(1, 1);
            case SWAP -> Move.place(0, 0);
        };
        game.play("asha", cardMove);

        int expected = card == CardType.SWAP ? 2 : 1;
        assertEquals(expected, asha.hand().size());
    }

    @Test
    void endsAfterMaxRoundsWithWinner() {
        startWithTwo();
        int[][] asha = {{0, 0}, {0, 2}, {0, 4}};
        int[][] ravi = {{4, 0}, {4, 2}};
        for (int i = 0; i < 3; i++) {
            game.play("asha", Move.place(asha[i][0], asha[i][1]));
            if (i < 2) {
                game.play("ravi", Move.place(ravi[i][0], ravi[i][1]));
            }
        }
        assertEquals("ravi", game.currentPlayer().orElseThrow().id());
        game.play("ravi", Move.place(4, 4));
        assertEquals(Phase.FINISHED, game.phase());
        assertEquals(6, game.history().size());
        assertEquals(List.of("asha", "ravi"), game.winnerIds());
        GameEvent last = events.get(events.size() - 1);
        assertInstanceOf(GameEvent.GameFinished.class, last);
        assertFalse(game.endedEarly());
    }

    @Test
    void endsWhenBoardIsFull() {
        game = new Game("FULL1", new GameSettings(5, 50, 2, 0, 50, 0), new Random(1));
        game.join("asha", "Asha");
        game.join("ravi", "Ravi");
        game.start();
        int turn = 0;
        while (game.phase() == Phase.PLAYING) {
            String id = game.currentPlayer().orElseThrow().id();
            Move move = game.legalMoves(id).get(0);
            game.play(id, move);
            turn++;
        }
        assertTrue(turn <= 25);
        assertTrue(game.boardCopy().isFull());
        assertFalse(game.winnerIds().isEmpty());
    }

    @Test
    void endingEarlyFinishesTheGame() {
        startWithTwo();
        game.play("asha", Move.place(0, 0));
        game.end();
        assertEquals(Phase.FINISHED, game.phase());
        assertTrue(game.endedEarly());
        assertEquals(List.of("asha"), game.winnerIds());
        assertThrows(GameRuleException.class, () -> game.play("ravi", Move.place(1, 1)));
    }

    @Test
    void closingLobbyHasNoWinners() {
        game.join("asha", "Asha");
        game.end();
        assertEquals(Phase.FINISHED, game.phase());
        assertTrue(game.winnerIds().isEmpty());
    }

    @Test
    void connectionChangeFiresOnlyOnChange() {
        game.join("asha", "Asha");
        events.clear();
        game.setConnected("asha", true);
        assertTrue(events.isEmpty());
        game.setConnected("asha", false);
        assertEquals(List.of(new GameEvent.ConnectionChanged("asha", false)), events);
    }

    @Test
    void scoresTrackTiles() {
        startWithTwo();
        game.play("asha", Move.place(0, 0));
        game.play("ravi", Move.place(1, 1));
        game.play("asha", Move.place(0, 1));
        assertEquals(2, game.scores().get("asha"));
        assertEquals(1, game.scores().get("ravi"));
    }
}
