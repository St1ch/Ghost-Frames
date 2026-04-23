package org.Mist.ghostFrames;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public final class FrameBatchService {
    private final FrameSettingsService frameSettingsService;

    public FrameBatchService(FrameSettingsService frameSettingsService) {
        this.frameSettingsService = frameSettingsService;
    }

    public int applyInRadius(Player player, int radius, FrameMode mode, String actor) {
        int changed = 0;
        double maxDistanceSquared = radius * radius;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof ItemFrame frame && frame.getLocation().distanceSquared(player.getLocation()) <= maxDistanceSquared) {
                frameSettingsService.applyMode(frame, mode, actor);
                changed++;
            }
        }
        return changed;
    }

    public int applyInChunk(Chunk chunk, FrameMode mode, String actor) {
        int changed = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame frame) {
                frameSettingsService.applyMode(frame, mode, actor);
                changed++;
            }
        }
        return changed;
    }

    public int applyInWorld(World world, FrameMode mode, String actor) {
        int changed = 0;
        for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
            frameSettingsService.applyMode(frame, mode, actor);
            changed++;
        }
        return changed;
    }

    public int countInRadius(Player player, int radius) {
        int count = 0;
        double maxDistanceSquared = radius * radius;
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity instanceof ItemFrame frame && frame.getLocation().distanceSquared(player.getLocation()) <= maxDistanceSquared) {
                count++;
            }
        }
        return count;
    }

    public int countInChunk(Chunk chunk) {
        int count = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame) {
                count++;
            }
        }
        return count;
    }

    public int countInWorld(World world) {
        return world.getEntitiesByClass(ItemFrame.class).size();
    }
}
