package com.agent1k.glowbattlepass.command;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.domain.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public final class PlayerCommand extends AbstractCommand {
    public PlayerCommand(@NotNull GlowBattlePass plugin) { super(plugin); }
    @Override public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        Player p = player(sender); plugin.register(p);
        if (args.length == 0) { plugin.menus().open(p, "main"); return; }
        Profile profile = plugin.store().state().profile(p.getUniqueId());
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help": p.sendMessage(new String[]{"/bp — меню", "/bp stats", "/bp open <меню>", "/bp open week <номер>", "/bp leaderboard [tier|quests]", "/bp boosters", "/bp balance", "/bp mail", "/bpah [sell <цена>|search <название>|ник]"}); break;
            case "stats": p.sendMessage("Уровень: " + plugin.tier(profile) + ", опыт: " + profile.experience() + ", выполнено квестов: " + profile.completed().size() + ", пропуск: " + profile.pass()); break;
            case "balance": p.sendMessage("Валюта БП: " + profile.balance()); break;
            case "mail": plugin.menus().claimMail(p); break;
            case "open": require(args, 2, "/bp open <меню|week номер>"); if (args[1].equalsIgnoreCase("week")) { require(args, 3, "/bp open week <номер>"); plugin.menus().week(p, Integer.parseInt(args[2])); } else plugin.menus().open(p, args[1]); break;
            case "leaderboard": leaderboard(p, args.length > 1 ? args[1] : "tier"); break;
            case "boosters":
                List<Booster> boosters = plugin.store().state().boosters.stream().filter(b -> b.expires() > System.currentTimeMillis()).filter(b -> b.owner().equals("all") || b.owner().equals(p.getUniqueId().toString())).collect(Collectors.toList());
                if (boosters.isEmpty()) p.sendMessage("Активных бустеров нет");
                boosters.forEach(b -> p.sendMessage("+" + b.percent() + "% опыта, цель: " + b.target() + ", осталось минут: " + Math.max(0, (b.expires() - System.currentTimeMillis()) / 60000))); break;
            default: throw new IllegalArgumentException("Используйте /bp help");
        }
    }
    private void leaderboard(@NotNull Player p, @NotNull String type) {
        if (!Arrays.asList("tier", "quests").contains(type)) throw new IllegalArgumentException("/bp leaderboard [tier|quests]");
        Comparator<Profile> ranking = type.equals("quests") ? Comparator.comparingLong(a -> a.completed().size()) : Comparator.comparingLong(Profile::experience);
        List<Profile> top = plugin.store().state().profiles.values().stream().sorted(ranking.reversed().thenComparing(Profile::name)).limit(10).collect(Collectors.toList());
        for (int i = 0; i < top.size(); i++) { Profile v = top.get(i); p.sendMessage((i + 1) + ". " + v.name() + " — " + (type.equals("quests") ? v.completed().size() : plugin.tier(v))); }
    }
}
