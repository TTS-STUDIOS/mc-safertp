> 🌐 **English** · [Español](es/Configuration.md)

# ⚙️ Configuration

SafeRTP splits its configuration across **three** files in `plugins/SafeRTP/`. After editing any of them, apply changes with `/rtp reload` — no restart required.

| File | Purpose |
|------|---------|
| [`config.yml`](#-configyml) | Global plugin settings (currently versioning only — reserved for future use) |
| [`worlds.yml`](#-worldsyml) | Per-world radii, biome filters, cost, plus global cooldown & warmup |
| [`messages.yml`](#-messagesyml) | All player-facing strings (MiniMessage format) |

---

## 📄 config.yml

The root config file. Today it carries only a schema version — global settings live in `worlds.yml` for now, but new top-level options will land here.

```yaml
# SafeRTP Configuration
# World-specific settings are in worlds.yml
config-version: 1
```

| Key | Default | Description |
|-----|---------|-------------|
| `config-version` | `1` | Schema version. Do not modify — used by future migrations |

> 💡 **Tip:** Don't be alarmed by how short this file is. Cooldown and warmup live in `worlds.yml` because they're tightly coupled to the per-world map below.

---

## 🧭 Destination menu (`menu:`, 1.5.0)

`/rtp` without a world opens a menu with one button per enabled world (from `worlds.yml`) the player
may use, showing its distance, cost and wait. `/rtp <world>` still goes straight there.

```yaml
menu:
  enabled: true          # false = /rtp works as before 1.5.0
  rows: 3
  title: ""              # empty = menu-title from the lang file; MiniMessage, pack glyphs and <lang_or> allowed
  slots:                 # button slot per world (0 = top left); unlisted worlds take a free middle slot
    world: 11
    world_nether: 13
    world_the_end: 15
  icons:
    world: GRASS_BLOCK
    world_nether: NETHERRACK
    world_the_end: END_STONE
  models: {}             # optional item_model per world (1.21.4+), e.g. world: "mypack:menu/rtp_overworld"
  notes: {}              # extra lore lines per world, e.g. world_the_end: "<red>Dangerous: bring gear."
```

A title that draws its own background (a resource-pack glyph, `<lang_or>`, `<font>`) leaves the glass
frame out (tts-sdk 0.13.5).

## 🌍 worlds.yml

The heart of SafeRTP. Defines which worlds are RTP-enabled and how the random location is chosen.

```yaml
# SafeRTP — world configuration
worlds:
  world:
    enabled: true
    min-radius: 1000
    max-radius: 10000
    center-x: 0
    center-z: 0
    biome-whitelist: []
    biome-blacklist:
      - "DEEP_OCEAN"
      - "OCEAN"
      - "WARM_OCEAN"
      - "LUKEWARM_OCEAN"
      - "COLD_OCEAN"
      - "FROZEN_OCEAN"
      - "DEEP_LUKEWARM_OCEAN"
      - "DEEP_COLD_OCEAN"
      - "DEEP_FROZEN_OCEAN"
    max-attempts: 50
    cost: 0
  world_nether:
    enabled: false
  world_the_end:
    enabled: false

# Default cooldown in seconds. Overridable per-player via safertp.cooldown.<n> permission
cooldown-default: 300

# Warmup duration in seconds before teleporting
warmup-seconds: 5
```

### 🗺️ Per-World Block

| Key | Default | Description |
|-----|---------|-------------|
| `enabled` | `true` | Disable to prevent `/rtp` in this world (returns the `rtp-world-disabled` message) |
| `min-radius` | `1000` | Inner radius of the annulus, in blocks, from `(center-x, center-z)` |
| `max-radius` | `10000` | Outer radius of the annulus |
| `center-x` | `0` | X coordinate of the RTP zone center |
| `center-z` | `0` | Z coordinate of the RTP zone center |
| `biome-whitelist` | `[]` | If non-empty, **only** biomes listed here are considered safe |
| `biome-blacklist` | *(see ocean list)* | Biomes that always count as unsafe — candidates in them are rejected |
| `max-attempts` | `50` | How many random points to try before giving up and showing `rtp-no-safe-location` |
| `cost` | `0` | Vault economy cost per successful RTP. `0` = free. Charged only on success |

> 📝 **Note:** The random point is drawn from a **uniform annulus** between `min-radius` and `max-radius` — points are *not* biased toward the center. The math (`sqrt(min² + r·(max² − min²))`) guarantees equal-area sampling.

> ⚠️ **Warning:** Biome names must match the Bukkit `Biome` enum **exactly** (uppercase, underscores). Unknown names are logged as warnings on load and skipped — they do **not** abort startup.

### ⏱️ Global Keys (Bottom of File)

| Key | Default | Description |
|-----|---------|-------------|
| `cooldown-default` | `300` (5 min) | Fallback cooldown applied after a successful RTP. Overridable per-rank via `safertp.cooldown.<seconds>` permissions |
| `warmup-seconds` | `5` | Title countdown before the actual teleport. Cancels on movement or damage |

> 💡 **Tip:** Setting `warmup-seconds: 0` skips the countdown — the search starts immediately. Useful for staff worlds, dangerous on PvP servers.

---

## 🛡️ Safety Checks Explained

Each candidate location is validated by `SafetyChecker.isSafe()` before being accepted. **All** checks must pass:

| # | Check | What It Means |
|---|-------|---------------|
| 1 | Y bounds | `minHeight + 1 ≤ y ≤ maxHeight − 2` — never the void floor or the build limit |
| 2 | Feet passable | Block at Y is `AIR`, `CAVE_AIR`, or `VOID_AIR` |
| 3 | Head passable | Block at Y+1 is also passable — no suffocation |
| 4 | Solid floor | Block at Y−1 is solid (not water, not air, not a slab edge) |
| 5 | Floor not dangerous | Floor is **not** `LAVA`, `FIRE`, `MAGMA_BLOCK`, `CAMPFIRE`, `SOUL_FIRE`, `CACTUS`, `SWEET_BERRY_BUSH`, `WITHER_ROSE`, or `POWDER_SNOW` |
| 6 | Biome whitelist | If `biome-whitelist` is non-empty, the candidate's biome must be in it |
| 7 | Biome blacklist | Candidate's biome must **not** be in `biome-blacklist` |

The Y is picked using `HeightMap.MOTION_BLOCKING_NO_LEAVES` — landing on top of trees is avoided.

If `max-attempts` is exhausted without finding a safe spot, the player is messaged `rtp-no-safe-location` and **no** cooldown or cost is applied.

---

## 💬 messages.yml

All player-facing strings, in **MiniMessage** format. SafeRTP ships with Spanish defaults — copy any line and translate as needed.

```yaml
# SafeRTP — player-facing messages (MiniMessage format)
rtp-warmup-title: "<yellow><seconds>"
rtp-warmup-subtitle: "<gray>No te muevas"
rtp-searching: "<gray>Buscando localización segura..."
rtp-success: "<green>¡Teletransportado a <world> (<x>, <y>, <z>)!"
rtp-cancelled-move: "<red>Teletransporte cancelado al moverte."
rtp-cancelled-damage: "<red>Teletransporte cancelado al recibir daño."
rtp-cooldown: "<red>Debes esperar <seconds>s antes de usar /rtp de nuevo."
rtp-no-permission: "<red>No tienes permiso para usar /rtp."
rtp-world-disabled: "<red>El RTP no está habilitado en este mundo."
rtp-world-no-permission: "<red>No tienes permiso para hacer RTP en ese mundo."
rtp-no-safe-location: "<red>No se encontró una ubicación segura. Inténtalo de nuevo."
rtp-cost: "<yellow>Se cobraron <amount> del saldo para el RTP."
rtp-not-enough-money: "<red>Necesitas <amount> para usar /rtp."
rtp-player-not-found: "<red>Jugador '<player>' no encontrado."
rtp-other-success: "<green>Teletransportaste a <player> a <world>."
reload-success: "<green>SafeRTP recargado correctamente."
```

### 🏷️ Placeholders Available in Messages

| Token | Available In | Replaced With |
|-------|--------------|---------------|
| `<seconds>` | `rtp-warmup-title`, `rtp-cooldown` | Remaining seconds (integer) |
| `<world>` | `rtp-success`, `rtp-other-success` | Destination world name |
| `<x>`, `<y>`, `<z>` | `rtp-success` | Final block coordinates |
| `<amount>` | `rtp-cost`, `rtp-not-enough-money` | Vault cost, formatted to 2 decimals |
| `<player>` | `rtp-player-not-found`, `rtp-other-success` | Target player name |

> 📝 **Note:** The TTS-Studio chat prefix `◈ SafeRTP: ` is **added automatically** by the SDK — do not include it in `messages.yml` strings, you'll get a double prefix.

### 🎨 MiniMessage Cheatsheet

```yaml
# Colors
rtp-success: "<green>OK</green>"
rtp-success: "<#1ABC9C>Brand teal</#1ABC9C>"

# Gradients
rtp-success: "<gradient:green:aqua>¡Teletransportado!</gradient>"

# Bold + italic + clickable
rtp-success: "<bold><click:run_command:'/spawn'>Click here</click></bold>"
```

Full reference: https://docs.advntr.dev/minimessage/

---

## 🔄 Reload Behaviour

`/rtp reload` does the following, in order:

1. Re-reads `config.yml` (schema version check)
2. Reloads `messages.yml` from disk
3. Reloads `worlds.yml`, rebuilding the entire `WorldConfigRegistry`
4. Logs `Loaded N world configs.` to the console

Active warmups and cooldowns are **preserved** across reloads — only newly-issued `/rtp` commands see the new config.

---

🏠 [Home](Home) · 📦 [Installation](Installation) · 🎮 [Commands & Permissions](Commands-and-Permissions) · 📊 [PlaceholderAPI](PlaceholderAPI)
