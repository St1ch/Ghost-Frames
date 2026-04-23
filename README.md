# Ghost Frames

![Minecraft](https://img.shields.io/badge/Minecraft-1.18--1.21.x-67D08B?style=for-the-badge)
![Platforms](https://img.shields.io/badge/Platforms-Bukkit%20%7C%20Spigot%20%7C%20Paper%20%7C%20Folia-6AD3FF?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17-FFD166?style=for-the-badge)
![Modes](https://img.shields.io/badge/Frame%20Modes-4-8FF7D2?style=for-the-badge)
![Config](https://img.shields.io/badge/Config-YAML-E6F0FF?style=for-the-badge&color=3A4D66)
![License](https://img.shields.io/badge/License-MIT-C7A4FF?style=for-the-badge)

Ghost Frames turns item frames into configurable ghost displays. Players can hide frames, safely click through them into containers, lock rotation, protect displayed items, and manage everything through a compact in-game GUI or admin tools.

If you want decorative storage walls, cleaner shop displays, or protected framed indicators without sacrificing usability, this plugin is built for that exact workflow.

## Highlights

- Four frame modes: `NORMAL`, `GHOST`, `LOCKED`, `PROTECTED`
- Click-through access for containers behind invisible frames
- Item protection to prevent rotating or taking the displayed item
- Shift + Right Click GUI with preview, toggles, and status indicators
- Bulk admin tools for chunk, radius, and world scopes
- Audit logging to console and `plugins/GhostFrames/audit.log`
- Localized messages with `en` and `ru` language files
- Persistent settings stored directly in each frame via `PersistentDataContainer`

## Commands

- `/ghostframes reload`
- `/ghostframes info`
- `/ghostframes scan <chunk|radius|world> [radius/world]`
- `/ghostframes applyall <chunk|radius|world> <mode> [radius/world]`
- `/ghostframes setmode <mode>`
- `/ghostframes inspect`
- `/ghostframes reset`

## Permissions

- `ghostframes.use` - Use the GUI and click-through frames
- `ghostframes.admin` - Use admin commands and bulk actions
- `ghostframes.bypass` - Bypass protected frame restrictions

## Configuration

- `language` - Selects the language file, for example `en` or `ru`
- `gui.enabled` - Enables the frame settings GUI
- `click-through.enabled` - Enables click-through globally
- `item-protection.enabled` - Enables protected displayed items by default
- `audit-log.enabled` - Enables console and file audit logs
- `mass-edit.max-radius` - Maximum allowed radius for bulk edits
- `containers.whitelist` - Optional allowed container materials
- `containers.blacklist` - Blocked container materials

## Typical Workflow

1. Place an item frame on a chest, barrel, or other container.
2. Shift + Right Click the frame to open the Ghost Frames GUI.
3. Switch the frame to `GHOST`, `LOCKED`, or `PROTECTED`.
4. Fine-tune click-through, item protection, and fixed rotation.
5. Use admin commands to inspect, reset, or mass-apply settings when needed.

## Development

Build the plugin with:

```bash
.\gradlew.bat build
```

The built jar will be available in `build/libs`.

## Compatibility

- Minecraft: `1.18` to `1.21.x`
- Cores: Bukkit, Spigot, Paper, Folia
- Java: `17`

## Notes

- Ghost Frames already performs safe interaction checks before opening attached inventories.
- The repository includes soft-depend declarations for common protection plugins.
- Deep native integrations with external protection APIs can be added later if you decide to target specific server stacks.

## License

This project is distributed under the [MIT License](LICENSE).
