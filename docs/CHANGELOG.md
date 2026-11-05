# Changelog

## [1.7.0] — 2026-11-05

### Added
- **First trip closer to spawn.** New `first-max-radius` per world in `worlds.yml`: a player's first
  `/rtp` in that world lands within that radius, and every later trip uses `max-radius` as before.
  New players start near spawn instead of at the far edge of the map, where mobs are usually
  hardest. Remembered per player and per world (stored on the player, no database). The player is
  told with the new `rtp-first-trip` line. `0` (or a value at or above `max-radius`) turns it off;
  existing `worlds.yml` files without the key keep working as before. The bundled `worlds.yml`
  ships `first-max-radius: 3000`.

## [1.0.0] - 2026-05-10

### Added
- Initial release
- Per-world RTP configuration (radius, biomes, cost, attempts)
- Annular distribution for even spread of teleport destinations
- Warmup system with title countdown, cancellable on move/damage/quit
- Permission-based cooldown tiers (`safertp.cooldown.<n>`)
- Vault economy integration (cost per teleport)
- PlaceholderAPI expansion (`%safertp_cooldown%`, `%safertp_can_use%`)
- Admin commands: `/rtp reload`, `/rtp other <player>`
- bStats metrics
