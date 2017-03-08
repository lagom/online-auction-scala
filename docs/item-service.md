# Item service

Manages the description and auction status (created, auction, completed, cancelled) of an item.

## Queries handled

* **getItem** - Gets an item by an ID.
* **getItemsForUser** - Gets a list of items in a provided status that are owned by a given user.

## Events emitted

* **AuctionStarted** (public) - When the auction is started, in response to **startAuction**.
* **ItemUpdated** (private) - When user editable fields on an item are updated in response to **createItem**.
* **AuctionCancelled** (private) - When the auction is cancelled, in response to **cancelAuction**.
* **AuctionFinished** (private) - When the auction is finished, in response to **BiddingFinished**.

Event emitted publically are published via a broker topic named `item-ItemEvent`.

## Commands handled

### External (user)

* **createItem** - Creates an item - emits **ItemUpdated**.
* **updateItem** - Updates user editable properties of an item, if allowed in the current state (eg, currency can't be updated after auction is started), emits **ItemUpdated**.
* **startAuction** - Starts the auction if current state allows it, emits **AuctionStarted**.
* **cancelAuction** (not supported) - Cancels the auction if current state allows it, emits **AuctionCancelled**.

## Events consumed

### Bidding service

* **BidPlaced** - Updates the current price of the item.
* **BiddingFinished** - Completes the auction if current state allows, emits **AuctionFinished**.


