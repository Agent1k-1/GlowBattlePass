package com.agent1k.glowbattlepass.service;

import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.storage.State;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public final class AuctionService {
    private final CurrencyService currency;
    public AuctionService(@NotNull CurrencyService currency) { this.currency = currency; }
    public void buy(@NotNull State state, @NotNull UUID buyer, @NotNull UUID listingId, long now) {
        Listing l = Optional.ofNullable(state.listings.get(listingId)).orElseThrow(() -> new IllegalArgumentException("Лот уже снят"));
        if (l.expires() <= now) throw new IllegalArgumentException("Срок лота истёк");
        currency.transfer(state.profile(buyer), state.profile(l.seller()), l.price());
        state.profile(buyer).mailbox().add(l.item()); state.listings.remove(l.id());
    }
    public void cancel(@NotNull State state, @NotNull UUID seller, @NotNull UUID listingId) {
        Listing l = Optional.ofNullable(state.listings.get(listingId)).orElseThrow(() -> new IllegalArgumentException("Лот уже снят"));
        if (!l.seller().equals(seller)) throw new IllegalArgumentException("Это чужой лот");
        state.profile(seller).mailbox().add(l.item()); state.listings.remove(l.id());
    }
    public void expire(@NotNull State state, long now) {
        List<Listing> expired = state.listings.values().stream().filter(l -> l.expires() <= now).collect(Collectors.toList());
        expired.forEach(l -> { state.profile(l.seller()).mailbox().add(l.item()); state.listings.remove(l.id()); });
    }
}
