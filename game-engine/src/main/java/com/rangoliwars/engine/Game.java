package com.rangoliwars.engine;

import com.rangoliwars.engine.bot.BotLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One match of Rangoli Wars. All public methods are synchronized so a game can be
 * driven safely from WebSocket threads, turn timers and bots at the same time.
 */
public final class Game {

    private final String code;
    private final GameSettings settings;
    private final CardFactory cards;
    private final Board board;
    private final List<Player> players = new ArrayList<>();
    private final List<MoveRecord> history = new ArrayList<>();
    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();

    private GameState state = new LobbyState();
    private int currentIndex;
    private int turnNumber;
    private boolean endedEarly;
    private Instant startedAt;
    private Instant finishedAt;

    public Game(String code, GameSettings settings, Random random) {
        this.code = code;
        this.settings = settings;
        this.cards = new CardFactory(random);
        this.board = new Board(settings.boardSize());
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    // ---- actions ----

    public synchronized Player join(String id, String name) {
        return state.join(this, id, name, null);
    }

    public synchronized Player addBot(String id, String name, BotLevel level) {
        return state.join(this, id, name, level);
    }

    public synchronized void leave(String id) {
        state.leave(this, id);
    }

    public synchronized void start() {
        state.start(this);
    }

    public synchronized MoveResult play(String playerId, Move move) {
        return state.play(this, playerId, move);
    }

    /** Ends the game now: closes a lobby, or finishes a running game early. */
    public synchronized void end() {
        state.end(this);
    }

    public synchronized void setConnected(String playerId, boolean connected) {
        Player p = requirePlayer(playerId);
        if (p.isConnected() != connected) {
            p.setConnected(connected);
            fire(new GameEvent.ConnectionChanged(playerId, connected));
        }
    }

    // ---- queries ----

    public String code() {
        return code;
    }

    public GameSettings settings() {
        return settings;
    }

    public synchronized Phase phase() {
        return state.phase();
    }

    public synchronized List<Player> players() {
        return List.copyOf(players);
    }

    public synchronized Optional<Player> player(String id) {
        return players.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public synchronized Optional<Player> currentPlayer() {
        return state.phase() == Phase.PLAYING ? Optional.of(players.get(currentIndex)) : Optional.empty();
    }

    public synchronized int turnNumber() {
        return turnNumber;
    }

    /** 1-based round number; a round is one turn for every player. */
    public synchronized int round() {
        if (players.isEmpty()) {
            return 1;
        }
        return Math.min(turnNumber / players.size() + 1, settings.maxRounds());
    }

    public synchronized Board boardCopy() {
        return board.copy();
    }

    public synchronized int[][] ownersSnapshot() {
        return board.ownersSnapshot();
    }

    public synchronized boolean[][] shieldsSnapshot() {
        return board.shieldsSnapshot();
    }

    public synchronized Map<String, Integer> scores() {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Player p : players) {
            scores.put(p.id(), board.countOwnedBy(p.color()));
        }
        return scores;
    }

    /** Players with the highest score once the game is finished; empty if no move was made. */
    public synchronized List<String> winnerIds() {
        if (state.phase() != Phase.FINISHED || history.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> scores = scores();
        int best = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return scores.entrySet().stream()
                .filter(e -> e.getValue() == best)
                .map(Map.Entry::getKey)
                .toList();
    }

    public synchronized List<MoveRecord> history() {
        return List.copyOf(history);
    }

    public synchronized Optional<MoveRecord> lastMove() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }

    public synchronized List<Move> legalMoves(String playerId) {
        Player p = requirePlayer(playerId);
        return MoveGenerator.legalMoves(board, p.color(), p.hand());
    }

    public synchronized boolean endedEarly() {
        return endedEarly;
    }

    public synchronized Instant startedAt() {
        return startedAt;
    }

    public synchronized Instant finishedAt() {
        return finishedAt;
    }

    // ---- transitions, called by the states ----

    Player doJoin(String id, String name, BotLevel botLevel) {
        if (players.size() >= settings.maxPlayers()) {
            throw new GameRuleException("This room is full");
        }
        if (player(id).isPresent()) {
            throw new GameRuleException("You are already in this room");
        }
        Player p = new Player(id, name, players.size(), botLevel);
        players.add(p);
        fire(new GameEvent.PlayerJoined(id));
        return p;
    }

    void doLeave(String id) {
        players.remove(requirePlayer(id));
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setColor(i);
        }
        fire(new GameEvent.PlayerLeft(id));
    }

    void doStart() {
        if (players.size() < 2) {
            throw new GameRuleException("At least 2 players are needed to start");
        }
        for (Player p : players) {
            p.addCards(cards.deal(settings.startingCards()));
        }
        startedAt = Instant.now();
        currentIndex = 0;
        state = new PlayingState();
        fire(new GameEvent.GameStarted());
        fire(new GameEvent.TurnChanged(players.get(0).id(), 0));
    }

    MoveResult doPlay(String playerId, Move move) {
        Player p = requirePlayer(playerId);
        if (p.color() != currentIndex) {
            throw new GameRuleException("It is not your turn");
        }
        Optional<CardType> card = move.type().requiredCard();
        if (card.isPresent() && !p.hand().contains(card.get())) {
            throw new GameRuleException("You do not have a " + card.get() + " card");
        }

        MoveResult result = MoveCommandFactory.create(move).execute(board, p.color());
        card.ifPresent(p::removeCard);
        p.incrementTurns();
        if (p.turnsTaken() % settings.cardEveryTurns() == 0 && p.hand().size() < settings.maxHandSize()) {
            p.addCards(List.of(cards.draw()));
        }

        MoveRecord record = new MoveRecord(turnNumber + 1, round(), p.id(), p.color(), move,
                result.gained(), result.captured());
        history.add(record);
        turnNumber++;
        fire(new GameEvent.MovePlayed(record));

        if (board.isFull() || turnNumber >= settings.maxRounds() * players.size()) {
            finish(false);
        } else {
            currentIndex = (currentIndex + 1) % players.size();
            fire(new GameEvent.TurnChanged(players.get(currentIndex).id(), turnNumber));
        }
        return result;
    }

    void finish(boolean early) {
        endedEarly = early;
        finishedAt = Instant.now();
        state = new FinishedState();
        fire(new GameEvent.GameFinished(winnerIds(), early));
    }

    void close() {
        endedEarly = true;
        finishedAt = Instant.now();
        state = new FinishedState();
        fire(new GameEvent.GameFinished(List.of(), true));
    }

    private Player requirePlayer(String id) {
        return player(id).orElseThrow(() -> new GameRuleException("You are not in this room"));
    }

    private void fire(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onEvent(this, event);
        }
    }
}
