package com.example.auction.transaction.api;

public enum TransactionStatus {
    /**
     * Negotiating delivery details.
     */
    NEGOTIATING_DELIVERY,

    /**
     * Buyer has submitted payment details.
     */
    PAYMENT_SUBMITTED,

    /**
     * Payment is confirmed.
     */
    PAYMENT_CONFIRMED,

    /**
     * Item is dispatched.
     */
    ITEM_DISPATCHED,

    /**
     * Item has been received.
     */
    ITEM_RECEIVED,

    /**
     * The transaction has been cancelled.
     */
    CANCELLED,

    /**
     * The transaction is to be refunded.
     */
    REFUNDING,

    /**
     * The transaction has been refunded.
     */
    REFUNDED
}
