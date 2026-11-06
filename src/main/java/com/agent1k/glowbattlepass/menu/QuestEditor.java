package com.agent1k.glowbattlepass.menu;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.quest.Quest;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;

final class QuestEditor {
    private final GlowBattlePass plugin;
    private final MenuService menus;
    QuestEditor(@NotNull GlowBattlePass plugin, @NotNull MenuService menus) { this.plugin = plugin; this.menus = menus; }
    public void editor(@NotNull Player p, int index) { editor(p, index, 0); }
    void editor(@NotNull Player p, int index, int requested) {
        admin(p); MenuView view = menus.template(p, "editor"); int slot = 0;
        int capacity = view.getInventory().getSize() - 9;
        int page = Math.max(0, Math.min(requested, Math.max(0, (plugin.quests().all().size() - 1) / capacity)));
        int offset = page * capacity;
        for (Quest q : plugin.quests().all().subList(offset, Math.min(plugin.quests().all().size(), offset + capacity))) {
            if (slot >= view.getInventory().getSize() - 9) break;
            menus.set(view, slot, menus.icon("WRITABLE_BOOK", "&e" + q.key(), Collections.singletonList("&7ЛКМ: редактировать")), Collections.singletonList("[edit] " + (offset + slot)), Collections.emptyList()); slot++;
        }
        menus.set(view, capacity, menus.icon("ARROW", "&eНазад", Collections.emptyList()), Collections.singletonList("[editor-page] " + Math.max(0, page - 1)), Collections.emptyList());
        menus.set(view, capacity + 8, menus.icon("ARROW", "&eДалее", Collections.emptyList()), Collections.singletonList("[editor-page] " + (page + 1)), Collections.emptyList());
        if (index >= 0) {
            if (index >= plugin.quests().all().size()) throw new IllegalArgumentException("Квест не найден");
            Quest q = plugin.quests().all().get(index); view = menus.template(p, "quest-editor");
            String[] fields = {"target", "experience", "currency", "type", "material"};
            String[] values = {String.valueOf(q.target()), String.valueOf(q.experience()), String.valueOf(q.currency()), q.type().name(), q.material()};
            for (int i = 0; i < fields.length; i++) menus.set(view, 10 + i, menus.icon("PAPER", "&e" + fields[i] + ": " + values[i], Arrays.asList("&7ЛКМ: +1 / следующий", "&7ПКМ: -1 / предыдущий", "&7Для material возьмите предмет в руку")), Collections.singletonList("[adjust] " + index + " " + fields[i] + " 1"), Collections.singletonList("[adjust] " + index + " " + fields[i] + " -1"));
        }
        p.openInventory(view.getInventory());
    }
    void createQuest(@NotNull Player p, @NotNull String area) {
        admin(p); String id = "quest-" + UUID.randomUUID().toString().substring(0, 8);
        String path = area.equals("daily") ? "quests.daily." + id : "quests.weeks." + Integer.parseInt(area) + "." + id;
        plugin.getConfig().set(path + ".type", "BREAK"); plugin.getConfig().set(path + ".material", "ANY"); plugin.getConfig().set(path + ".target", 1); plugin.getConfig().set(path + ".experience", 10); plugin.getConfig().set(path + ".currency", 1);
        plugin.saveConfig(); plugin.reloadSettings();
        int index = plugin.quests().all().indexOf(plugin.quests().all().stream().filter(q -> q.id().equals(id)).findFirst().orElseThrow(IllegalStateException::new)); editor(p, index);
    }
    void adjust(@NotNull Player p, @NotNull String args) {
        admin(p); String[] a = args.split(" "); int index = Integer.parseInt(a[0]); Quest q = plugin.quests().all().get(index);
        String path = q.week() == 0 ? "quests.daily." + q.id() : "quests.weeks." + q.week() + "." + q.id();
        String field = a[1]; int delta = Integer.parseInt(a[2]);
        switch (field) {
            case "type": com.agent1k.glowbattlepass.quest.QuestType[] types = com.agent1k.glowbattlepass.quest.QuestType.values(); plugin.getConfig().set(path + ".type", types[Math.floorMod(q.type().ordinal() + delta, types.length)].name()); break;
            case "material": if (p.getInventory().getItemInMainHand().getType().isAir()) throw new IllegalArgumentException("Возьмите предмет в руку"); plugin.getConfig().set(path + ".material", p.getInventory().getItemInMainHand().getType().name()); break;
            default: long value = plugin.getConfig().getLong(path + "." + field); plugin.getConfig().set(path + "." + field, Math.max(field.equals("target") ? 1 : 0, Math.addExact(value, delta)));
        }
        plugin.saveConfig(); plugin.reloadSettings(); editor(p, index);
    }
    private void admin(@NotNull Player p) { if (!p.hasPermission("glowbattlepass.admin")) throw new IllegalArgumentException("Недостаточно прав"); }
}
