package com.rangoliwars.game.web;

import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.game.dto.Dtos.ErrorResponse;
import com.rangoliwars.game.dto.Dtos.MoveRequest;
import com.rangoliwars.game.room.GameRoomService;
import com.rangoliwars.game.room.RoomNotFoundException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameSocketController {

    private final GameRoomService rooms;

    public GameSocketController(GameRoomService rooms) {
        this.rooms = rooms;
    }

    @MessageMapping("/rooms/{code}/move")
    public void move(@DestinationVariable String code, @Payload MoveRequest move, Principal user) {
        rooms.play(code, user.getName(), move.toMove());
    }

    @MessageExceptionHandler({GameRuleException.class, RoomNotFoundException.class})
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse error(RuntimeException e) {
        return new ErrorResponse(e.getMessage());
    }
}
