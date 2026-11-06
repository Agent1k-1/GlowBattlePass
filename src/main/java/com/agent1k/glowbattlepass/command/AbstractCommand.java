package com.agent1k.glowbattlepass.command;

import com.agent1k.glowbattlepass.GlowBattlePass;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public abstract class AbstractCommand {
    protected final GlowBattlePass plugin;
    protected AbstractCommand(@NotNull GlowBattlePass plugin) { this.plugin = Objects.requireNonNull(plugin); }
    public abstract void execute(@NotNull CommandSender sender, @NotNull String[] args);
    protected @NotNull Player player(@NotNull CommandSender sender) { if (!(sender instanceof Player)) throw new IllegalArgumentException("Команда доступна только игроку"); return (Player) sender; }
    protected void require(@NotNull String[] args, int length, @NotNull String usage) { if (args.length < length) throw new IllegalArgumentException(usage); }
}
