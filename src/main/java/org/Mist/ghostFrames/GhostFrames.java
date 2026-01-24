package org.Mist.ghostFrames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostFrames extends JavaPlugin implements Listener {
    private static final String GUI_TITLE = "Ghost Frames";
    private final Map<UUID, UUID> editingFrames = new HashMap<>();
    private NamespacedKey visibilityKey;
    private NamespacedKey fixedKey;
    private Method fixedGetter;
    private Method fixedSetter;

    @Override
    public void onEnable() {
        visibilityKey = new NamespacedKey(this, "ghostframes_visibility");
        fixedKey = new NamespacedKey(this, "ghostframes_fixed");
        fixedGetter = resolveMethod(ItemFrame.class, "isFixed");
        fixedSetter = resolveMethod(ItemFrame.class, "setFixed", boolean.class);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }

        Player player = event.getPlayer();
        applyStoredSettings(frame);
        if (player.isSneaking()) {
            event.setCancelled(true);
            openGui(player, frame);
            return;
        }

        if (!frame.isVisible()) {
            event.setCancelled(true);
            openAttachedInventory(player, frame);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        UUID frameId = editingFrames.get(player.getUniqueId());
        if (frameId == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(frameId);
        if (!(entity instanceof ItemFrame frame)) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 3) {
            frame.setVisible(!frame.isVisible());
            setStoredVisibility(frame, frame.isVisible());
        } else if (slot == 5) {
            if (supportsFixed()) {
                boolean newValue = !getFixed(frame);
                setFixed(frame, newValue);
                setStoredFixed(frame, newValue);
            }
        }

        Inventory inventory = event.getInventory();
        inventory.setItem(3, createVisibilityItem(frame));
        inventory.setItem(5, createFixedItem(frame));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        editingFrames.remove(event.getPlayer().getUniqueId());
    }

    private void openGui(Player player, ItemFrame frame) {
        applyStoredSettings(frame);
        Inventory inventory = Bukkit.createInventory(null, 9, GUI_TITLE);
        inventory.setItem(3, createVisibilityItem(frame));
        inventory.setItem(5, createFixedItem(frame));
        editingFrames.put(player.getUniqueId(), frame.getUniqueId());
        player.openInventory(inventory);
    }

    private void openAttachedInventory(Player player, ItemFrame frame) {
        Block block = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
        BlockState state = block.getState();
        if (state instanceof InventoryHolder holder) {
            player.openInventory(holder.getInventory());
        }
    }

    private ItemStack createVisibilityItem(ItemFrame frame) {
        boolean visible = frame.isVisible();
        Material material = visible ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "Frame visibility: " + (visible ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFixedItem(ItemFrame frame) {
        boolean supported = supportsFixed();
        boolean fixed = supported && getFixed(frame);
        Material material = fixed ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (supported) {
                meta.setDisplayName(ChatColor.WHITE + "Item rotation lock: " + (fixed ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
            } else {
                meta.setDisplayName(ChatColor.WHITE + "Item rotation lock: " + ChatColor.DARK_GRAY + "unsupported");
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyStoredSettings(ItemFrame frame) {
        PersistentDataContainer container = frame.getPersistentDataContainer();
        Byte visibility = container.get(visibilityKey, PersistentDataType.BYTE);
        if (visibility != null) {
            frame.setVisible(visibility == 1);
        }
        Byte fixed = container.get(fixedKey, PersistentDataType.BYTE);
        if (fixed != null && supportsFixed()) {
            setFixed(frame, fixed == 1);
        }
    }

    private void setStoredVisibility(ItemFrame frame, boolean visible) {
        frame.getPersistentDataContainer().set(visibilityKey, PersistentDataType.BYTE, visible ? (byte) 1 : (byte) 0);
    }

    private void setStoredFixed(ItemFrame frame, boolean fixed) {
        frame.getPersistentDataContainer().set(fixedKey, PersistentDataType.BYTE, fixed ? (byte) 1 : (byte) 0);
    }

    private boolean supportsFixed() {
        return fixedGetter != null && fixedSetter != null;
    }

    private boolean getFixed(ItemFrame frame) {
        if (!supportsFixed()) {
            return false;
        }
        try {
            return (boolean) fixedGetter.invoke(frame);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void setFixed(ItemFrame frame, boolean value) {
        if (!supportsFixed()) {
            return;
        }
        try {
            fixedSetter.invoke(frame, value);
        } catch (Exception ignored) {
        }
    }

    private Method resolveMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (Exception ignored) {
            return null;
        }
    }
}
