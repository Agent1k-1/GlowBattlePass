package com.agent1k.glowbattlepass.menu;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.function.BiConsumer;

public final class ActionRegistry {
    private final Map<String, BiConsumer<Player, String>> actions = new HashMap<>();
    public ActionRegistry(@NotNull GlowBattlePass plugin) {
        actions.put("message", (p, s) -> p.sendMessage(Colors.color(s)));
        actions.put("player", (p, s) -> p.performCommand(s.replace("{player}", p.getName())));
        actions.put("console", (p, s) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replace("{player}", p.getName())));
        actions.put("open", (p, s) -> plugin.menus().open(p, s));
        actions.put("close", (p, s) -> p.closeInventory());
        actions.put("sound", (p, s) -> p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(s.toUpperCase(Locale.ROOT)), 1, 1));
    }
    public void register(@NotNull String key, @NotNull BiConsumer<Player, String> handler) { actions.put(key, handler); }
    public void execute(@NotNull Player player, @NotNull String action) {
        int end = action.indexOf(']');
        if (!action.startsWith("[") || end < 2) throw new IllegalArgumentException("Некорректный action: " + action);
        BiConsumer<Player, String> handler = Optional.ofNullable(actions.get(action.substring(1, end))).orElseThrow(() -> new IllegalArgumentException("Неизвестный action"));
        handler.accept(player, action.substring(end + 1).trim());
    }
}
