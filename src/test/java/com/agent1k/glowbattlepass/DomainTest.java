package com.agent1k.glowbattlepass;

import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.service.*;
import com.agent1k.glowbattlepass.quest.*;
import com.agent1k.glowbattlepass.storage.State;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class DomainTest {
    private final CurrencyService currency = new CurrencyService();
    private final AuctionService auction = new AuctionService(currency);
    private Profile profile() { return new Profile(UUID.randomUUID(), "Player"); }
    @Test void insufficientTransferDoesNotMutateBalances() {
        Profile buyer = profile(), seller = profile(); buyer.balance(5); seller.balance(8);
        assertThrows(IllegalArgumentException.class, () -> currency.transfer(buyer, seller, 6));
        assertEquals(5, buyer.balance()); assertEquals(8, seller.balance());
    }
    @Test void overflowDoesNotDebitBuyer() {
        Profile buyer = profile(), seller = profile(); buyer.balance(10); seller.balance(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> currency.transfer(buyer, seller, 1)); assertEquals(10, buyer.balance());
    }
    @Test void negativeAmountCannotMintCurrency() { Profile p = profile(); assertThrows(IllegalArgumentException.class, () -> currency.take(p, -1)); assertEquals(0, p.balance()); }
    @Test void buyingMovesItemToMailAndCreditsOfflineSellerOnce() {
        State s = new State(); Profile buyer = profile(), seller = profile(); buyer.balance(100); s.profiles.put(buyer.id(), buyer); s.profiles.put(seller.id(), seller);
        Listing l = new Listing(UUID.randomUUID(), seller.id(), "encoded-item", "stone", 25, 200); s.listings.put(l.id(), l);
        auction.buy(s, buyer.id(), l.id(), 100);
        assertEquals(75, buyer.balance()); assertEquals(25, seller.balance()); assertEquals(Collections.singletonList("encoded-item"), buyer.mailbox());
        assertThrows(IllegalArgumentException.class, () -> auction.buy(s, buyer.id(), l.id(), 100)); assertEquals(25, seller.balance());
    }
    @Test void expiredListingCannotBePurchasedAndReturnsToSeller() {
        State s = new State(); Profile buyer = profile(), seller = profile(); buyer.balance(100); s.profiles.put(buyer.id(), buyer); s.profiles.put(seller.id(), seller);
        Listing l = new Listing(UUID.randomUUID(), seller.id(), "item", "stone", 25, 100); s.listings.put(l.id(), l);
        assertThrows(IllegalArgumentException.class, () -> auction.buy(s, buyer.id(), l.id(), 100));
        auction.expire(s, 100); auction.expire(s, 100); assertEquals(Collections.singletonList("item"), seller.mailbox()); assertEquals(100, buyer.balance());
    }
    @Test void playerCannotCancelOtherPlayersListing() {
        State s = new State(); Profile owner = profile(); s.profiles.put(owner.id(), owner); Listing l = new Listing(UUID.randomUUID(), owner.id(), "item", "stone", 1, 200); s.listings.put(l.id(), l);
        assertThrows(IllegalArgumentException.class, () -> auction.cancel(s, UUID.randomUUID(), l.id())); assertEquals(1, s.listings.size());
    }
    @Test void questRewardsGrantedOnceWithMatchingBoosters() {
        State s = new State(); Profile p = profile(); Quest q = new Quest("stone", QuestType.BREAK, "STONE", 1, 2, 100, 15);
        s.boosters.add(new Booster("all", "experience", 50, Long.MAX_VALUE, "all")); s.boosters.add(new Booster("all", "experience", 100, Long.MAX_VALUE, "other"));
        QuestService service = new QuestService(); service.advance(s, p, q, 1); assertEquals(0, p.balance()); service.advance(s, p, q, 5); service.advance(s, p, q, 5);
        assertEquals(150, p.experience()); assertEquals(15, p.balance()); assertEquals(2L, p.progress().get(q.key()).longValue());
    }
    @Test void lockedWeeksCannotProgressWithoutBypass() {
        State s = new State(); Profile p = profile(); Quest q = new Quest("stone", QuestType.BREAK, "STONE", 2, 1, 1, 1); QuestService service = new QuestService();
        service.advance(s, p, q, 1); assertEquals(0, p.experience()); p.bypass(true); service.advance(s, p, q, 1); assertEquals(1, p.experience());
    }
    @Test void dailyRefreshPreservesWeeklyProgressAndBalance() {
        State s = new State(); Profile p = profile(); s.profiles.put(p.id(), p); p.balance(5); p.progress().put("daily:x", 1L); p.progress().put("week-1:x", 2L); p.completed().add("daily:x");
        new QuestService().refresh(s, true); assertEquals(5, p.balance()); assertFalse(p.progress().containsKey("daily:x")); assertEquals(2L, p.progress().get("week-1:x").longValue());
    }
    @Test void seasonResetPreservesCurrencyAndMailbox() { Profile p = profile(); p.balance(50); p.experience(100); p.mailbox().add("item"); p.resetSeason(); assertEquals(50, p.balance()); assertEquals(0, p.experience()); assertEquals(1, p.mailbox().size()); }
}
