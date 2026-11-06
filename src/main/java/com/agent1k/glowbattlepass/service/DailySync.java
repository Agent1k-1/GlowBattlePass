package com.agent1k.glowbattlepass.service;

import com.agent1k.glowbattlepass.GlowBattlePass;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.time.*;

public final class DailySync implements PluginMessageListener {
    private final GlowBattlePass plugin;
    public DailySync(@NotNull GlowBattlePass plugin) { this.plugin = plugin; }
    public static void send(@NotNull GlowBattlePass plugin, @NotNull Player player) {
        plugin.store().transaction(s -> { plugin.progress().refresh(s, false); return true; });
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF("Forward"); out.writeUTF("ALL"); out.writeUTF("GlowBattlePass");
            byte[] data = LocalDate.now(ZoneOffset.UTC).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeShort(data.length); out.write(data); player.sendPluginMessage(plugin, "BungeeCord", bytes.toByteArray());
        } catch (IOException e) { throw new IllegalStateException("Не удалось отправить синхронизацию", e); }
    }
    @Override public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            if (!in.readUTF().equals("GlowBattlePass")) return;
            int size = in.readUnsignedShort(); if (size > 32) return; byte[] payload = new byte[size]; in.readFully(payload);
            String day = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
            if (!day.equals(LocalDate.now(ZoneOffset.UTC).toString())) return;
            plugin.store().transaction(s -> { plugin.progress().refresh(s, false); return true; });
        } catch (IOException | RuntimeException e) { plugin.getLogger().warning("Отклонена синхронизация: " + e.getMessage()); }
    }
}
