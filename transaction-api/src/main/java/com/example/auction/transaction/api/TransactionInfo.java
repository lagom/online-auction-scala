package com.example.auction.transaction.api;

import com.example.auction.item.api.Item;
import org.pcollections.PSequence;

public final class TransactionInfo {

    private final Item item;
    private final PSequence<TransactionMessage> messages;
    private final TransactionStatus status;
    private final DeliveryInfo deliveryInfo;
    private final int deliveryPrice;
    private final PaymentInfo paymentInfo;

    public TransactionInfo(Item item, PSequence<TransactionMessage> messages, TransactionStatus status, DeliveryInfo deliveryInfo, int deliveryPrice, PaymentInfo paymentInfo) {
        this.item = item;
        this.messages = messages;
        this.status = status;
        this.deliveryInfo = deliveryInfo;
        this.deliveryPrice = deliveryPrice;
        this.paymentInfo = paymentInfo;
    }
}
