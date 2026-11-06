package com.agent1k.glowbattlepass.quest;

import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.storage.State;
import org.jetbrains.annotations.NotNull;
import java.time.*;
import java.util.*;

public final class QuestService {
    public void refresh(@NotNull State state, boolean force) {
        String day = LocalDate.now(ZoneOffset.UTC).toString();
        if (!force && day.equals(state.dailyEpoch)) return;
        state.dailyEpoch = day;
        state.profiles.values().forEach(p -> { p.progress().keySet().removeIf(k -> k.startsWith("daily:")); p.completed().removeIf(k -> k.startsWith("daily:")); });
    }
    public boolean unlocked(@NotNull State state, @NotNull Profile p, @NotNull Quest q) { return p.bypass() || q.week() <= 1 + (System.currentTimeMillis() - state.seasonStarted) / 604800000L; }
    public void advance(@NotNull State state, @NotNull Profile p, @NotNull Quest q, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Прогресс должен быть положительным");
        if (!unlocked(state, p, q) || p.completed().contains(q.key())) return;
        long current = Math.min(q.target(), p.progress().getOrDefault(q.key(), 0L));
        long next = current + Math.min(amount, q.target() - current); p.progress().put(q.key(), next);
        if (next < q.target()) return;
        long percent = state.boosters.stream().filter(b -> b.expires() > System.currentTimeMillis()).filter(b -> b.owner().equals("all") || b.owner().equals(p.id().toString())).filter(b -> b.target().equals("all") || b.target().equals(q.id()) || b.target().equals(q.key())).mapToLong(Booster::percent).sum();
        long xp = Math.addExact(q.experience(), Math.multiplyExact(q.experience(), percent) / 100);
        p.experience(Math.addExact(p.experience(), xp)); p.balance(Math.addExact(p.balance(), q.currency())); p.completed().add(q.key());
    }
}
