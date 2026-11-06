package com.agent1k.glowbattlepass.domain;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public final class Booster {
    private final String owner, type, target;
    private final int percent;
    private final long expires;
    public Booster(@NotNull String owner, @NotNull String type, int percent, long expires, @NotNull String target) {
        this.owner = Objects.requireNonNull(owner); this.type = Objects.requireNonNull(type); this.target = Objects.requireNonNull(target);
        if (percent < 0 || percent > 10000) throw new IllegalArgumentException("Процент: 0–10000");
        this.percent = percent; this.expires = expires;
    }
    public @NotNull String owner() { return owner; }
    public @NotNull String type() { return type; }
    public @NotNull String target() { return target; }
    public int percent() { return percent; }
    public long expires() { return expires; }
}
