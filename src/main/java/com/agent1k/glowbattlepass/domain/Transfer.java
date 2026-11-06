package com.agent1k.glowbattlepass.domain;

import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class Transfer {
    public enum Direction { SALE, DELIVERY }
    private final UUID id, player;
    private final Direction direction;
    private final String item;
    private boolean active;
    public Transfer(@NotNull UUID id, @NotNull UUID player, @NotNull Direction direction, @NotNull String item, boolean active) {
        this.id = Objects.requireNonNull(id); this.player = Objects.requireNonNull(player); this.direction = Objects.requireNonNull(direction); this.item = Objects.requireNonNull(item); this.active = active;
    }
    public @NotNull UUID id() { return id; }
    public @NotNull UUID player() { return player; }
    public @NotNull Direction direction() { return direction; }
    public @NotNull String item() { return item; }
    public boolean active() { return active; }
    public void activate() { active = true; }
}
