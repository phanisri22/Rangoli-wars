package com.rangoliwars.engine.bot;

import com.rangoliwars.engine.Board;
import com.rangoliwars.engine.CardType;
import com.rangoliwars.engine.Move;

import java.util.List;

/** Strategy pattern: each difficulty picks moves differently. */
public interface BotStrategy {

    /** The board is a private copy; strategies may change it while thinking. */
    Move chooseMove(Board board, int player, List<CardType> hand, int playerCount);
}
