package com.agent1k.glowbattlepass;

import com.agent1k.glowbattlepass.command.*;
import com.agent1k.glowbattlepass.menu.*;
import com.agent1k.glowbattlepass.quest.*;
import com.agent1k.glowbattlepass.service.*;
import com.agent1k.glowbattlepass.storage.*;
import com.agent1k.glowbattlepass.listener.*;
import com.agent1k.glowbattlepass.placeholder.BattlePassExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class GlowBattlePass extends JavaPlugin {
    private StateStore store;
    private QuestCatalog quests;
    private MenuService menus;
    private InventoryTransferService transfers;
    private final CurrencyService currency = new CurrencyService();
    private final AuctionService auction = new AuctionService(currency);
    private final QuestService progress = new QuestService();
    @Override public void onEnable() {
        try {
            saveDefaultConfig(); validateConfig();
            store = new StateStore(getDataFolder().toPath().resolve("battlepass.db"));
            quests = new QuestCatalog(getConfig()); transfers = new InventoryTransferService(this); menus = new MenuService(this);
            CommandRouter router = new CommandRouter(this);
            for (String name : Arrays.asList("bp", "bpa", "bpah")) {
                PluginCommand command = Objects.requireNonNull(getCommand(name)); command.setExecutor(router); command.setTabCompleter(router);
            }
            Bukkit.getPluginManager().registerEvents(new GameplayListener(this), this);
            Bukkit.getPluginManager().registerEvents(menus, this);
            Bukkit.getOnlinePlayers().forEach(this::register);
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new DailySync(this));
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                try { store().transaction(s -> { progress.refresh(s, false); auction.expire(s, System.currentTimeMillis()); s.boosters.removeIf(b -> b.expires() <= System.currentTimeMillis()); return true; }); }
                catch (RuntimeException e) { getLogger().log(java.util.logging.Level.SEVERE, "Ошибка обслуживания хранилища", e); }
            }, 20L, 1200L);
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) new BattlePassExpansion(this).register();
        } catch (Exception e) { getLogger().log(java.util.logging.Level.SEVERE, "Не удалось запустить GlowBattlePass", e); Bukkit.getPluginManager().disablePlugin(this); }
    }
    @Override public void onDisable() {
        if (menus != null) Bukkit.getOnlinePlayers().stream().filter(p -> p.getOpenInventory().getTopInventory().getHolder() instanceof MenuView).forEach(Player::closeInventory);
        if (store != null) try { store.close(); } catch (Exception e) { getLogger().severe(e.getMessage()); }
    }
    public void register(@NotNull Player player) {
        store().transaction(s -> { s.profiles.computeIfAbsent(player.getUniqueId(), id -> new com.agent1k.glowbattlepass.domain.Profile(id, player.getName())).name(player.getName()); progress.refresh(s, false); return true; });
        try { transfers().recover(player); }
        catch (RuntimeException e) { player.kickPlayer("Не удалось восстановить передачу предметов. Обратитесь к администратору."); throw e; }
    }
    public void reloadSettings() {
        String previous = getConfig().saveToString();
        try { reloadConfig(); validateConfig(); QuestCatalog next = new QuestCatalog(getConfig()); menus().reload(); quests = next; }
        catch (RuntimeException failure) {
            try { getConfig().loadFromString(previous); } catch (org.bukkit.configuration.InvalidConfigurationException e) { failure.addSuppressed(e); }
            throw failure;
        }
    }
    private void validateConfig() {
        Optional.ofNullable(getConfig().getConfigurationSection("rewards")).ifPresent(rewards -> rewards.getKeys(false).forEach(tier -> {
            if (Integer.parseInt(tier) < 1 || rewards.getLong(tier + ".currency") < 0 || rewards.getLong(tier + ".premium-currency") < 0) throw new IllegalArgumentException("Некорректная награда уровня " + tier);
        }));
        if (getConfig().getLong("experience-per-tier", 100) <= 0 || getConfig().getInt("auction.max-listings", 10) <= 0 || getConfig().getLong("auction.duration-hours", 72) <= 0) throw new IllegalArgumentException("Некорректные лимиты конфигурации");
    }
    public @NotNull StateStore store() { return Objects.requireNonNull(store, "Хранилище не запущено"); }
    public @NotNull QuestCatalog quests() { return Objects.requireNonNull(quests); }
    public @NotNull MenuService menus() { return Objects.requireNonNull(menus); }
    public @NotNull InventoryTransferService transfers() { return Objects.requireNonNull(transfers); }
    public @NotNull CurrencyService currency() { return currency; }
    public @NotNull AuctionService auction() { return auction; }
    public @NotNull QuestService progress() { return progress; }
    public long tier(@NotNull com.agent1k.glowbattlepass.domain.Profile p) { return p.experience() / getConfig().getLong("experience-per-tier", 100); }
}
