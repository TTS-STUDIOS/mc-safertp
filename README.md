# 🌍 SafeRTP

```
  ███████╗ █████╗ ███████╗███████╗██████╗ ████████╗██████╗
  ██╔════╝██╔══██╗██╔════╝██╔════╝██╔══██╗╚══██╔══╝██╔══██╗
  ███████╗███████║█████╗  █████╗  ██████╔╝   ██║   ██████╔╝
  ╚════██║██╔══██║██╔══╝  ██╔══╝  ██╔══██╗   ██║   ██╔═══╝
  ███████║██║  ██║██║     ███████╗██║  ██║   ██║   ██║
  ╚══════╝╚═╝  ╚═╝╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝
```

> **Async, safe random teleport for Paper and Spigot, 1.17 to 26.x** — annular distribution avoids spawn clustering, biome and material filters keep players out of lava and oceans, and a one-command **`/rtp back`** undoes the jump when the wilderness looks worse than expected.

> 🌍 Brought to you by **[TTS-Studio](https://github.com/TTS-STUDIOS)** — part of the unified TTS-Studio plugin suite.

> 🌐 **English** · [Español](README.es.md)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| ↩️ **`/rtp back`** *(NEW in v1.1.0)* | Undo the last RTP and return to where you stood — pre-teleport location is cached in memory and expires after a configurable TTL |
| ✨ **Arrival effects** *(NEW in v1.2.0)* | Landing invulnerability (configurable seconds), particle burst and sound on a successful RTP — all toggleable in `config.yml` |
| ⚡ **Pre-generated location cache** *(NEW in v1.2.0)* | Safe locations are pre-computed in the background (1 per tick per world) so `/rtp` lands instantly after warmup; refills automatically when the deque drops below a threshold |
| 🗺️ **Biome discovery** *(NEW in v1.2.0)* | First time a player lands in a new biome, a title + chat notification + sound fires; discoveries persist across restarts in `discoveries.yml` |
| 🎯 **Annular distribution** | Configurable min/max radius per world; uniform-area sampling avoids spawn clustering |
| ⚡ **Async location search** | Chunk loading and safety checks run off the main thread — no server hitch on big radius searches |
| 🌿 **Biome filters** | Per-world whitelist or blacklist (`OCEAN`, `DEEP_OCEAN`, `RIVER`, etc.) |
| ⏱️ **Warmup countdown** | Title-based timer that cancels on move or damage |
| 🏷️ **Tiered cooldowns** | `safertp.cooldown.<n>` permission nodes — lower wins (`safertp.cooldown.0` = no cooldown) |
| 💰 **Vault cost support** | Optional per-world money cost charged on successful teleport |
| 📊 **PlaceholderAPI** | `%safertp_cooldown%`, `%safertp_can_use%` for scoreboards, TAB, chat |
| 🛠️ **Admin commands** | `/rtp other <player>`, `/rtp reload` |
| 🎨 **TTS-Studio house style** | Suite-wide chat prefix and framed boot banner |
| 📈 **bStats metrics** | Plugin ID 31364 |

---

## 🔗 Integrations

| Plugin | Version | Required? |
|--------|---------|-----------|
| Paper | 1.21.4+ | ✅ Required |
| Java | 21+ | ✅ Required |
| Vault | any | Optional — money cost per RTP |
| PlaceholderAPI | 2.11+ | Optional |
| WorldGuard | 7+ | Optional — skips protected regions during RTP search |

---

## 🚀 Quick Start

```bash
# 1. Drop the jar into your plugins folder
cp safertp-1.2.2.jar plugins/

# 2. Restart the server
#    Sample worlds.yml, messages.yml and config.yml are written on first enable.

# 3. (Optional) Tune per-world radii in plugins/SafeRTP/worlds.yml

# 4. Players use it
/rtp                # random teleport in current world
/rtp <world>        # specific world (needs safertp.world.<world>)
/rtp back           # undo the last /rtp (NEW in v1.1.0)
```

> 💡 **Tip:** Run `/rtp reload` after any YAML change — no restart required.

---

## ↩️ `/rtp back` — Undo your last teleport

New in **v1.1.0**. When a player runs `/rtp`, their location is captured the instant before the teleport fires. They have a configurable window (`back.ttl-seconds`, default 300 s = 5 min) to run `/rtp back` and return to exactly where they were.

```yaml
# plugins/SafeRTP/config.yml
back:
  enabled: true            # set false to disable the feature entirely
  ttl-seconds: 300         # how long a captured location stays valid
  cooldown-seconds: 60     # cooldown between successive /rtp back calls
```

- **In-memory only** — no SQLite writes, no I/O on the hot path.
- **Auto-expires** — a periodic sweep purges stale entries every 60 s.
- **Independent cooldown** — `/rtp back` has its own bucket; using it does not burn your `/rtp` cooldown and vice versa.
- **Permission-gated** — grant `safertp.back.nocooldown` to skip the cooldown.

---

## ⚙️ Configuration overview

Drop a world section into `worlds.yml`, hit `/rtp reload`, and you're done.

```yaml
# plugins/SafeRTP/worlds.yml
cooldown-default: 300
warmup-seconds: 5

worlds:
  world:
    enabled: true
    min-radius: 1000                   # inner ring (avoids spawn area)
    max-radius: 10000                  # outer ring
    biome-blacklist:
      - OCEAN
      - DEEP_OCEAN
      - RIVER
    max-attempts: 50                   # how many candidate locations to test
    cost: 0                            # Vault cost (0 = free); requires Vault
  world_nether:
    enabled: true
    min-radius: 200
    max-radius: 2000
    max-attempts: 100
    cost: 100
```

| File | Purpose |
|------|---------|
| `config.yml` | Global toggle for `/rtp back`, TTL, independent cooldown |
| `worlds.yml` | Per-world radius, biome filters, max-attempts, Vault cost |
| `messages.yml` | Every player-facing string, MiniMessage-formatted and fully overridable |

---

## 🎮 Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/rtp` | Teleport to a random safe location in your current world | `safertp.use` | true |
| `/rtp <world>` | Teleport in a specific world | `safertp.use` + `safertp.world.<world>` | true / false |
| `/rtp back` | Undo your last `/rtp` (NEW in v1.1.0) | `safertp.back` | true |
| `/rtp other <player>` | Force RTP another player | `safertp.admin` | op |
| `/rtp reload` | Reload `config.yml`, `worlds.yml`, `messages.yml` | `safertp.admin` | op |

Aliases: `/randomtp`, `/wild`.

---

## 🛠️ Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `safertp.use` | Run `/rtp` | `true` |
| `safertp.back` | Run `/rtp back` (NEW in v1.1.0) | `true` |
| `safertp.back.nocooldown` | Bypass the `/rtp back` cooldown (NEW in v1.1.0) | `op` |
| `safertp.world.<world>` | Allow RTP in a specific world (set it to false to close a world) | `true` |
| `safertp.bypass.cooldown` | Skip the main `/rtp` cooldown | `op` |
| `safertp.bypass.cost` | Skip the Vault money cost | `op` |
| `safertp.cooldown.<n>` | Tiered cooldown — sets your `/rtp` cooldown to *n* seconds, lower wins | – |
| `safertp.admin` | `/rtp other`, `/rtp reload` | `op` |

**Cooldown tiers:** grant `safertp.cooldown.60` to give a player a 60-second `/rtp` cooldown. The lowest matching node wins; `safertp.cooldown.0` removes the cooldown entirely.

---

## 📊 PlaceholderAPI

| Placeholder | Returns |
|-------------|---------|
| `%safertp_cooldown%` | Seconds until next `/rtp` (0 if ready) |
| `%safertp_can_use%` | `true` / `false` |

PlaceholderAPI is auto-detected on enable; placeholders silently no-op when the plugin is absent.

---

## 🐛 Reporting bugs

Open an issue on the GitHub repository with:

- SafeRTP version (`/version SafeRTP`)
- Server type and version (Paper build, Java version)
- A minimal `worlds.yml` that reproduces the issue
- Server log excerpt — especially the stack trace if there is one

---

## 📜 License

Free to download and run on any server, under the **TTS-Studio Free License** (see `LICENSE`). Resale, rebranding and re-uploading elsewhere are not permitted — link to the official listing instead.

---

<sub>SafeRTP is a TTS-Studio plugin · © TTS-Studio · safer wilderness, one click at a time.</sub>
