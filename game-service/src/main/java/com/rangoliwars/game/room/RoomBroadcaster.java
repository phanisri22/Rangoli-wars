package com.rangoliwars.game.room;

import com.rangoliwars.engine.GameEvent;
import com.rangoliwars.engine.GameEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Pushes the full room view to everyone subscribed to /topic/rooms/{code}. */
@Component
public class RoomBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RoomBroadcaster.class);

    private final SimpMessagingTemplate messaging;

    public RoomBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public static String topic(String code) {
        return "/topic/rooms/" + code;
    }

    public GameEventListener listenerFor(GameRoom room) {
        return (game, event) -> {
            // a TurnChanged or GameFinished event always follows these two
            if (!(event instanceof GameEvent.MovePlayed) && !(event instanceof GameEvent.GameStarted)) {
                broadcast(room);
            }
        };
    }

    public void broadcast(GameRoom room) {
        try {
            messaging.convertAndSend(topic(room.code()), RoomViews.view(room));
        } catch (RuntimeException e) {
            log.warn("Broadcast failed for room {}", room.code(), e);
        }
    }
}
