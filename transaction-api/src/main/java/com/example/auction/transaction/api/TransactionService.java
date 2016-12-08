package com.example.auction.transaction.api;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;

import akka.Done;
import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import org.pcollections.PSequence;

import java.util.Optional;
import java.util.UUID;

public interface TransactionService extends Service {

    ServiceCall<TransactionMessage, Done> sendMessage(UUID itemId);

    ServiceCall<DeliveryInfo, Done> submitDeliveryDetails(UUID itemId);

    ServiceCall<Integer, Done> setDeliveryPrice(UUID itemId);

    ServiceCall<PaymentInfo, Done> submitPaymentDetails(UUID itemId);

    ServiceCall<NotUsed, Done> dispatchItem(UUID itemId);

    ServiceCall<NotUsed, Done> receiveItem(UUID itemId);

    ServiceCall<NotUsed, Done> initiateRefund(UUID itemId);

    //TopicCall<PaymentEvent> paymentEvents();

    //TopicCall<TransactionEvent> transactionEvents();

}
