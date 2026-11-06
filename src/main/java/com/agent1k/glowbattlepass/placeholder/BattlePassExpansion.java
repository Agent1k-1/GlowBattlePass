package com.agent1k.glowbattlepass.placeholder;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.domain.Profile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.*;
import java.util.*;

public final class BattlePassExpansion extends PlaceholderExpansion {
    private final GlowBattlePass plugin;
    public BattlePassExpansion(@NotNull GlowBattlePass plugin) { super(); this.plugin = plugin; }
    @Override public @NotNull String getIdentifier() { return "glowbattlepass"; }
    @Override public @NotNull String getAuthor() { return "Agent1k"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public @NotNull String onRequest(@Nullable OfflinePlayer player, @NotNull String parameter) {
        Optional<Profile> profile = Optional.ofNullable(player).map(OfflinePlayer::getUniqueId).map(id -> plugin.store().state().profiles.get(id));
        if (!profile.isPresent()) return "0";
        Profile p = profile.get();
        switch (parameter.toLowerCase(Locale.ROOT)) {
            case "balance": return String.valueOf(p.balance());
            case "tier": return String.valueOf(plugin.tier(p));
            case "experience": return String.valueOf(p.experience());
            case "quests": return String.valueOf(p.completed().size());
            case "pass": return p.pass();
            default: return "";
        }
    }
}
