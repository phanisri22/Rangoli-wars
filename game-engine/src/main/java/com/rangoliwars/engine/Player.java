package com.rangoliwars.engine;

import com.rangoliwars.engine.bot.BotLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Player {

    private final String id;
    private final String name;
    private final BotLevel botLevel;
    private final List<CardType> hand = new ArrayList<>();
    private int color;
    private boolean connected = true;
    private int turnsTaken;

    Player(String id, String name, int color, BotLevel botLevel) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.botLevel = botLevel;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Also the player's owner index on the board. */
    public int color() {
        return color;
    }

    /** Null for human players. */
    public BotLevel botLevel() {
        return botLevel;
    }

    public boolean isBot() {
        return botLevel != null;
    }

    public List<CardType> hand() {
        return Collections.unmodifiableList(hand);
    }

    public boolean isConnected() {
        return connected;
    }

    public int turnsTaken() {
        return turnsTaken;
    }

    void setColor(int color) {
        this.color = color;
    }

    void addCards(List<CardType> cards) {
        hand.addAll(cards);
    }

    void removeCard(CardType card) {
        hand.remove(card);
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    void incrementTurns() {
        turnsTaken++;
    }
}
