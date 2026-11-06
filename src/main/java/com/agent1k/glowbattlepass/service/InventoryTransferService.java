package com.agent1k.glowbattlepass.service;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.util.Items;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public final class InventoryTransferService {
    private final GlowBattlePass plugin;
    private final NamespacedKey key;
    public InventoryTransferService(@NotNull GlowBattlePass plugin) { this.plugin = plugin; key = new NamespacedKey(plugin, "transfer"); }
    public void sell(@NotNull Player p, @NotNull Listing listing, int slot) {
        recover(p);
        Transfer transfer = new Transfer(listing.id(), p.getUniqueId(), Transfer.Direction.SALE, listing.item(), false);
        plugin.store().transaction(s -> {
            plugin.auction().expire(s, System.currentTimeMillis());
            if (s.listings.values().stream().filter(l -> l.seller().equals(p.getUniqueId())).count() >= plugin.getConfig().getInt("auction.max-listings", 10)) throw new IllegalArgumentException("Достигнут лимит лотов");
            s.transfers.put(transfer.id(), transfer); return true;
        });
        try {
            p.getInventory().setItem(slot, tagged(transfer)); p.saveData();
            plugin.store().transaction(s -> { s.listings.put(listing.id(), listing); s.transfers.get(transfer.id()).activate(); return true; });
            finish(p, transfer.id());
        } catch (RuntimeException e) { recover(p); throw e; }
    }
    public void claim(@NotNull Player p) {
        recover(p); Profile profile = plugin.store().state().profile(p.getUniqueId());
        if (profile.mailbox().isEmpty()) throw new IllegalArgumentException("Почта пуста");
        int slot = p.getInventory().firstEmpty(); if (slot < 0) throw new IllegalArgumentException("Освободите слот в инвентаре");
        Transfer transfer = new Transfer(UUID.randomUUID(), p.getUniqueId(), Transfer.Direction.DELIVERY, profile.mailbox().get(0), false);
        Items.decode(transfer.item());
        plugin.store().transaction(s -> { s.profile(p.getUniqueId()).mailbox().remove(0); s.transfers.put(transfer.id(), transfer); return true; });
        try {
            p.getInventory().setItem(slot, tagged(transfer)); p.saveData();
            plugin.store().transaction(s -> { s.transfers.get(transfer.id()).activate(); return true; });
            finish(p, transfer.id());
        } catch (RuntimeException e) { recover(p); throw e; }
    }
    public void recover(@NotNull Player p) {
        List<UUID> pending = plugin.store().state().transfers.values().stream().filter(t -> t.player().equals(p.getUniqueId())).map(Transfer::id).collect(Collectors.toList());
        for (UUID id : pending) finish(p, id);
    }
    private void finish(@NotNull Player p, @NotNull UUID id) {
        Transfer transfer = Optional.ofNullable(plugin.store().state().transfers.get(id)).orElseThrow(() -> new IllegalStateException("Отсутствует запись передачи"));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            Optional<ItemStack> item = Optional.ofNullable(p.getInventory().getItem(i));
            if (item.flatMap(stack -> Optional.ofNullable(stack.getItemMeta())).map(meta -> id.toString().equals(meta.getPersistentDataContainer().get(key, PersistentDataType.STRING))).orElse(false)) slots.add(i);
        }
        boolean sale = transfer.direction() == Transfer.Direction.SALE;
        if (!sale && !transfer.active() && slots.isEmpty()) {
            plugin.store().transaction(s -> { s.profile(p.getUniqueId()).mailbox().add(transfer.item()); s.transfers.remove(id); return true; }); return;
        }
        if (!sale && !transfer.active()) plugin.store().transaction(s -> { s.transfers.get(id).activate(); return true; });
        ItemStack replacement = sale && transfer.active() ? new ItemStack(Material.AIR) : Items.decode(transfer.item());
        for (int slot : slots) p.getInventory().setItem(slot, replacement.clone());
        p.saveData();
        plugin.store().transaction(s -> { s.transfers.remove(id); return true; });
    }
    public boolean locked(@NotNull Player p) { return plugin.store().state().transfers.values().stream().anyMatch(t -> t.player().equals(p.getUniqueId())); }
    private @NotNull ItemStack tagged(@NotNull Transfer transfer) {
        ItemStack item = Items.decode(transfer.item()); ItemMeta meta = Objects.requireNonNull(item.getItemMeta()); meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, transfer.id().toString()); item.setItemMeta(meta); return item;
    }
}
