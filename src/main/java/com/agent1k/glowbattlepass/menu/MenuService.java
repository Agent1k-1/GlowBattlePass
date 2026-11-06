package com.agent1k.glowbattlepass.menu;

import com.agent1k.glowbattlepass.GlowBattlePass;
import com.agent1k.glowbattlepass.domain.*;
import com.agent1k.glowbattlepass.quest.Quest;
import com.agent1k.glowbattlepass.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public final class MenuService implements Listener {
    private final GlowBattlePass plugin;
    private final ActionRegistry actions;
    private final QuestEditor editor;
    private final Map<String, YamlConfiguration> definitions = new HashMap<>();
    public MenuService(@NotNull GlowBattlePass plugin) {
        this.plugin = plugin; actions = new ActionRegistry(plugin); editor = new QuestEditor(plugin, this);
        actions.register("buy", (p, id) -> { plugin.store().transaction(s -> { plugin.auction().buy(s, p.getUniqueId(), UUID.fromString(id), System.currentTimeMillis()); return true; }); auction(p, "", "", 0); });
        actions.register("cancel", (p, id) -> { plugin.store().transaction(s -> { plugin.auction().cancel(s, p.getUniqueId(), UUID.fromString(id)); return true; }); auction(p, "", p.getName(), 0); });
        actions.register("mail", (p, s) -> claimMail(p));
        actions.register("reward", (p, s) -> claimReward(p, Integer.parseInt(s)));
        actions.register("auction", (p, value) -> { String[] a = value.split(" "); auction(p, a.length > 1 ? decodeFilter(a[1]) : "", a.length > 2 ? decodeFilter(a[2]) : "", Integer.parseInt(a[0])); });
        actions.register("page", (p, value) -> { String[] a = value.split(" "); screen(p, a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2])); });
        actions.register("create", editor::createQuest);
        actions.register("editor-page", (p, value) -> editor.editor(p, -1, Integer.parseInt(value)));
        actions.register("edit", (p, key) -> editor(p, Integer.parseInt(key)));
        actions.register("adjust", editor::adjust);
        reload();
    }
    public void reload() {
        Map<String, YamlConfiguration> next = new HashMap<>();
        for (String name : Arrays.asList("main", "daily-quests", "week", "rewards", "auction", "editor", "quest-editor")) {
            File f = new File(plugin.getDataFolder(), "memus/" + name + ".yml");
            if (!f.exists()) plugin.saveResource("memus/" + name + ".yml", false);
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f); int size = y.getInt("size", 54);
            if (size < (name.equals("main") ? 9 : 18) || size > 54 || size % 9 != 0) throw new IllegalArgumentException("Некорректный размер меню " + name);
            next.put(name, y);
        }
        definitions.clear(); definitions.putAll(next);
    }
    public void open(@NotNull Player player, @NotNull String name) {
        screen(player, name, 0, 0);
    }
    public void week(@NotNull Player player, int week) {
        if (week <= 0) throw new IllegalArgumentException("Неделя должна быть положительной");
        screen(player, "week", week, 0);
    }
    private void screen(@NotNull Player player, @NotNull String name, int week, int page) {
        if (name.equals("auction")) { auction(player, "", "", page); return; }
        if (name.equals("editor") || name.equals("quest-editor")) { editor(player, -1); return; }
        MenuView view = template(player, name); render(player, view, name, week, page); player.openInventory(view.getInventory());
    }
    @NotNull MenuView template(@NotNull Player p, @NotNull String name) {
        YamlConfiguration y = definition(name); MenuView view = new MenuView(y.getInt("size", 54), Colors.color(y.getString("title", name)));
        Optional.ofNullable(y.getConfigurationSection("items")).ifPresent(items -> items.getKeys(false).forEach(k -> {
            String path = "items." + k; int slot = y.getInt(path + ".slot");
            set(view, slot, icon(y.getString(path + ".material", "PAPER"), format(p, y.getString(path + ".name", k)), y.getStringList(path + ".lore").stream().map(t -> format(p, t)).collect(Collectors.toList())), y.getStringList(path + ".left_click_action"), y.getStringList(path + ".right_click_action"));
        })); return view;
    }
    private void render(@NotNull Player p, @NotNull MenuView view, @NotNull String name, int week, int requested) {
        if (!Arrays.asList("rewards", "daily-quests", "week").contains(name)) return;
        Profile profile = plugin.store().state().profile(p.getUniqueId()); int slot = 0; int capacity = view.getInventory().getSize() - 9;
        List<String> rewardKeys = new ArrayList<>(Optional.ofNullable(plugin.getConfig().getConfigurationSection("rewards")).map(section -> section.getKeys(false)).orElse(Collections.emptySet()));
        rewardKeys.sort(Comparator.comparingInt(Integer::parseInt));
        List<Quest> quests = plugin.quests().all().stream().filter(q -> q.week() == week).collect(Collectors.toList());
        int count = name.equals("rewards") ? rewardKeys.size() : quests.size();
        int page = Math.max(0, Math.min(requested, Math.max(0, (count - 1) / capacity)));
        if (name.equals("rewards")) {
            for (String key : rewardKeys.stream().skip((long) page * capacity).limit(capacity).collect(Collectors.toList())) {
                set(view, slot++, icon("CHEST", "&eУровень " + key, Arrays.asList("&7ЛКМ: забрать награду", "&7Ваш уровень: " + plugin.tier(profile))), Collections.singletonList("[reward] " + key), Collections.emptyList());
            }
        } else if (name.equals("daily-quests") || name.equals("week")) {
            for (Quest q : quests.stream().skip((long) page * capacity).limit(capacity).collect(Collectors.toList())) {
                set(view, slot++, icon("BOOK", "&e" + q.id(), Arrays.asList("&7" + q.type() + " " + q.material(), "&f" + profile.progress().getOrDefault(q.key(), 0L) + "/" + q.target(), "&aОпыт: " + q.experience() + " Валюта: " + q.currency(), plugin.progress().unlocked(plugin.store().state(), profile, q) ? "&aДоступно" : "&cНеделя заблокирована")), Collections.emptyList(), Collections.emptyList());
            }
        } else return;
        set(view, capacity, icon("ARROW", "&eНазад", Collections.emptyList()), Collections.singletonList("[page] " + name + " " + week + " " + Math.max(0, page - 1)), Collections.emptyList());
        set(view, capacity + 8, icon("ARROW", "&eДалее", Collections.emptyList()), Collections.singletonList("[page] " + name + " " + week + " " + (page + 1)), Collections.emptyList());
    }
    public void auction(@NotNull Player p, @NotNull String search, @NotNull String seller, int requested) {
        plugin.store().transaction(s -> { plugin.auction().expire(s, System.currentTimeMillis()); return true; });
        MenuView view = template(p, "auction");
        List<Listing> listings = plugin.store().state().listings.values().stream().filter(l -> l.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).filter(l -> seller.isEmpty() || plugin.store().state().profile(l.seller()).name().equalsIgnoreCase(seller)).collect(Collectors.toList());
        int capacity = view.getInventory().getSize() - 9; int page = Math.max(0, Math.min(requested, Math.max(0, (listings.size() - 1) / capacity)));
        for (int i = page * capacity; i < Math.min(listings.size(), (page + 1) * capacity); i++) {
            Listing l = listings.get(i); ItemStack item = Items.decode(l.item()); ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
            List<String> lore = new ArrayList<>(Optional.ofNullable(meta.getLore()).orElse(Collections.emptyList()));
            lore.add(Colors.color("&eЦена: " + l.price())); lore.add(Colors.color("&7Продавец: " + plugin.store().state().profile(l.seller()).name())); lore.add(Colors.color("&aЛКМ: купить &7ПКМ: снять свой лот")); meta.setLore(lore); item.setItemMeta(meta);
            set(view, i % capacity, item, Collections.singletonList("[buy] " + l.id()), Collections.singletonList("[cancel] " + l.id()));
        }
        int offset = view.getInventory().getSize() - 9;
        set(view, offset, icon("ARROW", "&eНазад", Collections.emptyList()), Collections.singletonList("[auction] " + Math.max(0, page - 1) + " " + encodeFilter(search) + " " + encodeFilter(seller)), Collections.emptyList());
        set(view, offset + 8, icon("ARROW", "&eДалее", Collections.emptyList()), Collections.singletonList("[auction] " + (page + 1) + " " + encodeFilter(search) + " " + encodeFilter(seller)), Collections.emptyList());
        p.openInventory(view.getInventory());
    }
    private void claimReward(@NotNull Player p, int tier) {
        plugin.store().transaction(s -> {
            Profile profile = s.profile(p.getUniqueId()); String path = "rewards." + tier;
            if (!plugin.getConfig().isConfigurationSection(path) || plugin.tier(profile) < tier) throw new IllegalArgumentException("Награда пока недоступна");
            String free = "free:" + tier, premium = "premium:" + tier; long amount = 0;
            if (profile.rewards().add(free)) amount = Math.addExact(amount, plugin.getConfig().getLong(path + ".currency"));
            if (profile.pass().equals("premium") && profile.rewards().add(premium)) amount = Math.addExact(amount, plugin.getConfig().getLong(path + ".premium-currency"));
            if (amount < 0) throw new IllegalArgumentException("Некорректная награда");
            profile.balance(Math.addExact(profile.balance(), amount)); return true;
        }); open(p, "rewards");
    }
    public void claimMail(@NotNull Player p) {
        plugin.transfers().claim(p);
    }
    public void editor(@NotNull Player player, int index) { editor.editor(player, index); }
    private @NotNull String encodeFilter(@NotNull String value) { return value.isEmpty() ? "." : Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private @NotNull String decodeFilter(@NotNull String value) { return value.equals(".") ? "" : new String(Base64.getUrlDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8); }
    private @NotNull YamlConfiguration definition(@NotNull String name) { return Optional.ofNullable(definitions.get(name)).orElseThrow(() -> new IllegalArgumentException("Меню не найдено")); }
    private @NotNull String format(@NotNull Player p, @NotNull String text) { Profile profile = plugin.store().state().profile(p.getUniqueId()); return text.replace("{balance}", String.valueOf(profile.balance())).replace("{tier}", String.valueOf(plugin.tier(profile))).replace("{player}", p.getName()); }
    @NotNull ItemStack icon(@NotNull String material, @NotNull String name, @NotNull List<String> lore) {
        ItemStack item = new ItemStack(Optional.ofNullable(Material.matchMaterial(material)).filter(m -> !m.isAir()).orElse(Material.PAPER)); ItemMeta meta = Objects.requireNonNull(item.getItemMeta()); meta.setDisplayName(Colors.color(name)); meta.setLore(lore.stream().map(Colors::color).collect(Collectors.toList())); item.setItemMeta(meta); return item;
    }
    void set(@NotNull MenuView view, int slot, @NotNull ItemStack item, @NotNull List<String> left, @NotNull List<String> right) {
        if (slot < 0 || slot >= view.getInventory().getSize()) throw new IllegalArgumentException("Слот за границами меню");
        view.getInventory().setItem(slot, item); view.actions(slot, left, right);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void click(@NotNull InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof MenuView)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player) || e.getRawSlot() < 0 || e.getRawSlot() >= e.getView().getTopInventory().getSize() || (!e.isLeftClick() && !e.isRightClick()) || e.isShiftClick()) return;
        Player player = (Player) e.getWhoClicked(); MenuView view = (MenuView) e.getView().getTopInventory().getHolder();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != view.getInventory()) return;
            try { for (String action : view.actions(e.getRawSlot(), e.isRightClick())) actions.execute(player, format(player, action)); }
            catch (IllegalArgumentException | IllegalStateException | ArithmeticException error) { player.sendMessage(Colors.color("&c" + error.getMessage())); }
        });
    }
    @EventHandler public void drag(@NotNull InventoryDragEvent e) { if (e.getView().getTopInventory().getHolder() instanceof MenuView) e.setCancelled(true); }
}
