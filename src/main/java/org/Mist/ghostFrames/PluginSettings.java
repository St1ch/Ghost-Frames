package org.Mist.ghostFrames;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PluginSettings {
    private final JavaPlugin plugin;

    private String language;
    private boolean guiEnabled;
    private boolean clickThroughEnabled;
    private boolean playSounds;
    private boolean auditLogEnabled;
    private boolean itemProtectionEnabled;
    private int massEditRadius;
    private Set<Material> containerWhitelist = Collections.emptySet();
    private Set<Material> containerBlacklist = Collections.emptySet();

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        language = config.getString("language", "en");
        guiEnabled = config.getBoolean("gui.enabled", true);
        clickThroughEnabled = config.getBoolean("click-through.enabled", true);
        playSounds = config.getBoolean("sounds.enabled", true);
        auditLogEnabled = config.getBoolean("audit-log.enabled", true);
        itemProtectionEnabled = config.getBoolean("item-protection.enabled", true);
        massEditRadius = Math.max(1, config.getInt("mass-edit.max-radius", 16));
        containerWhitelist = readMaterialSet(config.getStringList("containers.whitelist"));
        containerBlacklist = readMaterialSet(config.getStringList("containers.blacklist"));
    }

    public String getLanguage() {
        return language;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public boolean isClickThroughEnabled() {
        return clickThroughEnabled;
    }

    public boolean isPlaySounds() {
        return playSounds;
    }

    public boolean isAuditLogEnabled() {
        return auditLogEnabled;
    }

    public boolean isItemProtectionEnabled() {
        return itemProtectionEnabled;
    }

    public int getMassEditRadius() {
        return massEditRadius;
    }

    public boolean isContainerAllowed(Material material) {
        if (!containerWhitelist.isEmpty() && !containerWhitelist.contains(material)) {
            return false;
        }
        return !containerBlacklist.contains(material);
    }

    private Set<Material> readMaterialSet(List<String> values) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String value : values) {
            Material material = Material.matchMaterial(value.toUpperCase(Locale.ROOT));
            if (material != null) {
                materials.add(material);
            }
        }
        return materials;
    }
}
