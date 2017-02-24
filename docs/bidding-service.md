
# Bidding service

Manages bids on items.

## Queries handled

* **getBids** - Gets all the bids for an item.

## Events emitted

* **BidPlaced** - When a bid is placed, in response to **placeBid**.
* **BiddingFinished** - When bidding has finished, in response to **finishBidding**.

## Commands handled

### External (user)

* **placeBid** - Places a bid, if the bid is greater than the current bid, emits **BidPlaced**.

### Internal

* **finishBidding** - Triggered by scheduled task that polls a read side view of auctions to finish, emits **BiddingFinished**

## Events consumed

### Item service

* **AuctionStarted** - Creates a new auction for the item
* **AuctionCancelled** - Completes an auction prematurely

