package com.rangoliwars.game.web;

import com.rangoliwars.game.dto.Dtos.RoomSummary;
import com.rangoliwars.game.room.GameRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Used by the admin page in leaderboard-service. Not routed by the gateway. */
@RestController
@RequestMapping("/internal/games")
public class InternalController {

    private final GameRoomService rooms;

    public InternalController(GameRoomService rooms) {
        this.rooms = rooms;
    }

    @GetMapping
    public List<RoomSummary> all() {
        return rooms.allRooms();
    }

    @PostMapping("/{code}/end")
    public ResponseEntity<Void> end(@PathVariable String code) {
        rooms.end(code);
        return ResponseEntity.noContent().build();
    }
}
