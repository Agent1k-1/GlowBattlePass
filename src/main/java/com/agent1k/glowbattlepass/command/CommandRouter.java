package com.agent1k.glowbattlepass.command;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.util.Colors;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public final class CommandRouter implements TabExecutor {
    private final Map<String, AbstractCommand> handlers = new HashMap<>();
    private final GlowBattlePass plugin;
    public CommandRouter(@NotNull GlowBattlePass plugin) { this.plugin = plugin; handlers.put("bp", new PlayerCommand(plugin)); handlers.put("bpa", new AdminCommand(plugin)); handlers.put("bpah", new AuctionCommand(plugin)); }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try { Optional.ofNullable(handlers.get(command.getName())).orElseThrow(() -> new IllegalArgumentException("Неизвестная команда")).execute(sender, args); }
        catch (IllegalArgumentException | ArithmeticException e) { sender.sendMessage(Colors.color(plugin.getConfig().getString("messages.error", "&c{error}").replace("{error}", Optional.ofNullable(e.getMessage()).orElse("Некорректный аргумент")))); }
        catch (IllegalStateException e) { sender.sendMessage(Colors.color("&cОперация не выполнена: ошибка хранилища. Обратитесь к администратору.")); plugin.getLogger().log(java.util.logging.Level.SEVERE, "Ошибка команды", e); }
        return true;
    }
    @Override public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> values;
        switch (command.getName()) {
            case "bp": values = Arrays.asList("help", "stats", "open", "leaderboard", "boosters", "balance", "mail"); break;
            case "bpa": values = sender.hasPermission("glowbattlepass.admin") ? Arrays.asList("help", "editor", "reload", "set", "currency", "boosters", "reset", "progress", "refresh", "sync", "new", "material", "bypass", "open") : Collections.emptyList(); break;
            default: values = Arrays.asList("sell", "search");
        }
        return values.stream().filter(v -> v.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
    }
}
