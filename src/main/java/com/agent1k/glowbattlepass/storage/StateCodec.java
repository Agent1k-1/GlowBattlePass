package com.agent1k.glowbattlepass.storage;

import com.agent1k.glowbattlepass.domain.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class StateCodec {
    public @NotNull String encode(@NotNull State state) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("started", state.seasonStarted); y.set("daily", state.dailyEpoch);
        state.profiles.forEach((id, p) -> {
            String k = "players." + id;
            y.set(k + ".name", p.name()); y.set(k + ".balance", p.balance()); y.set(k + ".experience", p.experience());
            y.set(k + ".pass", p.pass()); y.set(k + ".bypass", p.bypass()); y.set(k + ".progress", p.progress());
            y.set(k + ".completed", new ArrayList<>(p.completed())); y.set(k + ".rewards", new ArrayList<>(p.rewards())); y.set(k + ".mailbox", p.mailbox());
        });
        state.listings.forEach((id, l) -> {
            String k = "listings." + id;
            y.set(k + ".seller", l.seller().toString()); y.set(k + ".item", l.item()); y.set(k + ".name", l.name()); y.set(k + ".price", l.price()); y.set(k + ".expires", l.expires());
        });
        for (int i = 0; i < state.boosters.size(); i++) {
            Booster b = state.boosters.get(i); String k = "boosters." + i;
            y.set(k + ".owner", b.owner()); y.set(k + ".type", b.type()); y.set(k + ".target", b.target()); y.set(k + ".percent", b.percent()); y.set(k + ".expires", b.expires());
        }
        state.transfers.forEach((id, t) -> {
            String k = "transfers." + id;
            y.set(k + ".player", t.player().toString()); y.set(k + ".direction", t.direction().name()); y.set(k + ".item", t.item()); y.set(k + ".active", t.active());
        });
        return y.saveToString();
    }
    public @NotNull State decode(@NotNull String data) {
        YamlConfiguration y = new YamlConfiguration();
        try { y.loadFromString(data); } catch (Exception e) { throw new IllegalStateException("Повреждено хранилище", e); }
        State s = new State(); s.seasonStarted = y.getLong("started", s.seasonStarted); s.dailyEpoch = y.getString("daily", "");
        section(y, "players").ifPresent(root -> root.getKeys(false).forEach(id -> {
            String k = "players." + id; Profile p = new Profile(UUID.fromString(id), y.getString(k + ".name", id));
            p.balance(y.getLong(k + ".balance")); p.experience(y.getLong(k + ".experience")); p.pass(y.getString(k + ".pass", "free")); p.bypass(y.getBoolean(k + ".bypass"));
            section(y, k + ".progress").ifPresent(q -> q.getKeys(false).forEach(key -> p.progress().put(key, q.getLong(key))));
            p.completed().addAll(y.getStringList(k + ".completed")); p.rewards().addAll(y.getStringList(k + ".rewards")); p.mailbox().addAll(y.getStringList(k + ".mailbox")); s.profiles.put(p.id(), p);
        }));
        section(y, "listings").ifPresent(root -> root.getKeys(false).forEach(id -> {
            String k = "listings." + id; Listing l = new Listing(UUID.fromString(id), UUID.fromString(y.getString(k + ".seller", "")), y.getString(k + ".item", ""), y.getString(k + ".name", ""), y.getLong(k + ".price"), y.getLong(k + ".expires")); s.listings.put(l.id(), l);
        }));
        section(y, "boosters").ifPresent(root -> root.getKeys(false).forEach(id -> {
            String k = "boosters." + id; s.boosters.add(new Booster(y.getString(k + ".owner", "all"), y.getString(k + ".type", "experience"), y.getInt(k + ".percent"), y.getLong(k + ".expires"), y.getString(k + ".target", "all")));
        }));
        section(y, "transfers").ifPresent(root -> root.getKeys(false).forEach(id -> {
            String k = "transfers." + id; Transfer t = new Transfer(UUID.fromString(id), UUID.fromString(y.getString(k + ".player", "")), Transfer.Direction.valueOf(y.getString(k + ".direction", "")), y.getString(k + ".item", ""), y.getBoolean(k + ".active")); s.transfers.put(t.id(), t);
        }));
        return s;
    }
    private @NotNull Optional<ConfigurationSection> section(@NotNull ConfigurationSection y, @NotNull String key) { return Optional.ofNullable(y.getConfigurationSection(key)); }
}
