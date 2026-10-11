> 🌐 **English** · [Español](es/Commands-and-Permissions.md)

# 🎮 Commands & Permissions

---

## 💬 Commands

All SafeRTP functionality is exposed via the `/rtp` base command (aliases: `/randomtp`, `/wild`).

| Command | Description | Permission Required |
|---------|-------------|---------------------|
| `/rtp` | Open the destination menu (one button per world; `menu.enabled: false` = straight to a random safe spot in your world) | `safertp.use` |
| `/rtp <world>` | Teleport yourself into a specific world | `safertp.use` + `safertp.world.<name>` |
| `/rtp other <player>` | Teleport another player (bypasses their cooldown) | `safertp.admin` |
| `/rtp reload` | Hot-reload `config.yml`, `worlds.yml`, and `messages.yml` | `safertp.admin` |

### Command Details

#### `/rtp`
Starts a random-teleport request in your **current** world. The flow:

1. World, cooldown and cost are validated.
2. A warmup countdown begins (default 5 seconds) — a Title shows the seconds left.
3. Moving or taking damage during the warmup **cancels** the teleport.
4. SafeRTP picks a random point in the configured annulus, async-loads the chunk, runs the [safety checks](Configuration#-safety-checks-explained), and tries again up to `max-attempts` times.
5. On success you're teleported, charged (if `cost > 0`), and put on cooldown.

```
> /rtp
◈ SafeRTP: Buscando localización segura...
◈ SafeRTP: ¡Teletransportado a world (3412, 72, -8721)!
◈ SafeRTP: Se cobraron 100.00 del saldo para el RTP.
```

#### `/rtp <world>`
Same as `/rtp` but targets a specific world. Requires the per-world permission `safertp.world.<worldname>` (or `safertp.admin`).

```
> /rtp world_nether
◈ SafeRTP: El RTP no está habilitado en este mundo.
```

#### `/rtp other <player>`
Admin command — sends another online player on an RTP in **their** current world.

- Bypasses the target's cooldown.
- Still runs the safety / warmup pipeline.
- Player must be online and match exactly (`Bukkit.getPlayerExact`).

```
> /rtp other Steve
◈ SafeRTP: Teletransportaste a Steve a world.
```

#### `/rtp reload`
Re-reads all three config files without restarting. New radius / cooldown / cost / biome values take effect on the **next** `/rtp` call — in-flight warmups are not interrupted.

```
> /rtp reload
◈ SafeRTP: SafeRTP recargado correctamente.
```

---

## 🔑 Permissions

### Player Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `safertp.use` | Use `/rtp` in worlds the player has access to | `true` (all players) |
| `safertp.world.<name>` | Allow RTP in the named world (e.g. `safertp.world.world`) | `true` *(checked alongside `safertp.use`)* |

### Admin Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `safertp.admin` | Use `/rtp other <player>`, `/rtp reload`, and any world regardless of per-world permission | `op` |

### Bypass Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `safertp.bypass.cooldown` | Never put on cooldown after a successful RTP | `op` |
| `safertp.bypass.cost` | Vault cost is never charged | `op` |

### Per-Player Cooldown Tiers

SafeRTP picks the **lowest** matching `safertp.cooldown.<seconds>` permission. Grant a shorter cooldown to higher ranks:

| Permission | Cooldown | Typical Rank |
|------------|----------|--------------|
| `safertp.cooldown.300` | 300s (5 min) | *(matches default — explicit)* |
| `safertp.cooldown.120` | 120s (2 min) | VIP |
| `safertp.cooldown.60` | 60s (1 min) | Premium |
| `safertp.cooldown.10` | 10s | Staff |

> 📝 **Note:** If a player has multiple `safertp.cooldown.<n>` permissions, the **lowest** value wins. The default cooldown (`cooldown-default` in `worlds.yml`) is the fallback when no permission matches.

> 💡 **Tip:** `safertp.bypass.cooldown` short-circuits the tier system entirely — no `safertp.cooldown.<n>` check is performed.

---

## 🛠️ LuckPerms Examples

```bash
# Default group — basic RTP in the overworld only
/lp group default permission set safertp.use true
/lp group default permission set safertp.world.world true

# VIP — 2-minute cooldown
/lp group vip permission set safertp.cooldown.120 true

# Premium — 1-minute cooldown
/lp group premium permission set safertp.cooldown.60 true

# Staff — bypass cooldown and cost, admin commands
/lp group staff permission set safertp.admin true
/lp group staff permission set safertp.bypass.cooldown true
/lp group staff permission set safertp.bypass.cost true

# Open the nether for paying members
/lp group premium permission set safertp.world.world_nether true
```

---

## 📋 Permission Summary Table

| Permission | Player | VIP | Premium | Staff | OP |
|------------|--------|-----|---------|-------|----|
| `safertp.use` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `safertp.world.world` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `safertp.world.world_nether` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `safertp.cooldown.120` | ❌ | ✅ | ✅ | ❌ | ❌ |
| `safertp.cooldown.60` | ❌ | ❌ | ✅ | ❌ | ❌ |
| `safertp.bypass.cooldown` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `safertp.bypass.cost` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `safertp.admin` | ❌ | ❌ | ❌ | ✅ | ✅ |

---

🏠 [Home](Home) · 📦 [Installation](Installation) · ⚙️ [Configuration](Configuration) · 📊 [PlaceholderAPI](PlaceholderAPI)
