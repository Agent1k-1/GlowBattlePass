package com.agent1k.glowbattlepass.command;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.domain.Listing;
import com.agent1k.glowbattlepass.util.Items;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class AuctionCommand extends AbstractCommand {
    public AuctionCommand(@NotNull GlowBattlePass plugin) { super(plugin); }
    @Override public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Player p = player(sender); plugin.register(p);
        if (args.length == 0) { plugin.menus().auction(p, "", "", 0); return; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sell": require(args, 2, "/bpah sell <цена>"); sell(p, Long.parseLong(args[1])); break;
            case "search": require(args, 2, "/bpah search <название>"); plugin.menus().auction(p, String.join(" ", Arrays.copyOfRange(args, 1, args.length)), "", 0); break;
            default: plugin.menus().auction(p, "", args[0], 0);
        }
    }
    private void sell(@NotNull Player p, long price) {
        if (price <= 0 || price > plugin.getConfig().getLong("auction.max-price", 1000000000)) throw new IllegalArgumentException("Цена вне разрешённого диапазона");
        ItemStack item = p.getInventory().getItemInMainHand().clone();
        if (item.getType().isAir()) throw new IllegalArgumentException("Возьмите товар в основную руку");
        long expires = Math.addExact(System.currentTimeMillis(), Math.multiplyExact(plugin.getConfig().getLong("auction.duration-hours", 72), 3600000L));
        String name = Optional.ofNullable(item.getItemMeta()).filter(m -> m.hasDisplayName()).map(m -> org.bukkit.ChatColor.stripColor(m.getDisplayName())).orElse(item.getType().name());
        Listing listing = new Listing(UUID.randomUUID(), p.getUniqueId(), Items.encode(item), name, price, expires);
        int slot = p.getInventory().getHeldItemSlot();
        plugin.transfers().sell(p, listing, slot); p.sendMessage("Товар выставлен за " + price + " валюты БП");
    }
}
