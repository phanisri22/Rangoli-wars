package com.rangoliwars.game.room;

import com.rangoliwars.engine.Game;
import com.rangoliwars.engine.GameSettings;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomRegistry {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new SecureRandom();

    public GameRoom create(GameSettings settings, String hostId) {
        while (true) {
            String code = newCode();
            GameRoom room = new GameRoom(new Game(code, settings, new Random()), hostId);
            if (rooms.putIfAbsent(code, room) == null) {
                return room;
            }
        }
    }

    public GameRoom get(String code) {
        GameRoom room = rooms.get(code.toUpperCase());
        if (room == null) {
            throw new RoomNotFoundException(code);
        }
        return room;
    }

    public Collection<GameRoom> all() {
        return List.copyOf(rooms.values());
    }

    public void remove(String code) {
        rooms.remove(code);
    }

    private String newCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
