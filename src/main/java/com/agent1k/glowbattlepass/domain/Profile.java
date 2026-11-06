package com.agent1k.glowbattlepass.domain;

import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class Profile {
    private final UUID id;
    private String name;
    private long balance;
    private long experience;
    private String pass = "free";
    private boolean bypass;
    private final Map<String, Long> progress = new HashMap<>();
    private final Set<String> completed = new HashSet<>();
    private final Set<String> rewards = new HashSet<>();
    private final List<String> mailbox = new ArrayList<>();

    public Profile(@NotNull UUID id, @NotNull String name) { this.id = Objects.requireNonNull(id); this.name = Objects.requireNonNull(name); }
    public @NotNull UUID id() { return id; }
    public @NotNull String name() { return name; }
    public void name(@NotNull String value) { name = Objects.requireNonNull(value); }
    public long balance() { return balance; }
    public void balance(long value) { if (value < 0) throw new IllegalArgumentException("Недостаточно валюты"); balance = value; }
    public long experience() { return experience; }
    public void experience(long value) { if (value < 0) throw new IllegalArgumentException("Отрицательный опыт"); experience = value; }
    public @NotNull String pass() { return pass; }
    public void pass(@NotNull String value) { pass = Objects.requireNonNull(value); }
    public boolean bypass() { return bypass; }
    public void bypass(boolean value) { bypass = value; }
    public @NotNull Map<String, Long> progress() { return progress; }
    public @NotNull Set<String> completed() { return completed; }
    public @NotNull Set<String> rewards() { return rewards; }
    public @NotNull List<String> mailbox() { return mailbox; }
    public void resetSeason() { experience = 0; pass = "free"; bypass = false; progress.clear(); completed.clear(); rewards.clear(); }
}
