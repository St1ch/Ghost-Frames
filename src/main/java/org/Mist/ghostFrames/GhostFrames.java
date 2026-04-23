package org.Mist.ghostFrames;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GhostFrames extends JavaPlugin {
    private PluginSettings pluginSettings;
    private MessageService messageService;
    private FrameSettingsService frameSettingsService;
    private FrameBatchService frameBatchService;
    private FrameAuditService frameAuditService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages_en.yml", false);
        saveResource("messages_ru.yml", false);

        pluginSettings = new PluginSettings(this);
        messageService = new MessageService(this);
        frameAuditService = new FrameAuditService(this);
        frameSettingsService = new FrameSettingsService(this, pluginSettings, frameAuditService);
        frameBatchService = new FrameBatchService(frameSettingsService);

        Bukkit.getPluginManager().registerEvents(
            new GhostFramesListener(this, pluginSettings, messageService, frameSettingsService, frameBatchService),
            this
        );

        PluginCommand command = getCommand("ghostframes");
        if (command != null) {
            GhostFramesCommand executor = new GhostFramesCommand(this, pluginSettings, messageService, frameSettingsService, frameBatchService);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        applySettingsToLoadedChunks();
    }

    public void reloadPlugin() {
        reloadConfig();
        pluginSettings.reload();
        messageService.reload();
        applySettingsToLoadedChunks();
    }

    public PluginSettings getPluginSettings() {
        return pluginSettings;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public FrameSettingsService getFrameSettingsService() {
        return frameSettingsService;
    }

    public FrameBatchService getFrameBatchService() {
        return frameBatchService;
    }

    private void applySettingsToLoadedChunks() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                frameSettingsService.applyToChunk(chunk);
            }
        }
    }
}
