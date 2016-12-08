package com.example.auction.search.api;

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

public interface SearchService extends Service {

    ServiceCall<SearchRequest, SearchResult> search();

    ServiceCall<NotUsed, PSequence<SearchItem>> getUserAuctions();

    @Override
    default Descriptor descriptor() {
        return named("search");
    }
}
