package com.example.auction.search.api;

import java.time.Instant;
import java.util.UUID;

public final class SearchItem {

    private final UUID id;
    private final UUID creator;
    private final String title;
    private final String description;
    private final UUID categoryId;
    private final String currencyId;
    private final int price;
    private final Instant auctionStart;
    private final Instant auctionEnd;


    public SearchItem(UUID id, UUID creator, String title, String description, UUID categoryId, String currencyId, int price, Instant auctionStart, Instant auctionEnd) {
        this.id = id;
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.currencyId = currencyId;
        this.price = price;
        this.auctionStart = auctionStart;
        this.auctionEnd = auctionEnd;
    }
}
