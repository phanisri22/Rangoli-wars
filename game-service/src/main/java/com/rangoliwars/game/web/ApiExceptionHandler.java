package com.rangoliwars.game.web;

import com.rangoliwars.engine.GameRuleException;
import com.rangoliwars.game.dto.Dtos.ErrorResponse;
import com.rangoliwars.game.room.NotHostException;
import com.rangoliwars.game.room.RoomNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GameRuleException.class)
    ResponseEntity<ErrorResponse> rule(GameRuleException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable() {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed");
    }

    @ExceptionHandler(RoomNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(RoomNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NotHostException.class)
    ResponseEntity<ErrorResponse> notHost(NotHostException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
