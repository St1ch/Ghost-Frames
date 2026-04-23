package org.Mist.ghostFrames;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class InteractionAccessService {
    private final PluginSettings pluginSettings;

    public InteractionAccessService(PluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    public InventoryHolder resolveAccessibleInventoryHolder(Player player, ItemFrame frame, EquipmentSlot hand, FrameSettingsService frameSettingsService) {
        if (!pluginSettings.isClickThroughEnabled() || !frameSettingsService.isClickThrough(frame)) {
            return null;
        }

        Block attachedBlock = getAttachedBlock(frame);
        if (!pluginSettings.isContainerAllowed(attachedBlock.getType())) {
            return null;
        }

        BlockState state = attachedBlock.getState();
        if (!(state instanceof InventoryHolder holder)) {
            return null;
        }

        PlayerInteractEvent blockInteractEvent = createBlockInteractEvent(player, attachedBlock, frame, hand);
        Bukkit.getPluginManager().callEvent(blockInteractEvent);
        if (blockInteractEvent.isCancelled() || blockInteractEvent.useInteractedBlock() == Result.DENY) {
            return null;
        }

        return holder;
    }

    private PlayerInteractEvent createBlockInteractEvent(Player player, Block attachedBlock, ItemFrame frame, EquipmentSlot hand) {
        ItemStack item = hand == null ? null : player.getInventory().getItem(hand);
        BlockFace clickedFace = frame.getAttachedFace().getOppositeFace();
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, attachedBlock, clickedFace, hand);
        event.setUseItemInHand(Result.DENY);
        return event;
    }

    private Block getAttachedBlock(ItemFrame frame) {
        return frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
    }
}
