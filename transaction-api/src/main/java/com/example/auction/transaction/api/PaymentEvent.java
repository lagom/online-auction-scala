package com.example.auction.transaction.api;

public abstract class PaymentEvent {

    private PaymentEvent() {}

    public static final class PaymentDetailsSubmitted extends PaymentEvent {

    }

    public static final class RefundInitiated extends PaymentEvent {

    }

}
