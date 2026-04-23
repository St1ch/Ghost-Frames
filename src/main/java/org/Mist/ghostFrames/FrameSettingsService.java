package org.Mist.ghostFrames;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class FrameSettingsService {
    public static final int MODE_SLOT = 2;
    public static final int CLICK_THROUGH_SLOT = 3;
    public static final int ITEM_PROTECTION_SLOT = 4;
    public static final int FIXED_SLOT = 5;
    public static final int CLOSE_SLOT = 8;

    private final PluginSettings pluginSettings;
    private final FrameAuditService frameAuditService;
    private final NamespacedKey modeKey;
    private final NamespacedKey visibilityKey;
    private final NamespacedKey fixedKey;
    private final NamespacedKey clickThroughKey;
    private final NamespacedKey itemProtectionKey;
    private final NamespacedKey lastEditorKey;
    private final NamespacedKey lastUpdatedKey;
    private final Method fixedGetter;
    private final Method fixedSetter;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public FrameSettingsService(JavaPlugin plugin, PluginSettings pluginSettings, FrameAuditService frameAuditService) {
        this.pluginSettings = pluginSettings;
        this.frameAuditService = frameAuditService;
        this.modeKey = new NamespacedKey(plugin, "ghostframes_mode");
        this.visibilityKey = new NamespacedKey(plugin, "ghostframes_visibility");
        this.fixedKey = new NamespacedKey(plugin, "ghostframes_fixed");
        this.clickThroughKey = new NamespacedKey(plugin, "ghostframes_click_through");
        this.itemProtectionKey = new NamespacedKey(plugin, "ghostframes_item_protection");
        this.lastEditorKey = new NamespacedKey(plugin, "ghostframes_last_editor");
        this.lastUpdatedKey = new NamespacedKey(plugin, "ghostframes_last_updated");
        this.fixedGetter = resolveMethod(ItemFrame.class, "isFixed");
        this.fixedSetter = resolveMethod(ItemFrame.class, "setFixed", boolean.class);
    }

    public void applyToChunk(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame frame) {
                applyStoredSettings(frame);
            }
        }
    }

    public void applyStoredSettings(ItemFrame frame) {
        PersistentDataContainer container = frame.getPersistentDataContainer();
        String modeName = container.get(modeKey, PersistentDataType.STRING);
        if (modeName != null) {
            FrameMode mode = FrameMode.fromName(modeName);
            if (mode != null) {
                applyModeState(frame, mode);
                return;
            }
        }

        Byte visibility = container.get(visibilityKey, PersistentDataType.BYTE);
        if (visibility != null) {
            frame.setVisible(visibility == 1);
        }

        Byte fixed = container.get(fixedKey, PersistentDataType.BYTE);
        if (fixed != null && supportsFixed()) {
            setFixed(frame, fixed == 1);
        }
    }

    public void applyMode(ItemFrame frame, FrameMode mode, String actor) {
        applyModeState(frame, mode);
        PersistentDataContainer container = frame.getPersistentDataContainer();
        container.set(modeKey, PersistentDataType.STRING, mode.name());
        container.set(visibilityKey, PersistentDataType.BYTE, toByte(frame.isVisible()));
        container.set(clickThroughKey, PersistentDataType.BYTE, toByte(isClickThrough(frame)));
        container.set(itemProtectionKey, PersistentDataType.BYTE, toByte(isItemProtected(frame)));
        if (supportsFixed()) {
            container.set(fixedKey, PersistentDataType.BYTE, toByte(getFixed(frame)));
        }
        container.set(lastEditorKey, PersistentDataType.STRING, actor);
        container.set(lastUpdatedKey, PersistentDataType.LONG, System.currentTimeMillis());
        if (pluginSettings.isAuditLogEnabled()) {
            frameAuditService.logModeChange(actor, frame, mode);
        }
    }

    public void cycleMode(ItemFrame frame, String actor) {
        applyMode(frame, getMode(frame).next(), actor);
    }

    public void toggleClickThrough(ItemFrame frame, String actor) {
        boolean newValue = !isClickThrough(frame);
        frame.getPersistentDataContainer().set(clickThroughKey, PersistentDataType.BYTE, toByte(newValue));
        frame.getPersistentDataContainer().set(lastEditorKey, PersistentDataType.STRING, actor);
        frame.getPersistentDataContainer().set(lastUpdatedKey, PersistentDataType.LONG, System.currentTimeMillis());
        if (pluginSettings.isAuditLogEnabled()) {
            frameAuditService.logClickThroughChange(actor, frame, newValue);
        }
    }

    public void toggleItemProtection(ItemFrame frame, String actor) {
        boolean newValue = !isItemProtected(frame);
        frame.getPersistentDataContainer().set(itemProtectionKey, PersistentDataType.BYTE, toByte(newValue));
        frame.getPersistentDataContainer().set(lastEditorKey, PersistentDataType.STRING, actor);
        frame.getPersistentDataContainer().set(lastUpdatedKey, PersistentDataType.LONG, System.currentTimeMillis());
        if (pluginSettings.isAuditLogEnabled()) {
            frameAuditService.logItemProtectionChange(actor, frame, newValue);
        }
    }

    public boolean toggleFixed(ItemFrame frame, String actor) {
        if (!supportsFixed()) {
            return false;
        }

        boolean newValue = !getFixed(frame);
        setFixed(frame, newValue);
        frame.getPersistentDataContainer().set(fixedKey, PersistentDataType.BYTE, toByte(newValue));
        frame.getPersistentDataContainer().set(lastEditorKey, PersistentDataType.STRING, actor);
        frame.getPersistentDataContainer().set(lastUpdatedKey, PersistentDataType.LONG, System.currentTimeMillis());
        if (pluginSettings.isAuditLogEnabled()) {
            frameAuditService.logFixedChange(actor, frame, newValue);
        }
        return true;
    }

    public void resetFrame(ItemFrame frame, String actor) {
        applyMode(frame, FrameMode.NORMAL, actor);
        PersistentDataContainer container = frame.getPersistentDataContainer();
        container.set(clickThroughKey, PersistentDataType.BYTE, toByte(pluginSettings.isClickThroughEnabled()));
        container.set(itemProtectionKey, PersistentDataType.BYTE, toByte(false));
        if (supportsFixed()) {
            container.set(fixedKey, PersistentDataType.BYTE, toByte(false));
        }
        container.set(lastEditorKey, PersistentDataType.STRING, actor);
        container.set(lastUpdatedKey, PersistentDataType.LONG, System.currentTimeMillis());
    }

    public ItemStack createModeItem(ItemFrame frame) {
        FrameMode mode = getMode(frame);
        Material material = switch (mode) {
            case NORMAL -> Material.ITEM_FRAME;
            case GHOST -> Material.GLASS;
            case LOCKED -> Material.CHAIN;
            case PROTECTED -> Material.SHIELD;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Frame mode: " + ChatColor.GOLD + mode.getDisplayName());
            meta.setLore(colorizeLore(
                "&7Click to cycle through modes.",
                "&fNormal: &7Visible frame",
                "&fGhost: &7Invisible + container access",
                "&fLocked: &7Ghost + rotation lock",
                "&fProtected: &7Locked + anti-break"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createClickThroughItem(ItemFrame frame) {
        boolean enabled = isClickThrough(frame);
        Material material = enabled ? Material.ENDER_CHEST : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Click-through: " + (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            meta.setLore(colorizeLore(
                "&7Allows opening the attached container",
                "&7through an invisible frame."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createItemProtectionItem(ItemFrame frame) {
        boolean enabled = isItemProtected(frame);
        Material material = enabled ? Material.NETHERITE_CHESTPLATE : Material.LEATHER_CHESTPLATE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Item protection: " + (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            meta.setLore(colorizeLore(
                "&7Prevents players from rotating or taking",
                "&7the displayed item when protection is active."
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createFixedItem(ItemFrame frame) {
        boolean supported = supportsFixed();
        boolean fixed = supported && getFixed(frame);
        Material material = fixed ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (supported) {
                meta.setDisplayName(ChatColor.WHITE + "Item rotation lock: " + (fixed ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
                meta.setLore(colorizeLore("&7Prevents rotation changes when supported by the server."));
            } else {
                meta.setDisplayName(ChatColor.WHITE + "Item rotation lock: " + ChatColor.DARK_GRAY + "unsupported");
                meta.setLore(colorizeLore("&7Your server version does not expose fixed item frames."));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Close");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPreviewItem(ItemFrame frame) {
        ItemStack item = frame.getItem();
        if (item == null || item.getType() == Material.AIR) {
            item = new ItemStack(Material.PAINTING);
        } else {
            item = item.clone();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Displayed item");
            meta.setLore(colorizeLore(
                "&7Mode: &f" + getMode(frame).getDisplayName(),
                "&7Click-through: " + (isClickThrough(frame) ? "&aenabled" : "&cdisabled"),
                "&7Item protection: " + (isItemProtected(frame) ? "&aenabled" : "&cdisabled"),
                "&7Last editor: &f" + getLastEditor(frame),
                "&7Last update: &f" + getLastUpdated(frame)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    public FrameMode getMode(ItemFrame frame) {
        String modeName = frame.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        FrameMode mode = modeName == null ? null : FrameMode.fromName(modeName);
        if (mode != null) {
            return mode;
        }
        return frame.isVisible() ? FrameMode.NORMAL : FrameMode.GHOST;
    }

    public boolean isClickThrough(ItemFrame frame) {
        Byte clickThrough = frame.getPersistentDataContainer().get(clickThroughKey, PersistentDataType.BYTE);
        return clickThrough == null ? pluginSettings.isClickThroughEnabled() : clickThrough == 1;
    }

    public boolean isProtected(ItemFrame frame) {
        return getMode(frame) == FrameMode.PROTECTED;
    }

    public boolean isItemProtected(ItemFrame frame) {
        Byte value = frame.getPersistentDataContainer().get(itemProtectionKey, PersistentDataType.BYTE);
        if (value != null) {
            return value == 1;
        }
        return pluginSettings.isItemProtectionEnabled() && isProtected(frame);
    }

    public boolean isFixedEnabled(ItemFrame frame) {
        return supportsFixed() && getFixed(frame);
    }

    public String getLastEditor(ItemFrame frame) {
        String value = frame.getPersistentDataContainer().get(lastEditorKey, PersistentDataType.STRING);
        return value == null ? "Unknown" : value;
    }

    public String getLastUpdated(ItemFrame frame) {
        Long timestamp = frame.getPersistentDataContainer().get(lastUpdatedKey, PersistentDataType.LONG);
        return timestamp == null ? "Never" : formatter.format(Instant.ofEpochMilli(timestamp));
    }

    public boolean supportsFixed() {
        return fixedGetter != null && fixedSetter != null;
    }

    private void applyModeState(ItemFrame frame, FrameMode mode) {
        switch (mode) {
            case NORMAL -> {
                frame.setVisible(true);
                if (supportsFixed()) {
                    setFixed(frame, false);
                }
            }
            case GHOST -> {
                frame.setVisible(false);
                if (supportsFixed()) {
                    setFixed(frame, false);
                }
            }
            case LOCKED, PROTECTED -> {
                frame.setVisible(false);
                if (supportsFixed()) {
                    setFixed(frame, true);
                }
            }
        }
    }

    private List<String> colorizeLore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return lore;
    }

    private boolean getFixed(ItemFrame frame) {
        try {
            return supportsFixed() && (boolean) fixedGetter.invoke(frame);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void setFixed(ItemFrame frame, boolean value) {
        try {
            if (supportsFixed()) {
                fixedSetter.invoke(frame, value);
            }
        } catch (Exception ignored) {
        }
    }

    private static byte toByte(boolean value) {
        return value ? (byte) 1 : (byte) 0;
    }

    private static Method resolveMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (Exception ignored) {
            return null;
        }
    }
}
