package com.rangoliwars.game.room;

public class NotHostException extends RuntimeException {

    public NotHostException() {
        super("Only the host can do that");
    }
}
