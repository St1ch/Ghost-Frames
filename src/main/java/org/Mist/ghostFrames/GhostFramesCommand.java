package org.Mist.ghostFrames;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GhostFramesCommand implements CommandExecutor, TabCompleter {
    private final GhostFrames plugin;
    private final PluginSettings pluginSettings;
    private final MessageService messageService;
    private final FrameSettingsService frameSettingsService;
    private final FrameBatchService frameBatchService;

    public GhostFramesCommand(
        GhostFrames plugin,
        PluginSettings pluginSettings,
        MessageService messageService,
        FrameSettingsService frameSettingsService,
        FrameBatchService frameBatchService
    ) {
        this.plugin = plugin;
        this.pluginSettings = pluginSettings;
        this.messageService = messageService;
        this.frameSettingsService = frameSettingsService;
        this.frameBatchService = frameBatchService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            case "scan":
                return handleScan(sender, args);
            case "applyall":
                return handleApplyAll(sender, args);
            case "setmode":
                return handleSetMode(sender, args);
            case "inspect":
                return handleInspect(sender);
            case "reset":
                return handleReset(sender);
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }

        plugin.reloadPlugin();
        messageService.send(sender, "reload-success");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }

        int loadedFrames = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedFrames += world.getEntitiesByClass(ItemFrame.class).size();
        }

        messageService.send(sender, "info.header");
        messageService.send(sender, "info.loaded-frames", Map.of("count", String.valueOf(loadedFrames)));
        messageService.send(sender, "info.gui", Map.of("value", yesNo(pluginSettings.isGuiEnabled())));
        messageService.send(sender, "info.click-through", Map.of("value", yesNo(pluginSettings.isClickThroughEnabled())));
        messageService.send(sender, "info.audit-log", Map.of("value", yesNo(pluginSettings.isAuditLogEnabled())));
        messageService.send(sender, "info.fixed-support", Map.of("value", yesNo(frameSettingsService.supportsFixed())));
        messageService.send(sender, "info.hooks", Map.of("value", detectHooks()));
        return true;
    }

    private boolean handleScan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            messageService.send(sender, "usage.scan");
            return true;
        }

        String scope = args[1].toLowerCase(Locale.ROOT);
        int count;
        if ("chunk".equals(scope)) {
            if (!(sender instanceof Player player)) {
                messageService.send(sender, "player-only");
                return true;
            }
            count = frameBatchService.countInChunk(player.getLocation().getChunk());
        } else if ("radius".equals(scope)) {
            if (!(sender instanceof Player player)) {
                messageService.send(sender, "player-only");
                return true;
            }
            int radius = parseRadius(args, 2);
            if (radius < 1 || radius > pluginSettings.getMassEditRadius()) {
                messageService.send(sender, "invalid-radius", Map.of("max", String.valueOf(pluginSettings.getMassEditRadius())));
                return true;
            }
            count = frameBatchService.countInRadius(player, radius);
        } else if ("world".equals(scope)) {
            World world = resolveWorld(sender, args, 2);
            if (world == null) {
                messageService.send(sender, "unknown-world");
                return true;
            }
            count = frameBatchService.countInWorld(world);
        } else {
            messageService.send(sender, "usage.scan");
            return true;
        }

        messageService.send(sender, "scan-result", Map.of("scope", scope, "count", String.valueOf(count)));
        return true;
    }

    private boolean handleApplyAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            messageService.send(sender, "usage.applyall");
            return true;
        }

        String actor = sender.getName();
        String scope = args[1].toLowerCase(Locale.ROOT);
        FrameMode mode = FrameMode.fromName(args[2]);
        if (mode == null) {
            messageService.send(sender, "invalid-mode");
            return true;
        }

        int changed;
        if ("chunk".equals(scope)) {
            if (!(sender instanceof Player player)) {
                messageService.send(sender, "player-only");
                return true;
            }
            changed = frameBatchService.applyInChunk(player.getLocation().getChunk(), mode, actor);
        } else if ("radius".equals(scope)) {
            if (!(sender instanceof Player player)) {
                messageService.send(sender, "player-only");
                return true;
            }
            int radius = parseRadius(args, 3);
            if (radius < 1 || radius > pluginSettings.getMassEditRadius()) {
                messageService.send(sender, "invalid-radius", Map.of("max", String.valueOf(pluginSettings.getMassEditRadius())));
                return true;
            }
            changed = frameBatchService.applyInRadius(player, radius, mode, actor);
        } else if ("world".equals(scope)) {
            World world = resolveWorld(sender, args, 3);
            if (world == null) {
                messageService.send(sender, "unknown-world");
                return true;
            }
            changed = frameBatchService.applyInWorld(world, mode, actor);
        } else {
            messageService.send(sender, "usage.applyall");
            return true;
        }

        messageService.send(sender, "applyall-success", Map.of("scope", scope, "count", String.valueOf(changed), "mode", mode.getDisplayName()));
        return true;
    }

    private boolean handleSetMode(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage.setmode");
            return true;
        }

        FrameMode mode = FrameMode.fromName(args[1]);
        if (mode == null) {
            messageService.send(sender, "invalid-mode");
            return true;
        }

        ItemFrame frame = getTargetFrame(player);
        if (frame == null) {
            messageService.send(sender, "no-target-frame");
            return true;
        }

        frameSettingsService.applyMode(frame, mode, sender.getName());
        messageService.send(sender, "setmode-success", Map.of("mode", mode.getDisplayName()));
        return true;
    }

    private boolean handleInspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }

        ItemFrame frame = getTargetFrame(player);
        if (frame == null) {
            messageService.send(sender, "no-target-frame");
            return true;
        }

        frameSettingsService.applyStoredSettings(frame);
        messageService.send(sender, "inspect.header");
        messageService.send(sender, "inspect.mode", Map.of("value", frameSettingsService.getMode(frame).getDisplayName()));
        messageService.send(sender, "inspect.click-through", Map.of("value", yesNo(frameSettingsService.isClickThrough(frame))));
        messageService.send(sender, "inspect.item-protection", Map.of("value", yesNo(frameSettingsService.isItemProtected(frame))));
        String fixedValue = frameSettingsService.supportsFixed()
            ? yesNo(frameSettingsService.isFixedEnabled(frame))
            : ChatColor.DARK_GRAY + "unsupported";
        messageService.send(sender, "inspect.fixed", Map.of("value", fixedValue));
        messageService.send(sender, "inspect.editor", Map.of("value", frameSettingsService.getLastEditor(frame)));
        messageService.send(sender, "inspect.updated", Map.of("value", frameSettingsService.getLastUpdated(frame)));
        return true;
    }

    private boolean handleReset(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return true;
        }
        if (!sender.hasPermission("ghostframes.admin")) {
            messageService.send(sender, "no-permission");
            return true;
        }

        ItemFrame frame = getTargetFrame(player);
        if (frame == null) {
            messageService.send(sender, "no-target-frame");
            return true;
        }

        frameSettingsService.resetFrame(frame, sender.getName());
        messageService.send(sender, "reset-success");
        return true;
    }

    private World resolveWorld(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            return Bukkit.getWorld(args[index]);
        }
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        return null;
    }

    private int parseRadius(String[] args, int index) {
        if (args.length <= index) {
            return pluginSettings.getMassEditRadius();
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "GhostFrames commands:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " scan <chunk|radius|world> [radius/world]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " applyall <chunk|radius|world> <mode> [radius/world]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " setmode <mode>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " inspect");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reset");
    }

    private String detectHooks() {
        List<String> hooks = new ArrayList<>();
        for (String pluginName : Arrays.asList("WorldGuard", "GriefPrevention", "Lands", "Residence")) {
            if (Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                hooks.add(pluginName);
            }
        }
        return hooks.isEmpty() ? ChatColor.RED + "none" : ChatColor.GREEN + String.join(", ", hooks);
    }

    private String yesNo(boolean value) {
        return value ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "info", "scan", "applyall", "setmode", "inspect", "reset"), args[0]);
        }
        if (args.length == 2 && ("scan".equalsIgnoreCase(args[0]) || "applyall".equalsIgnoreCase(args[0]))) {
            return filter(List.of("chunk", "radius", "world"), args[1]);
        }
        if ((args.length == 3 && "applyall".equalsIgnoreCase(args[0]))
            || (args.length == 2 && "setmode".equalsIgnoreCase(args[0]))) {
            List<String> values = new ArrayList<>();
            for (FrameMode mode : FrameMode.values()) {
                values.add(mode.name().toLowerCase(Locale.ROOT));
            }
            return filter(values, args[args.length - 1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private ItemFrame getTargetFrame(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            8.0,
            entity -> entity instanceof ItemFrame
        );
        if (result == null) {
            return null;
        }

        Entity entity = result.getHitEntity();
        return entity instanceof ItemFrame frame ? frame : null;
    }
}
