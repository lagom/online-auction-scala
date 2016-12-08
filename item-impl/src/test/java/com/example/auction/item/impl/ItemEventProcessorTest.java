package com.example.auction.item.impl;

import com.datastax.driver.core.utils.UUIDs;
import com.example.auction.item.api.ItemStatus;
import com.example.auction.item.api.ItemSummary;
import com.example.auction.item.api.PaginatedSequence;
import com.example.auction.item.impl.testkit.Await;
import com.example.auction.item.impl.testkit.DoNothingTopicFactory;
import com.example.auction.item.impl.testkit.ReadSideTestDriver;
import com.lightbend.lagom.internal.api.broker.TopicFactory;
import com.lightbend.lagom.javadsl.persistence.Offset;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.bind;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static org.junit.Assert.assertEquals;


public class ItemEventProcessorTest {

    private final static ServiceTest.Setup setup = defaultSetup().withCassandra(true)
            .configureBuilder(b ->
                    // by default, cassandra-query-journal delays propagation of events by 10sec. In test we're using
                    // a 1 node cluster so this delay is not necessary.
                    b.configure("cassandra-query-journal.eventual-consistency-delay", "0")
                            .overrides(bind(ReadSide.class).to(ReadSideTestDriver.class),
                                    bind(TopicFactory.class).to(DoNothingTopicFactory.class))
            );

    private static ServiceTest.TestServer testServer;

    @BeforeClass
    public static void beforeAll() {
        testServer = ServiceTest.startServer(setup);
    }

    @AfterClass
    public static void afterAll() {
        testServer.stop();
    }

    private ReadSideTestDriver testDriver = testServer.injector().instanceOf(ReadSideTestDriver.class);
    private ItemRepository itemRepository = testServer.injector().instanceOf(ItemRepository.class);

    private UUID creatorId = UUID.randomUUID();
    private UUID itemId = UUIDs.timeBased();
    private PItem item = new PItem(itemId, creatorId, "title", "desc", "USD", 10, 100, Duration.ofMinutes(10));

    private AtomicInteger offset;

    @Before
    public void restartOffset() {
        offset = new AtomicInteger(1);
    }

    @Test
    public void shouldCreateAnItem() throws InterruptedException, ExecutionException, TimeoutException {
        feed(new PItemEvent.ItemCreated(item));

        ItemStatus itemStatus = ItemStatus.CREATED;
        PaginatedSequence<ItemSummary> items = getItems(creatorId, itemStatus);
        assertEquals(1, items.getCount());
        ItemSummary expected =
                new ItemSummary(itemId, item.getTitle(), item.getCurrencyId(), item.getReservePrice(), item.getStatus().toItemStatus());
        assertEquals(expected, items.getItems().get(0));
    }

    @Test
    public void shouldUpdateAnItemWhenStartingTheAuction() throws InterruptedException, ExecutionException, TimeoutException {
        feed(new PItemEvent.ItemCreated(item));
        feed(new PItemEvent.AuctionStarted(itemId, Instant.now()));

        PaginatedSequence<ItemSummary> createdItems = getItems(creatorId, ItemStatus.CREATED);
        assertEquals(0, createdItems.getCount());
        PaginatedSequence<ItemSummary> ongoingItems = getItems(creatorId, ItemStatus.AUCTION);
        assertEquals(1, ongoingItems.getCount());

        ItemSummary expected = new ItemSummary(itemId, item.getTitle(), item.getCurrencyId(), item.getReservePrice(), ItemStatus.AUCTION);
        assertEquals(expected, ongoingItems.getItems().get(0));
    }

    @Test
    public void shouldNotUpdateAnItemOnAuctionWhenReceivingABid() throws InterruptedException, ExecutionException, TimeoutException {
        feed(new PItemEvent.ItemCreated(item));
        feed(new PItemEvent.AuctionStarted(itemId, Instant.now()));
        ItemSummary beforeUpdate = getItems(creatorId, ItemStatus.AUCTION).getItems().get(0);

        feed(new PItemEvent.PriceUpdated(itemId, 23));

        ItemSummary afterUpdate = getItems(creatorId, ItemStatus.AUCTION).getItems().get(0);
        assertEquals(beforeUpdate, afterUpdate);
    }


    @Test
    public void shouldUpdateAnItemWhenFinishingTheAuction() throws InterruptedException, ExecutionException, TimeoutException {
        feed(new PItemEvent.ItemCreated(item));
        feed(new PItemEvent.AuctionStarted(itemId, Instant.now()));
        feed(new PItemEvent.AuctionFinished(itemId, Optional.empty(), 10));

        PaginatedSequence<ItemSummary> createdItems = getItems(creatorId, ItemStatus.AUCTION);
        assertEquals(0, createdItems.getCount());
        PaginatedSequence<ItemSummary> ongoingItems = getItems(creatorId, ItemStatus.COMPLETED);
        assertEquals(1, ongoingItems.getCount());

        ItemSummary expected = new ItemSummary(itemId, item.getTitle(), item.getCurrencyId(), item.getReservePrice(), ItemStatus.COMPLETED);
        assertEquals(expected, ongoingItems.getItems().get(0));
    }


    @Test
    public void shouldPaginateItemRetrieval() throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < 35; i++) {
            feed(new PItemEvent.ItemCreated(buildFixture(UUIDs.timeBased(), i)));
        }

        PaginatedSequence<ItemSummary> createdItems = Await.result(itemRepository.getItemsForUser(creatorId, ItemStatus.CREATED, 2, 10));
        assertEquals(35, createdItems.getCount());
        assertEquals(10, createdItems.getItems().size());
        // default ordering is time DESC so page 2 of size 10 over a set of 35 returns item ids 5-14. On that seq, the fourth item is id=11
        assertEquals("title11", createdItems.getItems().get(3).getTitle());
    }


    private PItem buildFixture(UUID itemId, int id) {
        return new PItem(itemId, creatorId, "title" + id, "desc" + id, "USD", 10, 100, Duration.ofMinutes(10));

    }

    // ------------------------------------------------------------------------

    private PaginatedSequence<ItemSummary> getItems(UUID creatorId, ItemStatus itemStatus) throws InterruptedException, ExecutionException, TimeoutException {
        return Await.result(itemRepository.getItemsForUser(creatorId, itemStatus, 0, 10));
    }

    private void feed(PItemEvent itemCreated) throws InterruptedException, ExecutionException, TimeoutException {
        Await.result(testDriver.feed(itemCreated, Offset.sequence(offset.getAndIncrement())));
    }


}

