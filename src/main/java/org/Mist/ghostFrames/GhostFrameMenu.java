package org.Mist.ghostFrames;

import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class GhostFrameMenu implements InventoryHolder {
    private static final String GUI_TITLE = "Ghost Frames";

    private final UUID frameId;
    private final FrameSettingsService frameSettingsService;
    private final Inventory inventory;

    public GhostFrameMenu(ItemFrame frame, FrameSettingsService frameSettingsService) {
        this.frameId = frame.getUniqueId();
        this.frameSettingsService = frameSettingsService;
        this.inventory = Bukkit.createInventory(this, 9, GUI_TITLE);
        refresh(frame);
    }

    public UUID getFrameId() {
        return frameId;
    }

    public void refresh(ItemFrame frame) {
        inventory.setItem(0, frameSettingsService.createPreviewItem(frame));
        inventory.setItem(FrameSettingsService.MODE_SLOT, frameSettingsService.createModeItem(frame));
        inventory.setItem(FrameSettingsService.CLICK_THROUGH_SLOT, frameSettingsService.createClickThroughItem(frame));
        inventory.setItem(FrameSettingsService.ITEM_PROTECTION_SLOT, frameSettingsService.createItemProtectionItem(frame));
        inventory.setItem(FrameSettingsService.FIXED_SLOT, frameSettingsService.createFixedItem(frame));
        inventory.setItem(FrameSettingsService.CLOSE_SLOT, frameSettingsService.createCloseItem());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
