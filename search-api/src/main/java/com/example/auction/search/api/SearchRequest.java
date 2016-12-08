package com.example.auction.search.api;

import java.util.Optional;
import java.util.UUID;

public final class SearchRequest {

    private final Optional<String> query;
    private final Optional<String> category;
    private final Optional<String> country;
    private final Optional<String> state;
    private final Optional<String> city;
    private final Optional<UUID> creator;
    private final Optional<Integer> pageNo;
    private final Optional<Integer> pageSize;

    public SearchRequest(Optional<String> query, Optional<String> category, Optional<String> country, Optional<String> state, Optional<String> city, Optional<UUID> creator, Optional<Integer> pageNo, Optional<Integer> pageSize) {
        this.query = query;
        this.category = category;
        this.country = country;
        this.state = state;
        this.city = city;
        this.creator = creator;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
