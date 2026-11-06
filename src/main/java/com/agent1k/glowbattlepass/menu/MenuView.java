package com.agent1k.glowbattlepass.menu;

import org.bukkit.Bukkit;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class MenuView implements InventoryHolder {
    private final Inventory inventory;
    private final Map<Integer, List<String>> left = new HashMap<>(), right = new HashMap<>();
    public MenuView(int size, @NotNull String title) { inventory = Bukkit.createInventory(this, size, title); }
    @Override public @NotNull Inventory getInventory() { return inventory; }
    public void actions(int slot, @NotNull List<String> l, @NotNull List<String> r) { left.put(slot, l); right.put(slot, r); }
    public @NotNull List<String> actions(int slot, boolean rightClick) { return (rightClick ? right : left).getOrDefault(slot, Collections.emptyList()); }
}
