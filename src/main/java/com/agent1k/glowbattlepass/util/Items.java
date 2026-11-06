package com.agent1k.glowbattlepass.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.*;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.util.Base64;

public final class Items {
    private Items() { }
    public static @NotNull String encode(@NotNull ItemStack item) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) { out.writeObject(item); out.flush(); return Base64.getEncoder().encodeToString(bytes.toByteArray()); }
        catch (IOException e) { throw new IllegalStateException("Не удалось сохранить предмет", e); }
    }
    public static @NotNull ItemStack decode(@NotNull String data) {
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) { return (ItemStack) in.readObject(); }
        catch (IOException | ClassNotFoundException e) { throw new IllegalStateException("Не удалось прочитать предмет", e); }
    }
}
