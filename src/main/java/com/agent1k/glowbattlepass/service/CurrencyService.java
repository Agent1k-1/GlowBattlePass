package com.agent1k.glowbattlepass.service;

import com.agent1k.glowbattlepass.domain.Profile;
import org.jetbrains.annotations.NotNull;

public final class CurrencyService {
    public void give(@NotNull Profile player, long amount) { positive(amount); player.balance(Math.addExact(player.balance(), amount)); }
    public void take(@NotNull Profile player, long amount) { positive(amount); player.balance(Math.subtractExact(player.balance(), amount)); }
    public void transfer(@NotNull Profile from, @NotNull Profile to, long amount) {
        if (from.id().equals(to.id())) throw new IllegalArgumentException("Нельзя купить свой лот");
        positive(amount); long remaining = Math.subtractExact(from.balance(), amount); long total = Math.addExact(to.balance(), amount);
        if (remaining < 0) throw new IllegalArgumentException("Недостаточно валюты");
        from.balance(remaining); to.balance(total);
    }
    private void positive(long amount) { if (amount <= 0) throw new IllegalArgumentException("Количество должно быть положительным"); }
}
