package com.rangoliwars.game.room;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Marks players connected when they subscribe to their room and disconnected when their last socket closes. */
@Component
public class PresenceTracker {

    private static final String PREFIX = "/topic/rooms/";

    record Presence(String username, String code) {
    }

    private final Map<String, Set<Presence>> bySession = new ConcurrentHashMap<>();
    private final GameRoomService rooms;

    public PresenceTracker(GameRoomService rooms) {
        this.rooms = rooms;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Principal user = event.getUser();
        if (destination == null || user == null || !destination.startsWith(PREFIX)) {
            return;
        }
        String code = destination.substring(PREFIX.length());
        bySession.computeIfAbsent(accessor.getSessionId(), k -> ConcurrentHashMap.newKeySet())
                .add(new Presence(user.getName(), code));
        rooms.connectionChanged(code, user.getName(), true);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Set<Presence> gone = bySession.remove(event.getSessionId());
        if (gone == null) {
            return;
        }
        for (Presence presence : gone) {
            boolean stillConnected = bySession.values().stream().anyMatch(s -> s.contains(presence));
            if (!stillConnected) {
                rooms.connectionChanged(presence.code(), presence.username(), false);
            }
        }
    }
}
