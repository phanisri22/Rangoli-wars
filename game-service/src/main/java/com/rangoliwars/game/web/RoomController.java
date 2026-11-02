package com.rangoliwars.game.web;

import com.rangoliwars.game.dto.Dtos.BotRequest;
import com.rangoliwars.game.dto.Dtos.CreateRoomRequest;
import com.rangoliwars.game.dto.Dtos.MoveRequest;
import com.rangoliwars.game.dto.Dtos.RoomSummary;
import com.rangoliwars.game.dto.Dtos.RoomView;
import com.rangoliwars.game.room.GameRoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/game/rooms")
public class RoomController {

    private final GameRoomService rooms;

    public RoomController(GameRoomService rooms) {
        this.rooms = rooms;
    }

    @GetMapping
    public List<RoomSummary> openRooms() {
        return rooms.openRooms();
    }

    @PostMapping
    public RoomView create(@RequestBody(required = false) CreateRoomRequest request, Principal user) {
        return rooms.create(user.getName(), request);
    }

    @GetMapping("/{code}")
    public RoomView view(@PathVariable String code) {
        return rooms.view(code);
    }

    @PostMapping("/{code}/join")
    public RoomView join(@PathVariable String code, Principal user) {
        return rooms.join(code, user.getName());
    }

    @PostMapping("/{code}/leave")
    public RoomView leave(@PathVariable String code, Principal user) {
        return rooms.leave(code, user.getName());
    }

    @PostMapping("/{code}/bots")
    public RoomView addBot(@PathVariable String code, @RequestBody(required = false) BotRequest request,
                           Principal user) {
        return rooms.addBot(code, user.getName(), request == null ? null : request.level());
    }

    @PostMapping("/{code}/start")
    public RoomView start(@PathVariable String code, Principal user) {
        return rooms.start(code, user.getName());
    }

    /** REST fallback; the web client sends moves over STOMP. */
    @PostMapping("/{code}/moves")
    public RoomView play(@PathVariable String code, @RequestBody MoveRequest move, Principal user) {
        return rooms.play(code, user.getName(), move.toMove());
    }
}
