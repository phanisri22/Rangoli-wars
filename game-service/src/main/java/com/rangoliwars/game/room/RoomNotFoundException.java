package com.rangoliwars.game.room;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String code) {
        super("Room " + code + " was not found");
    }
}
