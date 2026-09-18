package com.frank.livegroovy;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaletteItems {
    private static final Pattern ENTRY_PATTERN = Pattern.compile(
        "\\\"([^\\\"]+)\\\"\\s*:\\s*\\{[^{}]*?" +
        "\\\"base_item\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?" +
        "\\\"custom_model_data\\\"\\s*:\\s*(\\d+).*?" +
        "\\\"java_material\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?" +
        "\\\"java_item_model\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?" +
        "\\\"bedrock_texture\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?" +
        "\\\"bedrock_identifier\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"",
        Pattern.DOTALL
    );

    private final JavaPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final Map<String, PaletteItem> byId;

    public PaletteItems(JavaPlugin plugin, Path manifestPath) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, "palette_item");
        this.byId = load(plugin, manifestPath);
    }

    public PaletteItem get(String id) {
        return byId.get(id);
    }

    public Map<String, PaletteItem> all() {
        return Collections.unmodifiableMap(byId);
    }

    public ItemStack create(String id) {
        return create(id, 1);
    }

    public ItemStack create(String id, int amount) {
        PaletteItem item = byId.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown palette item: " + id);
        }
        Material material = Material.matchMaterial(item.baseItem().toUpperCase(Locale.ROOT));
        if (material == null) {
            throw new IllegalArgumentException("Unknown Bukkit material for palette item " + id + ": " + item.baseItem());
        }
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(item.customModelData());
            NamespacedKey itemModel = NamespacedKey.fromString(item.javaItemModel());
            if (itemModel == null) {
                throw new IllegalArgumentException("Invalid item model key for palette item " + id + ": " + item.javaItemModel());
            }
            meta.setItemModel(itemModel);
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean is(ItemStack stack, String id) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        String stored = stack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return id.equals(stored);
    }

    private static Map<String, PaletteItem> load(JavaPlugin plugin, Path manifestPath) {
        Map<String, PaletteItem> result = new HashMap<>();
        if (!Files.isRegularFile(manifestPath)) {
            plugin.getLogger().warning("Palette association manifest not found: " + manifestPath);
            return result;
        }
        try {
            String json = Files.readString(manifestPath);
            int byIdStart = json.indexOf("\"by_id\"");
            if (byIdStart < 0) {
                return result;
            }
            Matcher matcher = ENTRY_PATTERN.matcher(json.substring(byIdStart));
            while (matcher.find()) {
                String id = matcher.group(1);
                result.put(id, new PaletteItem(
                    id,
                    matcher.group(2),
                    matcher.group(4),
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(5),
                    matcher.group(6),
                    matcher.group(7)
                ));
            }
            plugin.getLogger().info("Loaded " + result.size() + " palette item associations");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to read palette association manifest " + manifestPath, e);
        }
        return result;
    }
}
