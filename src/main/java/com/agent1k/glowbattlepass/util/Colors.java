package com.agent1k.glowbattlepass.util;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import java.util.regex.*;

public final class Colors {
    private static final Pattern RGB = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private Colors() { }
    public static @NotNull String color(@NotNull String text) {
        Matcher matcher = RGB.matcher(text); StringBuffer result = new StringBuffer();
        while (matcher.find()) { StringBuilder hex = new StringBuilder("§x"); for (char c : matcher.group(1).toCharArray()) hex.append('§').append(c); matcher.appendReplacement(result, Matcher.quoteReplacement(hex.toString())); }
        matcher.appendTail(result); return ChatColor.translateAlternateColorCodes('&', result.toString());
    }
}
