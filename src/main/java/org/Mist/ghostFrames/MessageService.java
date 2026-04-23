package org.Mist.ghostFrames;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class MessageService {
    private final GhostFrames plugin;
    private YamlConfiguration messages;

    public MessageService(GhostFrames plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String language = plugin.getPluginSettings() == null ? "en" : plugin.getPluginSettings().getLanguage();
        File file = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
        if (!file.exists()) {
            file = new File(plugin.getDataFolder(), "messages_en.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    public String get(String path, Map<String, String> placeholders) {
        String value = messages.getString(path, "&cMissing message: " + path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
