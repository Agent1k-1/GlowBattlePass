package com.agent1k.glowbattlepass.listener;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.quest.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class GameplayListener implements Listener {
    private final GlowBattlePass plugin;
    public GameplayListener(@NotNull GlowBattlePass plugin) { this.plugin = plugin; }
    @EventHandler public void join(@NotNull PlayerJoinEvent e) { plugin.register(e.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void broken(@NotNull BlockBreakEvent e) { advance(e.getPlayer(), QuestType.BREAK, e.getBlock().getType().name()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void placed(@NotNull BlockPlaceEvent e) { advance(e.getPlayer(), QuestType.PLACE, e.getBlock().getType().name()); }
    @EventHandler(priority = EventPriority.MONITOR) public void killed(@NotNull EntityDeathEvent e) { Optional.ofNullable(e.getEntity().getKiller()).ifPresent(p -> advance(p, QuestType.KILL, e.getEntityType().name())); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void fish(@NotNull PlayerFishEvent e) { if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) advance(e.getPlayer(), QuestType.FISH, "ANY"); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void craft(@NotNull CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player && !e.isShiftClick() && e.getClick() != org.bukkit.event.inventory.ClickType.NUMBER_KEY && e.getClick() != org.bukkit.event.inventory.ClickType.DROP && e.getClick() != org.bukkit.event.inventory.ClickType.CONTROL_DROP && Optional.ofNullable(e.getCursor()).map(item -> item.getType().isAir()).orElse(true)) advance((Player) e.getWhoClicked(), QuestType.CRAFT, e.getRecipe().getResult().getType().name());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingClick(@NotNull org.bukkit.event.inventory.InventoryClickEvent e) { if (e.getWhoClicked() instanceof Player && plugin.transfers().locked((Player) e.getWhoClicked())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingDrag(@NotNull org.bukkit.event.inventory.InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player && plugin.transfers().locked((Player) e.getWhoClicked())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingDrop(@NotNull PlayerDropItemEvent e) { if (plugin.transfers().locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingSwap(@NotNull PlayerSwapHandItemsEvent e) { if (plugin.transfers().locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingInteract(@NotNull PlayerInteractEvent e) { if (plugin.transfers().locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingDamage(@NotNull org.bukkit.event.entity.EntityDamageEvent e) { if (e.getEntity() instanceof Player && plugin.transfers().locked((Player) e.getEntity())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void pendingPickup(@NotNull org.bukkit.event.entity.EntityPickupItemEvent e) { if (e.getEntity() instanceof Player && plugin.transfers().locked((Player) e.getEntity())) e.setCancelled(true); }
    private void advance(@NotNull Player p, @NotNull QuestType type, @NotNull String material) {
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return;
        List<Quest> matching = new ArrayList<>();
        plugin.quests().all().stream().filter(q -> q.type() == type).filter(q -> q.material().equals("ANY") || q.material().equals(material)).forEach(matching::add);
        if (matching.isEmpty()) return;
        try { plugin.store().transaction(s -> { plugin.progress().refresh(s, false); matching.forEach(q -> plugin.progress().advance(s, s.profile(p.getUniqueId()), q, 1)); return true; }); }
        catch (RuntimeException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Не удалось обновить прогресс " + p.getUniqueId(), e); }
    }
}
