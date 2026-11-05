# Configuration Reference

## worlds.yml

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `cooldown-default` | int | `300` | Default cooldown in seconds |
| `warmup-seconds` | int | `5` | Warmup duration before teleport |
| `worlds.<name>.enabled` | boolean | `true` | Enable RTP in this world |
| `worlds.<name>.min-radius` | int | `1000` | Minimum distance from center |
| `worlds.<name>.max-radius` | int | `10000` | Maximum distance from center |
| `worlds.<name>.first-max-radius` | int | `0` (`3000` in the bundled file) | Maximum distance of a player's FIRST `/rtp` in this world; later trips use `max-radius`. `0` = off |
| `worlds.<name>.center-x` | int | `0` | X coordinate of search center |
| `worlds.<name>.center-z` | int | `0` | Z coordinate of search center |
| `worlds.<name>.biome-whitelist` | list | `[]` | Only allow these biomes (empty = all allowed) |
| `worlds.<name>.biome-blacklist` | list | `[]` | Reject these biomes |
| `worlds.<name>.max-attempts` | int | `50` | Max candidate locations before failing |
| `worlds.<name>.cost` | double | `0` | Economy cost (requires Vault) |

## messages.yml

All messages support MiniMessage format (`<red>`, `<gradient:...>`, etc.).

| Key | Description |
|-----|-------------|
| `rtp-no-permission` | Missing `safertp.use` |
| `rtp-world-disabled` | World not configured or disabled |
| `rtp-world-no-permission` | Missing `safertp.world.<name>` |
| `rtp-cooldown` | On cooldown — placeholder `<seconds>` |
| `rtp-not-enough-money` | Insufficient funds — placeholder `<amount>` |
| `rtp-searching` | Action bar during location search |
| `rtp-success` | Teleport success — placeholders `<world>`, `<x>`, `<y>`, `<z>` |
| `rtp-no-safe-location` | No safe location found after max-attempts |
| `rtp-cost` | Cost deducted notification — placeholder `<amount>` |
| `rtp-warmup-title` | Title during warmup — placeholder `<seconds>` |
| `rtp-warmup-subtitle` | Subtitle during warmup |
| `rtp-cancelled-move` | Warmup cancelled due to movement |
| `rtp-cancelled-damage` | Warmup cancelled due to damage |
| `rtp-player-not-found` | Admin `/rtp other` target not online — placeholder `<player>` |
| `reload-success` | Config reloaded successfully |
