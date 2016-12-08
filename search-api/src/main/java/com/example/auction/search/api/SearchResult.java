package com.example.auction.search.api;

import org.pcollections.PSequence;

public final class SearchResult {

    private final PSequence<SearchItem> items;
    private final int pageSize;
    private final int pageNo;
    private final int numPages;

    public SearchResult(PSequence<SearchItem> items, int pageSize, int pageNo, int numPages) {
        this.items = items;
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.numPages = numPages;
    }
}
