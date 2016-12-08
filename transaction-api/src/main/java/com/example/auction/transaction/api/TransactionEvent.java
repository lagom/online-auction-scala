package com.example.auction.transaction.api;

import java.util.UUID;

public abstract class TransactionEvent {

    private TransactionEvent() {}

    public static final class DeliveryByNegotiation extends TransactionEvent {

        private final UUID itemId;

        public DeliveryByNegotiation(UUID itemId) {
            this.itemId = itemId;
        }
    }

    public static final class DeliveryPriceUpdated extends TransactionEvent {

        private final UUID itemId;

        public DeliveryPriceUpdated(UUID itemId) {
            this.itemId = itemId;
        }
    }

    public static final class PaymentConfirmed extends TransactionEvent {

        private final UUID itemId;

        public PaymentConfirmed(UUID itemId) {
            this.itemId = itemId;
        }
    }

    public static final class PaymentFailed extends TransactionEvent {

        private final UUID itemId;

        public PaymentFailed(UUID itemId) {
            this.itemId = itemId;
        }
    }

    public static final class ItemDispatched extends TransactionEvent {

        private final UUID itemId;

        public ItemDispatched(UUID itemId) {
            this.itemId = itemId;
        }
    }

    public static final class ItemReceived extends TransactionEvent {

        private final UUID itemId;

        public ItemReceived(UUID itemId) {
            this.itemId = itemId;
        }
    }

    public static final class MessageSent extends TransactionEvent {

        private final UUID itemId;
        private final TransactionMessage message;

        public MessageSent(UUID itemId, TransactionMessage message) {
            this.itemId = itemId;
            this.message = message;
        }
    }

    public static final class RefundConfirmed extends TransactionEvent {

        private final UUID itemId;

        public RefundConfirmed(UUID itemId) {
            this.itemId = itemId;
        }
    }
}
