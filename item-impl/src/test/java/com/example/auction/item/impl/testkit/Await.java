package com.example.auction.item.impl.testkit;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
public class Await {

    public static <T> T result(CompletionStage<T> s) {
        try {
            return s.toCompletableFuture().get(5, SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

}
