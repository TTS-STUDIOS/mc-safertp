package com.ttsstudio.safertp.gui;

import com.ttsstudio.sdk.compat.Materials;
import com.ttsstudio.sdk.gui.Filler;
import com.ttsstudio.sdk.gui.GuiBuilder;
import com.ttsstudio.sdk.gui.GuiClickRouter;
import com.ttsstudio.sdk.text.Items;
import com.ttsstudio.safertp.config.WorldConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * /rtp without a world: one button per enabled world the player may use (config {@code menu}),
 * with its wait and cost. The title is MiniMessage: a pack glyph or {@code <lang_or>} draws the
 * background and the SDK leaves the glass frame out.
 */
public final class RtpMenu {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** What one button shows; {@code slot} from config, -1 when the world has none. */
    public record Entry(String world, String name, int slot, String icon, long waitSeconds, double cost,
                        int minRadius, int maxRadius) {}

    /** Menu settings from config.yml {@code menu:}. */
    public record Settings(boolean enabled, int rows, String title, Map<String, Integer> slots,
                           Map<String, String> icons, Map<String, String> models) {

        public static Settings from(ConfigurationSection menu) {
            if (menu == null) return new Settings(true, 3, "", defaultSlots(), defaultIcons(), Map.of());
            Map<String, Integer> slots = new LinkedHashMap<>(defaultSlots());
            ConfigurationSection s = menu.getConfigurationSection("slots");
            if (s != null) for (String k : s.getKeys(false)) slots.put(k, s.getInt(k));
            Map<String, String> icons = new LinkedHashMap<>(defaultIcons());
            ConfigurationSection i = menu.getConfigurationSection("icons");
            if (i != null) for (String k : i.getKeys(false)) icons.put(k, i.getString(k));
            Map<String, String> models = new LinkedHashMap<>();
            ConfigurationSection m = menu.getConfigurationSection("models");
            if (m != null) for (String k : m.getKeys(false)) models.put(k, m.getString(k));
            int rows = Math.max(1, Math.min(6, menu.getInt("rows", 3)));
            return new Settings(menu.getBoolean("enabled", true), rows, menu.getString("title", ""), slots, icons, models);
        }

        static Map<String, Integer> defaultSlots() {
            Map<String, Integer> m = new LinkedHashMap<>();
            m.put("world", 11);
            m.put("world_nether", 13);
            m.put("world_the_end", 15);
            return m;
        }

        static Map<String, String> defaultIcons() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("world", "GRASS_BLOCK");
            m.put("world_nether", "NETHERRACK");
            m.put("world_the_end", "END_STONE");
            return m;
        }
    }

    private RtpMenu() {}

    /**
     * Places the entries: a world with a configured slot gets it, the rest take the free
     * middle-row slots left to right. Slots outside the window are dropped.
     */
    public static Map<Integer, Entry> layout(List<Entry> entries, int rows) {
        Map<Integer, Entry> placed = new LinkedHashMap<>();
        int size = rows * 9;
        List<Entry> pending = new ArrayList<>();
        for (Entry e : entries) {
            if (e.slot() >= 0 && e.slot() < size && !placed.containsKey(e.slot())) placed.put(e.slot(), e);
            else pending.add(e);
        }
        int row = Math.min(rows - 1, rows / 2);
        for (Entry e : pending) {
            for (int col = 1; col < 8; col++) {
                int slot = row * 9 + col;
                if (!placed.containsKey(slot)) {
                    placed.put(slot, e);
                    break;
                }
            }
        }
        return placed;
    }

    /** Builds and opens the menu. {@code text} reads a lang key with placeholders; {@code travel} runs the /rtp. */
    public static void open(GuiClickRouter router, Player player, Settings settings, List<Entry> entries,
                            String langTitle, Function<String[], String> text, BiConsumer<Player, String> travel) {
        String title = settings.title() == null || settings.title().isBlank() ? langTitle : settings.title();
        GuiBuilder builder = GuiBuilder.create(settings.rows()).title(title).filler(Filler.PERIMETER);
        for (Map.Entry<Integer, Entry> e : layout(entries, settings.rows()).entrySet()) {
            Entry en = e.getValue();
            builder.item(e.getKey(), icon(en, settings, text), p -> {
                p.closeInventory();
                travel.accept(p, en.world());
            });
        }
        router.open(player, builder.build());
    }

    private static ItemStack icon(Entry e, Settings settings, Function<String[], String> text) {
        ItemStack stack = Materials.item(settings.icons().getOrDefault(e.world(), "COMPASS"));
        if (stack == null) stack = Materials.item("COMPASS");
        ItemMeta meta = stack.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize(text.apply(new String[]{"menu-radius",
            "min", String.valueOf(e.minRadius()), "max", String.valueOf(e.maxRadius())})));
        if (e.cost() > 0) {
            lore.add(MM.deserialize(text.apply(new String[]{"menu-cost",
                "amount", String.format(java.util.Locale.ROOT, "%.0f", e.cost())})));
        }
        lore.add(MM.deserialize(e.waitSeconds() > 0
            ? text.apply(new String[]{"menu-wait", "seconds", String.valueOf(e.waitSeconds())})
            : text.apply(new String[]{"menu-ready"})));
        lore.add(Component.empty());
        lore.add(MM.deserialize(text.apply(new String[]{"menu-click"})));
        Items.name(meta, MM.deserialize(text.apply(new String[]{"menu-world", "world", e.name()})));
        Items.lore(meta, lore);
        itemModel(meta, settings.models().get(e.world()));
        stack.setItemMeta(meta);
        return stack;
    }

    /** {@code item_model} (1.21.4+) from {@code menu.models}; older servers keep the plain icon. */
    static void itemModel(ItemMeta meta, String model) {
        if (meta == null || model == null || model.isBlank()) return;
        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(model.trim().toLowerCase(java.util.Locale.ROOT));
        java.lang.reflect.Method set = com.ttsstudio.sdk.compat.Reflect.findMethod(ItemMeta.class, "setItemModel",
            org.bukkit.NamespacedKey.class);
        if (key == null || set == null) return;
        com.ttsstudio.sdk.compat.Reflect.invoke(set, meta, key);
    }

    /** Entry for a configured world, from its settings. */
    public static Entry entry(WorldConfig c, String name, Settings settings, long waitSeconds) {
        return new Entry(c.worldName(), name, settings.slots().getOrDefault(c.worldName(), -1),
            settings.icons().getOrDefault(c.worldName(), "COMPASS"), waitSeconds, c.cost(), c.minRadius(), c.maxRadius());
    }
}
