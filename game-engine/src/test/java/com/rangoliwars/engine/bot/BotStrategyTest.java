package com.rangoliwars.engine.bot;

import com.rangoliwars.engine.Board;
import com.rangoliwars.engine.CardType;
import com.rangoliwars.engine.Game;
import com.rangoliwars.engine.GameSettings;
import com.rangoliwars.engine.Move;
import com.rangoliwars.engine.MoveCommandFactory;
import com.rangoliwars.engine.MoveType;
import com.rangoliwars.engine.Phase;
import com.rangoliwars.engine.Player;
import com.rangoliwars.engine.Position;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotStrategyTest {

    private static void place(Board board, int player, int... coords) {
        for (int i = 0; i < coords.length; i += 2) {
            MoveCommandFactory.create(Move.place(coords[i], coords[i + 1])).execute(board, player);
        }
    }

    @ParameterizedTest
    @EnumSource(BotLevel.class)
    void everyBotPlaysAFullGameWithLegalMoves(BotLevel level) {
        Random random = new Random(11);
        Game game = new Game("BOTS1", GameSettings.of(7, 10, 3), random);
        game.addBot("b1", "One", level);
        game.addBot("b2", "Two", BotLevel.MEDIUM);
        game.addBot("b3", "Three", BotLevel.EASY);
        game.start();

        while (game.phase() == Phase.PLAYING) {
            Player p = game.currentPlayer().orElseThrow();
            BotStrategy strategy = BotStrategies.forLevel(p.botLevel(), random);
            Move move = strategy.chooseMove(game.boardCopy(), p.color(), p.hand(), 3);
            game.play(p.id(), move);
        }
        assertTrue(game.history().size() <= 30);
        assertFalse(game.winnerIds().isEmpty());
    }

    @Test
    void greedyClosesARing() {
        Board board = new Board(5);
        place(board, 0, 1, 2, 3, 2, 2, 1);
        Move move = new GreedyBot(new Random(1)).chooseMove(board, 0, List.of(), 2);
        assertEquals(Move.place(2, 3), move);
    }

    @Test
    void greedyPrefersBigSplash() {
        Board board = new Board(7);
        place(board, 1, 2, 2, 2, 3, 2, 4, 3, 2, 3, 3, 3, 4);
        Move move = new GreedyBot(new Random(1)).chooseMove(board, 0, List.of(CardType.SPLASH), 2);
        assertEquals(MoveType.SPLASH, move.type());
    }

    @Test
    void lookaheadBlocksOpponentRing() {
        Board board = new Board(5);
        place(board, 1, 1, 2, 3, 2, 2, 1);
        Move move = new LookaheadBot(new Random(1)).chooseMove(board, 0, List.of(), 2);
        assertEquals(new Position(2, 3), move.target());
    }

    @Test
    void botsLeaveTheBoardUnchanged() {
        Board board = new Board(5);
        place(board, 0, 1, 1);
        place(board, 1, 3, 3);
        int[][] before = board.ownersSnapshot();
        new LookaheadBot(new Random(2)).chooseMove(board, 0, List.of(CardType.values()), 2);
        assertArrayEquals(before, board.ownersSnapshot());
    }

    @Test
    void easyBotUsesCardsWhenNoEmptyCells() {
        Board board = new Board(5);
        for (Position p : board.cells()) {
            int owner = p.row() < 2 ? 0 : 1;
            MoveCommandFactory.create(new Move(MoveType.PLACE, p, null)).execute(board, owner);
        }
        Move move = new RandomBot(new Random(5)).chooseMove(board, 0, List.of(CardType.SWAP), 2);
        assertEquals(MoveType.SWAP, move.type());
    }
}
