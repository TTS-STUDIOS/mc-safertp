# Changelog

All notable changes to SafeRTP are documented in this file. The format is based
on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
follows semantic versioning.

## [1.5.0] — 2026-10-11

### Added
- **Destination menu.** `/rtp` without a world opens a menu with one button per enabled world the
  player may use (overworld, Nether, End...), showing its distance, cost and wait. `/rtp <world>`
  still goes straight there. Rows, title (MiniMessage, resource-pack glyphs and `<lang_or>` allowed),
  button slots, icons and optional `item_model`s (`menu.models`, 1.21.4+) are in `menu:` of config.yml; `menu.enabled: false` keeps the old `/rtp`.
  A title that draws its own background leaves the glass frame out (tts-sdk 0.13.5).

## [1.4.3] — 2026-10-10

### Fixed
- **World names in the arrival message.** "Teleported to world (x, y, z)" showed the internal world id; `world-names.<id>` in the lang file gives each world a display name (the Spanish file names the three vanilla worlds). Without an entry the id is shown, as before.
- "Players only" from the console came from the code; it is now `players-only` in the lang file.
- Spanish: "Teleportando en 5s..." is now "Teletransportando en 5 s." and the cooldown lines use "5 s".
- No more "Could not save worlds.yml ... already exists" warning at startup: the file is only written when it is missing.

## [1.4.2] — 2026-10-10

### Fixed
- **Biome names follow the player's language.** The biome discovery title and chat line showed the English id ("Cherry Grove") on every server. The Spanish lang file now names every vanilla biome (`biome-names.<id>`, editable), and a biome missing from the list shows in each player's own game language in chat.
- TTS-SDK 0.13.2: `chat-prefix` in config.yml replaces the branded chat prefix, and the shared buttons read `sdk.*` from the lang file.

## [1.4.1] — 2026-10-09

### Added
- **`default-world` fallback.** A plain `/rtp` typed in a world where RTP is disabled (a spawn or lobby world) now teleports into the world named by `default-world` in `config.yml`. Empty by default, so nothing changes for existing setups.

## [1.4.0] — 2026-10-08

### Added
- **Runs on Folia.** Every task now goes through the TTS-Studio scheduler: on Paper and Spigot nothing changes, and on Folia (`folia-supported: true`) each piece of work runs on the region that owns what it touches. Tested on Folia 1.21.11.

## [1.3.1] — 2026-10-05

### Fixed
- **A random teleport in the Nether lands inside it.** Every teleport in a Nether world ended on top of the bedrock roof, because the highest block of the column is the roof itself. It now picks the highest safe spot under the ceiling.

## [1.3.0] — 2026-09-27

### Added
- **Runs on Paper and Spigot, from 1.17 to 26.x.** It used to need Paper 1.21 and Java 21.

### Changed
- Arrival and discovery sounds take a sound key (`entity.enderman.teleport`) or the old
  constant name, and particles take their name from any version: a config written for 1.21
  keeps working on 1.17 and on 26.x.

## [1.2.6] — 2026-09-20

### Changed
- **Menu titles no longer repeat the plugin name.** `◈ Plugin › Section` is now
  `◈ Section`: measured with the game's own font, the name alone ate more than half
  of the 160 px a chest title has, so any real section came out cut off with an
  ellipsis. The diamond keeps the plugin's colour, and anything still too wide is
  trimmed to fit.

---

## [1.2.8] — 2026-09-26

### Fixed
- **Players without operator can use `/rtp` again.** Every `/rtp` checks
  `safertp.world.<world>`, but those nodes were never declared, and a node the server
  doesn't know is operators-only. On a fresh install nobody but operators could teleport.
  Each world's node is now registered as open to everyone the first time it's checked;
  set it to `false` in your permissions plugin to close a world.

---

## [1.2.7] — 2026-09-21

### Fixed
- **Numbers no longer follow the server's system language.** `String.format` was used
  without a `Locale`, so the very same jar printed `$12.50` on an English machine and
  `$12,50` on a Spanish or French one, and the thousands separator flipped with it. Every
  player-facing number is written with `Locale.ROOT` now, and a test keeps it that way.

---

## [1.2.5] - 2026-09-17

### Fixed

- The background location cache no longer piles up searches. It used to start a
  new search every tick while the cache was not full, without counting the ones
  still running, so on a busy or freshly generated world the server kept
  generating terrain until it ran out of memory. Searches in progress now count
  towards `cache.size-per-world`.
- A world where no safe spot can be found (a void or fully ocean world, for
  example) is retried every 5 minutes instead of every tick.
- A player can only have one `/rtp` search running at a time.
- The fallback language when `language` is missing from `config.yml` is now
  English.

## [1.2.2] - 2026-05-18

### Fixed

- Resolved a stall on the post-boot location-cache pre-warm so the background
  refill no longer hangs on startup.

## [1.2.1] - 2026-05-14

### Fixed

- Corrected the bStats plugin ID to the real value (`31364`).

## [1.2.0] - 2026-05-14

### Added

- **Arrival effects** — on a successful RTP, the player gets configurable
  landing invulnerability (no-damage ticks), a particle burst and a sound.
  All three are toggleable in `config.yml` under `arrival:`.
- **Pre-generated location cache** — safe locations are pre-computed in the
  background (1 per tick per world) so `/rtp` lands instantly after the warmup.
  The deque refills automatically when it drops below `cache.refill-threshold`.
  Configurable via the `cache:` section of `config.yml`.
- **Biome discovery** — the first time a player lands in a new biome, a title,
  chat message and sound fire. Discoveries persist across restarts in
  `discoveries.yml`. Configurable via the `discovery:` section of `config.yml`.
- **WorldGuard integration** — RTP search now skips WorldGuard-protected
  regions when WorldGuard is installed (soft dependency, auto-detected).
- **Bilingual language files** — bundled `lang/es.yml` and `lang/en.yml`,
  selectable via `language:` in `config.yml`, with fallback to English for
  missing keys.

### Changed

- Warmup countdown now renders in the action bar instead of as a title.

### Fixed

- `SafetyChecker` now respects the world border when validating candidate
  locations.

## [1.1.0] - 2026-05-14

### Added

- **`/rtp back`** — Undo your last random teleport and return to the location
  you were standing on before the RTP fired. The pre-teleport location is held
  in memory only (no database hit) and expires after a configurable TTL.
- **New permissions**
  - `safertp.back` (default `true`) — grants access to `/rtp back`.
  - `safertp.back.nocooldown` (default `op`) — bypasses the back cooldown.
- **New config section** (`config.yml`):
  ```yaml
  back:
    enabled: true
    ttl-seconds: 300
    cooldown-seconds: 60
  ```
- **New messages keys** (`messages.yml`): `back-success`, `back-no-location`,
  `back-on-cooldown`, `back-no-permission`.
- Console boot banner now shows a `Back` hook badge when the feature is
  enabled.
- TTS-Studio SDK integration polished — chat prefix and ConsoleBanner reused
  across the suite.

### Changed

- `CooldownManager` now supports multiple independent cooldown buckets per
  player (`/rtp` and `/rtp back` cooldown independently). The legacy
  single-bucket API is preserved for backwards compatibility.
- `config.yml` bumped to `config-version: 2`. Existing configs continue to
  work — the `back:` section is added on first load with defaults.

### Internal

- New `BackLocationStore` (in-memory, TTL-bounded, thread-safe via
  `ConcurrentHashMap`) with injectable clock for deterministic tests.
- Periodic `purgeExpired()` sweep scheduled every 60 s to keep the map small.

## [1.0.0] - 2026-04-29

### Added

- Initial public release on SpigotMC.
- Annular distribution finder (min/max radius per world) to avoid spawn-point
  clustering.
- Per-world configuration in `worlds.yml`: enable flag, radius range, biome
  whitelist/blacklist, max attempts, Vault cost.
- Warmup countdown (title-based, cancels on move/damage) with `WarmupManager`
  and `WarmupListener`.
- Permission-resolved cooldowns via `safertp.cooldown.<n>` tiers (lower wins).
- Vault economy integration for paid teleports.
- PlaceholderAPI expansion exposing `%safertp_cooldown%` and
  `%safertp_can_use%`.
- Admin commands `/rtp other <player>` and `/rtp reload`.
- bStats metrics (plugin ID 31364).
