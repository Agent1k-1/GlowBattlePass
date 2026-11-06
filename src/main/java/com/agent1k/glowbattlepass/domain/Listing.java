package com.agent1k.glowbattlepass.domain;

import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class Listing {
    private final UUID id, seller;
    private final String item, name;
    private final long price, expires;
    public Listing(@NotNull UUID id, @NotNull UUID seller, @NotNull String item, @NotNull String name, long price, long expires) {
        this.id = Objects.requireNonNull(id); this.seller = Objects.requireNonNull(seller);
        this.item = Objects.requireNonNull(item); this.name = Objects.requireNonNull(name);
        if (price <= 0) throw new IllegalArgumentException("Цена должна быть положительной");
        this.price = price; this.expires = expires;
    }
    public @NotNull UUID id() { return id; }
    public @NotNull UUID seller() { return seller; }
    public @NotNull String item() { return item; }
    public @NotNull String name() { return name; }
    public long price() { return price; }
    public long expires() { return expires; }
}
