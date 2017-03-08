# Search service

Handles all item searching.

## Queries handled

* **search** - Search for items currently under auction matching a given criteria.
* **getUserAuctions** - Gets a list of all current auctions that a user is participating in by user ID.

## Events consumed

### Item service

* **ItemUpdated** - Creates or updates the details for an item in the search index
* **AuctionStarted** - Updates the status for an item to started
* **AuctionFinished** - Deletes an item from the search index
* **AuctionCancelled** - Deletes an item from the search index

### Bidding service

* **BidPlaced** - Updates the current price for an item, if it exists in the index


