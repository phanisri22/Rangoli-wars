package com.rangoliwars.engine;

import java.util.Optional;

public enum MoveType {
    PLACE(null),
    SPLASH(CardType.SPLASH),
    SHIELD(CardType.SHIELD),
    SWAP(CardType.SWAP);

    private final CardType requiredCard;

    MoveType(CardType requiredCard) {
        this.requiredCard = requiredCard;
    }

    public Optional<CardType> requiredCard() {
        return Optional.ofNullable(requiredCard);
    }
}
