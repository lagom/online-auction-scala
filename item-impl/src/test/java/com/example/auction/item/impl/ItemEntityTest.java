package com.example.auction.item.impl;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.example.auction.item.impl.PItemCommand.*;
import com.example.auction.item.impl.PItemEvent.AuctionFinished;
import com.example.auction.item.impl.PItemEvent.AuctionStarted;
import com.example.auction.item.impl.PItemEvent.ItemCreated;
import com.example.auction.item.impl.PItemEvent.PriceUpdated;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class ItemEntityTest {

    private static ActorSystem system;


    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }


    @After
    public void issues() {
        if (!driver.getAllIssues().isEmpty()) {
            driver.getAllIssues().forEach(System.out::println);
            fail("There were issues.");
        }
    }

    private UUID itemId = UUID.randomUUID();
    private UUID creatorId = UUID.randomUUID();
    private PItem pitem = new PItem(itemId, creatorId, "title", "desc", "EUR", 1, 10, Duration.ofMinutes(10));

    private PersistentEntityTestDriver<PItemCommand, PItemEvent, PItemState> driver =
            new PersistentEntityTestDriver<>(system, new PItemEntity(), itemId.toString());

    private PItemCommand createItem = new CreateItem(pitem);
    private PItemCommand startAuction = new StartAuction(creatorId);

    @Test
    public void shouldEmitEventWhenCreatingItem() {
        Outcome<PItemEvent, PItemState> outcome = driver.run(createItem);
        expectEvents(outcome, new ItemCreated(pitem));
        assertEquals(PItemStatus.CREATED, outcome.state().getStatus());
    }


    @Test
    public void shouldEmitEventWhenStartingAnAuction() {
        Outcome<PItemEvent, PItemState> outcome = driver.run(createItem, startAuction);

        Instant startInstant = outcome.state().getItem().get().getAuctionStart().get();
        expectEvents(outcome,
                new ItemCreated(pitem),
                new AuctionStarted(itemId, startInstant));
        assertEquals(PItemStatus.AUCTION, outcome.state().getStatus());
    }

    @Test(expected = PersistentEntity.InvalidCommandException.class)
    public void shouldForbidCommandWhenStartAuctionIsCommandedByADifferentUser() {
        driver.run(createItem);

        UUID hackerId = UUID.randomUUID();
        PItemCommand invalidStartAuction = new StartAuction(hackerId);
        Outcome<PItemEvent, PItemState> outcome = driver.run(invalidStartAuction);

        PersistentEntityTestDriver.Reply sideEffect = (PersistentEntityTestDriver.Reply) outcome.sideEffects().get(0);
        throw (PersistentEntity.InvalidCommandException) sideEffect.msg();
    }

    @Test
    public void shouldIgnoreDuplicateStartAuctionCommands() {
        Outcome<PItemEvent, PItemState> outcome = driver.run(createItem, startAuction, startAuction, startAuction);

        Instant startInstant = outcome.state().getItem().get().getAuctionStart().get();
        expectEvents(outcome,
                new ItemCreated(pitem),
                new AuctionStarted(itemId, startInstant));
        assertEquals(PItemStatus.AUCTION, outcome.state().getStatus());
    }

    @Test
    public void shouldEmitEventWhenUpdatingPrice() {
        UpdatePrice updatePrice1 = new UpdatePrice(10);
        UpdatePrice updatePrice2 = new UpdatePrice(20);

        // splitting commands into arrange phase and act phase makes it easier to assert emissions since only
        // the last batch is available in the outcome.
        driver.run(createItem, startAuction); //arrange
        Outcome<PItemEvent, PItemState> outcome = driver.run(updatePrice1, updatePrice2); // act

        expectEvents(outcome,
                new PriceUpdated(itemId, 10),
                new PriceUpdated(itemId, 20)
        );
    }

    @Test
    public void shouldEmitEventWhenFinishingAuction() {
        UpdatePrice updatePrice1 = new UpdatePrice(10);
        UUID winner = UUID.randomUUID();
        FinishAuction finish = new FinishAuction(Optional.of(winner), 20);

        driver.run(createItem, startAuction);// arrange a state
        Outcome<PItemEvent, PItemState> outcome = driver.run(updatePrice1, finish); // act

        expectEvents(outcome,
                new PriceUpdated(itemId, 10),
                new AuctionFinished(itemId, Optional.of(winner), 20)
        );
        assertEquals(PItemStatus.COMPLETED, outcome.state().getStatus());
    }

    @Test
    public void shouldRejectSilentlyToStartACompletedAuction() {
        UpdatePrice updatePrice1 = new UpdatePrice(10);
        UUID winner = UUID.randomUUID();
        FinishAuction finish = new FinishAuction(Optional.of(winner), 20);
        PItemCommand restart = new StartAuction(creatorId);

        Outcome<PItemEvent, PItemState> outcome = driver.run(createItem, startAuction, updatePrice1, finish, restart);

        assertEquals(PItemStatus.COMPLETED, outcome.state().getStatus());
    }

    @Test
    public void shouldReturnDefaultItemBeforeAuction() {
        GetItem getItem = GetItem.INSTANCE;
        Outcome<PItemEvent, PItemState> outcome = driver.run(createItem, getItem);

        assertEquals(Optional.of(pitem), outcome.getReplies().get(1));
    }

    @Test
    public void shouldReturnItemWithCurrentPriceDuringAuction() {
        int latestPrice = 23;
        UpdatePrice updatePrice1 = new UpdatePrice(latestPrice);
        driver.run(createItem, startAuction, updatePrice1);

        Optional<PItem> maybePItem = getItem();

        assertEquals(latestPrice, maybePItem.get().getPrice());
    }

    @Test
    public void shouldReturnItemWithWinningBidAfterAuction() {
        UpdatePrice updatePrice1 = new UpdatePrice(23);
        UUID winner = UUID.randomUUID();
        int winningPrice = 42;
        UpdatePrice updatePrice2 = new UpdatePrice(winningPrice);
        FinishAuction finish = new FinishAuction(Optional.of(winner), winningPrice);
        driver.run(createItem, startAuction, updatePrice1, updatePrice2, finish);

        Optional<PItem> maybePItem = getItem();

        assertEquals(winningPrice, maybePItem.get().getPrice());
    }

    private Optional<PItem> getItem() {
        GetItem getItem = GetItem.INSTANCE;
        Outcome<PItemEvent, PItemState> outcome = driver.run(getItem);
        return (Optional<PItem>)((PersistentEntityTestDriver.Reply) outcome.sideEffects().get(0)).msg();
    }


    //  ---------------------------------------------------------------------------------------------------------

    private <T> void expectEvents(Outcome<T, ?> outcome, T... expected) {
        if (!outcome.events().equals(Arrays.asList(expected))) {
            throw new AssertionError("Failed expectation. Expected [" + Arrays.asList(expected) + "] was not equal to [" + outcome.events() + "].");
        }
    }
}
