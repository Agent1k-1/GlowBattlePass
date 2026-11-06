package com.agent1k.glowbattlepass.command;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.quest.Quest;
import com.agent1k.glowbattlepass.service.DailySync;
import com.agent1k.glowbattlepass.util.Items;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class AdminCommand extends AbstractCommand {
    public AdminCommand(@NotNull GlowBattlePass plugin) { super(plugin); }
    @Override public void execute(@NotNull CommandSender sender, @NotNull String[] a) {
        if (!sender.hasPermission("glowbattlepass.admin")) throw new IllegalArgumentException("Недостаточно прав");
        require(a, 1, "/bpa help");
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "help": sender.sendMessage(new String[]{"/bpa editor | reload | open <игрок>", "/bpa currency <give|take|set> <игрок> <количество>", "/bpa set pass <игрок> <free|premium>", "/bpa boosters add <игрок|all> experience <процент> <часы> <цель|all>", "/bpa boosters clear <игрок|all>", "/bpa reset quest <игрок> <неделя> <id>", "/bpa progress quest <игрок> <неделя> <id> <количество>", "/bpa progress daily quest <игрок> <id> <количество>", "/bpa refresh daily quests | sync daily quests", "/bpa new season confirm", "/bpa material [block|item|b64]", "/bpa bypass locked quests <игрок>"}); return;
            case "editor": plugin.menus().editor(player(sender), -1); return;
            case "reload": plugin.reloadSettings(); break;
            case "currency": currency(a); break;
            case "set": setPass(a); break;
            case "boosters": boosters(a); break;
            case "reset": reset(a); break;
            case "progress": progress(a); break;
            case "refresh": exactTail(a, "daily", "quests"); plugin.store().transaction(s -> { plugin.progress().refresh(s, true); return true; }); break;
            case "sync": exactTail(a, "daily", "quests"); DailySync.send(plugin, player(sender)); break;
            case "new":
                require(a, 3, "/bpa new season confirm — сбросить сезон");
                if (!a[1].equalsIgnoreCase("season") || !a[2].equalsIgnoreCase("confirm")) throw new IllegalArgumentException("/bpa new season confirm");
                plugin.store().transaction(s -> { s.profiles.values().forEach(Profile::resetSeason); s.seasonStarted = System.currentTimeMillis(); s.boosters.clear(); return true; }); break;
            case "material": material(player(sender), a); return;
            case "bypass":
                require(a, 4, "/bpa bypass locked quests <игрок>");
                if (!a[1].equalsIgnoreCase("locked") || !a[2].equalsIgnoreCase("quests")) throw new IllegalArgumentException("/bpa bypass locked quests <игрок>");
                UUID bypassId = target(a[3]); plugin.store().transaction(s -> { s.profile(bypassId).bypass(true); return true; }); break;
            case "open": require(a, 2, "/bpa open <игрок>"); Player online = plugin.getServer().getPlayerExact(a[1]); plugin.menus().open(Optional.ofNullable(online).orElseThrow(() -> new IllegalArgumentException("Игрок не в сети")), "main"); break;
            default: throw new IllegalArgumentException("/bpa help");
        }
        sender.sendMessage(com.agent1k.glowbattlepass.util.Colors.color(plugin.getConfig().getString("messages.success", "&aГотово.")));
    }
    private @NotNull UUID target(@NotNull String name) {
        return plugin.store().state().profiles.values().stream().filter(p -> p.name().equalsIgnoreCase(name) || p.id().toString().equals(name)).map(Profile::id).findFirst().orElseThrow(() -> new IllegalArgumentException("Игрок ещё не заходил на сервер"));
    }
    private void currency(@NotNull String[] a) {
        require(a, 4, "/bpa currency <give|take|set> <игрок> <количество>"); UUID id = target(a[2]); long amount = Long.parseLong(a[3]);
        plugin.store().transaction(s -> { Profile p = s.profile(id); switch (a[1].toLowerCase(Locale.ROOT)) { case "give": plugin.currency().give(p, amount); break; case "take": plugin.currency().take(p, amount); break; case "set": p.balance(amount); break; default: throw new IllegalArgumentException("give, take или set"); } return true; });
    }
    private void setPass(@NotNull String[] a) {
        require(a, 4, "/bpa set pass <игрок> <free|premium>");
        if (!a[1].equalsIgnoreCase("pass") || !Arrays.asList("free", "premium").contains(a[3].toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Типы пропуска: free, premium");
        UUID id = target(a[2]); plugin.store().transaction(s -> { s.profile(id).pass(a[3].toLowerCase(Locale.ROOT)); return true; });
    }
    private void boosters(@NotNull String[] a) {
        require(a, 3, "/bpa boosters <add|clear> <игрок|all>"); String owner = a[2].equalsIgnoreCase("all") ? "all" : target(a[2]).toString();
        if (a[1].equalsIgnoreCase("clear")) { plugin.store().transaction(s -> { s.boosters.removeIf(b -> b.owner().equals(owner)); return true; }); return; }
        require(a, 7, "/bpa boosters add <игрок|all> experience <процент> <часы> <цель>");
        if (!a[1].equalsIgnoreCase("add") || !a[3].equalsIgnoreCase("experience")) throw new IllegalArgumentException("Поддерживается тип experience");
        long hours = Long.parseLong(a[5]); if (hours <= 0) throw new IllegalArgumentException("Часы должны быть положительными");
        Booster b = new Booster(owner, "experience", Integer.parseInt(a[4]), Math.addExact(System.currentTimeMillis(), Math.multiplyExact(hours, 3600000L)), a[6]);
        plugin.store().transaction(s -> { s.boosters.add(b); return true; });
    }
    private void reset(@NotNull String[] a) {
        require(a, 5, "/bpa reset quest <игрок> <неделя> <id>"); if (!a[1].equalsIgnoreCase("quest")) throw new IllegalArgumentException("Ожидается quest");
        UUID id = target(a[2]); Quest q = plugin.quests().find(Integer.parseInt(a[3]), a[4]);
        plugin.store().transaction(s -> { Profile p = s.profile(id); p.progress().remove(q.key()); p.completed().remove(q.key()); return true; });
    }
    private void progress(@NotNull String[] a) {
        require(a, 6, "/bpa progress quest <игрок> <неделя> <id> <количество>");
        boolean daily = a[1].equalsIgnoreCase("daily");
        if (!(daily ? a[2] : a[1]).equalsIgnoreCase("quest")) throw new IllegalArgumentException("Ожидается quest");
        UUID id = target(a[daily ? 3 : 2]); Quest q = plugin.quests().find(daily ? 0 : Integer.parseInt(a[3]), a[4]); long amount = Long.parseLong(a[5]);
        plugin.store().transaction(s -> { plugin.progress().refresh(s, false); plugin.progress().advance(s, s.profile(id), q, amount); return true; });
    }
    private void exactTail(@NotNull String[] a, @NotNull String first, @NotNull String second) { require(a, 3, "Ожидается " + first + " " + second); if (!a[1].equalsIgnoreCase(first) || !a[2].equalsIgnoreCase(second)) throw new IllegalArgumentException("Ожидается " + first + " " + second); }
    private void material(@NotNull Player p, @NotNull String[] a) {
        String type = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "item";
        switch (type) {
            case "b64": p.sendMessage(Items.encode(p.getInventory().getItemInMainHand())); break;
            case "item": p.sendMessage(p.getInventory().getItemInMainHand().getType().name()); break;
            case "block": p.sendMessage(Optional.ofNullable(p.getTargetBlockExact(6)).map(b -> b.getType().name()).orElse("AIR")); break;
            default: throw new IllegalArgumentException("/bpa material <block|item|b64>");
        }
    }
}
