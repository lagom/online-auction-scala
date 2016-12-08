package com.example.auction.transaction.api;

import java.time.Instant;
import java.util.UUID;

public final class TransactionMessage {

    private final UUID author;
    private final String message;
    private final Instant timeSent;

    public TransactionMessage(UUID author, String message, Instant timeSent) {
        this.author = author;
        this.message = message;
        this.timeSent = timeSent;
    }
}
