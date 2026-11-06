package com.agent1k.glowbattlepass.storage;

import com.agent1k.glowbattlepass.domain.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class State {
    public final Map<UUID, Profile> profiles = new LinkedHashMap<>();
    public final Map<UUID, Listing> listings = new LinkedHashMap<>();
    public final Map<UUID, Transfer> transfers = new LinkedHashMap<>();
    public final List<Booster> boosters = new ArrayList<>();
    public long seasonStarted = System.currentTimeMillis();
    public String dailyEpoch = "";
    public @NotNull Profile profile(@NotNull UUID id) {
        return Optional.ofNullable(profiles.get(id)).orElseThrow(() -> new IllegalArgumentException("Игрок ещё не заходил на сервер"));
    }
}
