> 🌐 [English](../Configuration.md) · **Español**

# ⚙️ Configuración

SafeRTP divide su configuración en **tres** archivos dentro de `plugins/SafeRTP/`. Tras editar cualquiera de ellos, aplica los cambios con `/rtp reload` — no se necesita reiniciar.

| Archivo | Propósito |
|---------|-----------|
| [`config.yml`](#-configyml) | Configuración global del plugin (actualmente solo versionado — reservado para uso futuro) |
| [`worlds.yml`](#-worldsyml) | Radios por mundo, filtros de bioma, coste, más enfriamiento y preparación global |
| [`messages.yml`](#-messagesyml) | Todas las cadenas dirigidas al jugador (formato MiniMessage) |

---

## 📄 config.yml

El archivo de configuración raíz. Por ahora solo almacena la versión del esquema — la configuración global vive en `worlds.yml` por el momento, pero las nuevas opciones de nivel superior irán aquí.

```yaml
# SafeRTP Configuration
# World-specific settings are in worlds.yml
config-version: 1
```

| Clave | Por Defecto | Descripción |
|-------|-------------|-------------|
| `config-version` | `1` | Versión del esquema. No modificar — usado por migraciones futuras |

> 💡 **Consejo:** No te alarmes por lo corto que es este archivo. El enfriamiento y la preparación viven en `worlds.yml` porque están estrechamente acoplados al mapa por mundo que hay debajo.

---

## 🧭 Menú de destinos (`menu:`, 1.5.0)

`/rtp` sin mundo abre un menú con un botón por cada mundo activado (en `worlds.yml`) que el jugador
puede usar, con su distancia, su coste y su espera. `/rtp <mundo>` sigue yendo directo.

```yaml
menu:
  enabled: true          # false = /rtp funciona como antes de la 1.5.0
  rows: 3
  title: ""              # vacío = menu-title del archivo de idioma; admite MiniMessage, glifos del pack y <lang_or>
  slots:                 # ranura del botón de cada mundo (0 = arriba a la izquierda); los no listados van a la fila central
    world: 11
    world_nether: 13
    world_the_end: 15
  icons:
    world: GRASS_BLOCK
    world_nether: NETHERRACK
    world_the_end: END_STONE
```

Un título que pinta su propio fondo (glifo del pack, `<lang_or>`, `<font>`) quita el marco de cristal
(tts-sdk 0.13.5).

## 🌍 worlds.yml

El corazón de SafeRTP. Define qué mundos tienen RTP habilitado y cómo se elige la ubicación aleatoria.

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

# Enfriamiento por defecto en segundos. Anulable por jugador mediante permisos safertp.cooldown.<n>
cooldown-default: 300

# Duración de la preparación en segundos antes de teletransportar
warmup-seconds: 5
```

### 🗺️ Bloque por Mundo

| Clave | Por Defecto | Descripción |
|-------|-------------|-------------|
| `enabled` | `true` | Deshabilitar para evitar `/rtp` en este mundo (devuelve el mensaje `rtp-world-disabled`) |
| `min-radius` | `1000` | Radio interior del anillo, en bloques, desde `(center-x, center-z)` |
| `max-radius` | `10000` | Radio exterior del anillo |
| `center-x` | `0` | Coordenada X del centro de la zona RTP |
| `center-z` | `0` | Coordenada Z del centro de la zona RTP |
| `biome-whitelist` | `[]` | Si no está vacío, **solo** los biomas listados aquí se consideran seguros |
| `biome-blacklist` | *(ver lista de océanos)* | Biomas que siempre se consideran inseguros — los candidatos en ellos se rechazan |
| `max-attempts` | `50` | Cuántos puntos aleatorios intentar antes de rendirse y mostrar `rtp-no-safe-location` |
| `cost` | `0` | Coste de economía Vault por RTP exitoso. `0` = gratis. Se cobra solo en éxito |

> 📝 **Nota:** El punto aleatorio se extrae de un **anillo uniforme** entre `min-radius` y `max-radius` — los puntos *no* están sesgados hacia el centro. La matemática (`sqrt(min² + r·(max² − min²))`) garantiza muestreo de igual área.

> ⚠️ **Advertencia:** Los nombres de bioma deben coincidir exactamente con el enum `Biome` de Bukkit (mayúsculas, guiones bajos). Los nombres desconocidos se registran como advertencias al cargar y se omiten — **no** abortan el inicio.

### ⏱️ Claves Globales (Final del Archivo)

| Clave | Por Defecto | Descripción |
|-------|-------------|-------------|
| `cooldown-default` | `300` (5 min) | Enfriamiento de respaldo aplicado tras un RTP exitoso. Anulable por rango mediante permisos `safertp.cooldown.<seconds>` |
| `warmup-seconds` | `5` | Cuenta atrás de Título antes del teletransporte real. Se cancela al moverse o recibir daño |

> 💡 **Consejo:** Establecer `warmup-seconds: 0` omite la cuenta atrás — la búsqueda comienza de inmediato. Útil para mundos de staff, peligroso en servidores PvP.

---

## 🛡️ Comprobaciones de Seguridad Explicadas

Cada ubicación candidata es validada por `SafetyChecker.isSafe()` antes de ser aceptada. **Todas** las comprobaciones deben pasar:

| # | Comprobación | Qué Significa |
|---|--------------|---------------|
| 1 | Límites Y | `minHeight + 1 ≤ y ≤ maxHeight − 2` — nunca el suelo del vacío ni el límite de construcción |
| 2 | Pies transitables | El bloque en Y es `AIR`, `CAVE_AIR` o `VOID_AIR` |
| 3 | Cabeza transitable | El bloque en Y+1 también es transitable — sin sofocamiento |
| 4 | Suelo sólido | El bloque en Y−1 es sólido (no agua, no aire, no borde de losa) |
| 5 | Suelo no peligroso | El suelo **no** es `LAVA`, `FIRE`, `MAGMA_BLOCK`, `CAMPFIRE`, `SOUL_FIRE`, `CACTUS`, `SWEET_BERRY_BUSH`, `WITHER_ROSE` o `POWDER_SNOW` |
| 6 | Lista blanca de bioma | Si `biome-whitelist` no está vacía, el bioma del candidato debe estar en ella |
| 7 | Lista negra de bioma | El bioma del candidato **no** debe estar en `biome-blacklist` |

La coordenada Y se elige usando `HeightMap.MOTION_BLOCKING_NO_LEAVES` — se evita aterrizar sobre árboles.

Si se agotan los `max-attempts` sin encontrar un lugar seguro, se envía al jugador el mensaje `rtp-no-safe-location` y **no** se aplica ningún enfriamiento ni coste.

---

## 💬 messages.yml

Todas las cadenas dirigidas al jugador, en formato **MiniMessage**. SafeRTP viene con valores por defecto en español — copia cualquier línea y tradúcela según necesites.

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

### 🏷️ Placeholders Disponibles en Mensajes

| Token | Disponible En | Sustituido Por |
|-------|---------------|----------------|
| `<seconds>` | `rtp-warmup-title`, `rtp-cooldown` | Segundos restantes (entero) |
| `<world>` | `rtp-success`, `rtp-other-success` | Nombre del mundo de destino |
| `<x>`, `<y>`, `<z>` | `rtp-success` | Coordenadas finales del bloque |
| `<amount>` | `rtp-cost`, `rtp-not-enough-money` | Coste de Vault, formateado a 2 decimales |
| `<player>` | `rtp-player-not-found`, `rtp-other-success` | Nombre del jugador objetivo |

> 📝 **Nota:** El prefijo de chat de TTS-Studio `◈ SafeRTP: ` se **añade automáticamente** por el SDK — no lo incluyas en las cadenas de `messages.yml` o obtendrás un prefijo doble.

### 🎨 Cheatsheet de MiniMessage

```yaml
# Colores
rtp-success: "<green>OK</green>"
rtp-success: "<#1ABC9C>Teal de marca</#1ABC9C>"

# Degradados
rtp-success: "<gradient:green:aqua>¡Teletransportado!</gradient>"

# Negrita + cursiva + clickable
rtp-success: "<bold><click:run_command:'/spawn'>Click here</click></bold>"
```

Referencia completa: https://docs.advntr.dev/minimessage/

---

## 🔄 Comportamiento del Reload

`/rtp reload` hace lo siguiente, en orden:

1. Relee `config.yml` (comprobación de versión del esquema)
2. Recarga `messages.yml` desde disco
3. Recarga `worlds.yml`, reconstruyendo todo el `WorldConfigRegistry`
4. Registra `Loaded N world configs.` en la consola

Las preparaciones y enfriamientos activos se **conservan** entre recargas — solo los nuevos comandos `/rtp` verán la nueva configuración.

---

🏠 [Inicio](Home) · 📦 [Instalación](Installation) · 🎮 [Comandos y Permisos](Commands-and-Permissions) · 📊 [PlaceholderAPI](PlaceholderAPI)
