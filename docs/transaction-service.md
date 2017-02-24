# Transaction service

Handles the transaction of negotiating delivery info and making payment of an item that has completed an auction.

## Queries handled

* **getTransaction** - Gets a transaction by an items ID.
* **getTransactionsForUser** - Gets a list of all transactions that a given user is involved with.

## Events emitted

* **DeliveryByNegotiation** - When the buyer has selected by negotiation, in response to **submitDeliveryDetails**.
* **DeliveryPriceUpdated** - When the seller has updated the delivery price, in response to **setDeliveryPrice**.
* **PaymentDetailsSubmitted** - When payment details are submitted, in response to **submitPaymentDetails**.
* **PaymentConfirmed** - When payment has been confirmed, in response to payment service **ReceivedPayment**.
* **PaymentFailed** - When payment has failed, in response to payment service **FailedPayment**.
* **ItemDispatched** - When the item has been dispatched, in response to **dispatchItem**.
* **ItemReceived** - When the item has been received, in response to **receiveItem**.
* **MessageSent** - When a user has sent a message, in response to **sendMessage**.
* **RefundInitiated** - When the seller has initiated a refund, in response to **initiateRefund**.
* **RefundConfirmed** - When a refund has been confirmed.

## Commands handled

### External (user)

* **sendMessage** - Send a message to the other user, emits **MessageSent**.
* **submitDeliveryDetails** - Used by the buyer to submit delivery details, emits **DeliveryDetailsSubmitted**.
* **setDeliveryPrice** - Used by the seller to set the delivery price when delivery option is by negotiation, emits **DeliveryPriceUpdated**.
* **submitPaymentDetails** - Used by the buyer to submit payment details.
* **dispatchItem** - Used by the seller to say they have dispatched the item.
* **receiveItem** - Used by the buyer to say they have received the item.
* **initiateRefund** - Used by the seller to initiate a refund.

## Events consumed

### Item service

* **AuctionFinished** - Creates the transaction.

### Payment service

* **ReceivedPayment** - Indicates the payment has been made, emits **PaymentConfirmed**.
* **FailedPayment** - Indicates payment failed, emits **PaymentFailed**.
* **MadeRefund** - Indicates a refund was made, emits **RefundConfirmed**.

