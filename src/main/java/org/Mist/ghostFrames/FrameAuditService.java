package org.Mist.ghostFrames;

import org.bukkit.entity.ItemFrame;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public final class FrameAuditService {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final File auditFile;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FrameAuditService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.auditFile = new File(plugin.getDataFolder(), "audit.log");
    }

    public void logModeChange(String actor, ItemFrame frame, FrameMode mode) {
        log("mode", actor, frame, "mode=" + mode.name());
    }

    public void logClickThroughChange(String actor, ItemFrame frame, boolean value) {
        log("click-through", actor, frame, "enabled=" + value);
    }

    public void logFixedChange(String actor, ItemFrame frame, boolean value) {
        log("fixed", actor, frame, "enabled=" + value);
    }

    public void logItemProtectionChange(String actor, ItemFrame frame, boolean value) {
        log("item-protection", actor, frame, "enabled=" + value);
    }

    private void log(String action, String actor, ItemFrame frame, String details) {
        String line = "[" + formatter.format(LocalDateTime.now()) + "] "
            + action + " actor=" + actor
            + " frame=" + frame.getUniqueId()
            + " world=" + frame.getWorld().getName()
            + " x=" + frame.getLocation().getBlockX()
            + " y=" + frame.getLocation().getBlockY()
            + " z=" + frame.getLocation().getBlockZ()
            + " " + details;

        logger.info(line);
        appendToFile(line);
    }

    private void appendToFile(String line) {
        try {
            File parent = auditFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            Files.writeString(
                auditFile.toPath(),
                line + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write audit log: " + exception.getMessage());
        }
    }
}
