package com.agent1k.glowbattlepass.quest;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public final class Quest {
    private final String id, material;
    private final QuestType type;
    private final int week;
    private final long target, experience, currency;
    public Quest(@NotNull String id, @NotNull QuestType type, @NotNull String material, int week, long target, long experience, long currency) {
        if (target <= 0 || experience < 0 || currency < 0 || week < 0 || id.contains(".")) throw new IllegalArgumentException("Некорректный квест: " + id);
        this.id = Objects.requireNonNull(id); this.type = Objects.requireNonNull(type); this.material = Objects.requireNonNull(material); this.week = week; this.target = target; this.experience = experience; this.currency = currency;
    }
    public @NotNull String id() { return id; }
    public @NotNull QuestType type() { return type; }
    public @NotNull String material() { return material; }
    public int week() { return week; }
    public long target() { return target; }
    public long experience() { return experience; }
    public long currency() { return currency; }
    public @NotNull String key() { return (week == 0 ? "daily" : "week-" + week) + ":" + id; }
}
