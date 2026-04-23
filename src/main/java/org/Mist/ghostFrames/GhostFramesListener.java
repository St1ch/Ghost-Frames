package org.Mist.ghostFrames;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public final class GhostFramesListener implements Listener {
    private final PluginSettings pluginSettings;
    private final MessageService messageService;
    private final FrameSettingsService frameSettingsService;
    private final InteractionAccessService interactionAccessService;

    public GhostFramesListener(
        GhostFrames plugin,
        PluginSettings pluginSettings,
        MessageService messageService,
        FrameSettingsService frameSettingsService,
        FrameBatchService frameBatchService
    ) {
        this.pluginSettings = pluginSettings;
        this.messageService = messageService;
        this.frameSettingsService = frameSettingsService;
        this.interactionAccessService = new InteractionAccessService(pluginSettings);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }

        Player player = event.getPlayer();
        frameSettingsService.applyStoredSettings(frame);

        if (player.isSneaking()) {
            if (!pluginSettings.isGuiEnabled()) {
                return;
            }
            event.setCancelled(true);
            if (!player.hasPermission("ghostframes.use")) {
                messageService.send(player, "no-permission");
                return;
            }
            openGui(player, frame);
            return;
        }

        if (frameSettingsService.isProtected(frame) && !player.hasPermission("ghostframes.bypass") && !player.isSneaking()) {
            event.setCancelled(true);
            return;
        }

        if (!frame.isVisible()) {
            event.setCancelled(true);
            openAttachedInventory(player, frame, event.getHand());
            return;
        }

        if (frameSettingsService.isItemProtected(frame) && !player.hasPermission("ghostframes.bypass")) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof GhostFrameMenu menu)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (!player.hasPermission("ghostframes.use")) {
            messageService.send(player, "no-permission");
            player.closeInventory();
            return;
        }

        if (slot == FrameSettingsService.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (slot != FrameSettingsService.MODE_SLOT
            && slot != FrameSettingsService.CLICK_THROUGH_SLOT
            && slot != FrameSettingsService.ITEM_PROTECTION_SLOT
            && slot != FrameSettingsService.FIXED_SLOT) {
            return;
        }

        Entity entity = Bukkit.getEntity(menu.getFrameId());
        if (!(entity instanceof ItemFrame frame)) {
            player.closeInventory();
            return;
        }

        frameSettingsService.applyStoredSettings(frame);
        if (slot == FrameSettingsService.MODE_SLOT) {
            frameSettingsService.cycleMode(frame, player.getName());
            messageService.send(player, "mode-changed", Map.of("mode", frameSettingsService.getMode(frame).getDisplayName()));
        } else if (slot == FrameSettingsService.CLICK_THROUGH_SLOT) {
            frameSettingsService.toggleClickThrough(frame, player.getName());
            messageService.send(player, "click-through-changed", Map.of("value", frameSettingsService.isClickThrough(frame) ? "enabled" : "disabled"));
        } else if (slot == FrameSettingsService.ITEM_PROTECTION_SLOT) {
            frameSettingsService.toggleItemProtection(frame, player.getName());
            messageService.send(player, "item-protection-changed", Map.of("value", frameSettingsService.isItemProtected(frame) ? "enabled" : "disabled"));
        } else {
            boolean changed = frameSettingsService.toggleFixed(frame, player.getName());
            messageService.send(player, "fixed-changed", Map.of("value", changed ? "updated" : "unsupported"));
        }

        menu.refresh(frame);
        playClick(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof GhostFrameMenu) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        frameSettingsService.applyToChunk(event.getChunk());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }

        frameSettingsService.applyStoredSettings(frame);
        if (!frameSettingsService.isProtected(frame)) {
            return;
        }

        if (event.getDamager() instanceof Player player && player.hasPermission("ghostframes.bypass")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }

        frameSettingsService.applyStoredSettings(frame);
        if (!frameSettingsService.isProtected(frame)) {
            return;
        }

        if (event.getRemover() instanceof Player player && player.hasPermission("ghostframes.bypass")) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }

        frameSettingsService.applyStoredSettings(frame);
        if (frameSettingsService.isProtected(frame)) {
            event.setCancelled(true);
        }
    }

    private void openGui(Player player, ItemFrame frame) {
        GhostFrameMenu menu = new GhostFrameMenu(frame, frameSettingsService);
        player.openInventory(menu.getInventory());
    }

    private void openAttachedInventory(Player player, ItemFrame frame, EquipmentSlot hand) {
        if (!player.hasPermission("ghostframes.use")) {
            return;
        }

        InventoryHolder holder = interactionAccessService.resolveAccessibleInventoryHolder(player, frame, hand, frameSettingsService);
        if (holder == null) {
            return;
        }

        player.openInventory(holder.getInventory());
    }

    private void playClick(Player player) {
        if (pluginSettings.isPlaySounds()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        }
    }
}
