package com.agent1k.glowbattlepass.quest;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public final class QuestCatalog {
    private final List<Quest> quests = new ArrayList<>();
    public QuestCatalog(@NotNull ConfigurationSection config) {
        read(config, "quests.daily", 0);
        Optional.ofNullable(config.getConfigurationSection("quests.weeks")).ifPresent(weeks -> weeks.getKeys(false).forEach(w -> read(config, "quests.weeks." + w, Integer.parseInt(w))));
    }
    private void read(@NotNull ConfigurationSection config, @NotNull String path, int week) {
        Optional.ofNullable(config.getConfigurationSection(path)).ifPresent(section -> section.getKeys(false).forEach(id -> {
            String k = path + "." + id;
            quests.add(new Quest(id, QuestType.valueOf(config.getString(k + ".type", "BREAK").toUpperCase(Locale.ROOT)), config.getString(k + ".material", "ANY").toUpperCase(Locale.ROOT), week, config.getLong(k + ".target", 1), config.getLong(k + ".experience"), config.getLong(k + ".currency")));
        }));
    }
    public @NotNull List<Quest> all() { return Collections.unmodifiableList(quests); }
    public @NotNull Quest find(int week, @NotNull String id) { return quests.stream().filter(q -> q.week() == week && q.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Квест не найден")); }
}
