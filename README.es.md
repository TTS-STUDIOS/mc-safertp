# 🌍 SafeRTP

```
  ███████╗ █████╗ ███████╗███████╗██████╗ ████████╗██████╗
  ██╔════╝██╔══██╗██╔════╝██╔════╝██╔══██╗╚══██╔══╝██╔══██╗
  ███████╗███████║█████╗  █████╗  ██████╔╝   ██║   ██████╔╝
  ╚════██║██╔══██║██╔══╝  ██╔══╝  ██╔══██╗   ██║   ██╔═══╝
  ███████║██║  ██║██║     ███████╗██║  ██║   ██║   ██║
  ╚══════╝╚═╝  ╚═╝╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝
```

> **Teletransporte aleatorio asíncrono y seguro para Paper y Spigot, de 1.17 a 26.x** — la distribución anular evita la acumulación cerca del spawn, los filtros de bioma y material mantienen a los jugadores fuera de la lava y los océanos, y un solo comando **`/rtp back`** deshace el salto cuando la naturaleza no era lo esperado.

> 🌍 Presentado por **[TTS-Studio](https://github.com/TTS-STUDIOS)** — parte de la suite unificada de plugins TTS-Studio.

> 🌐 [English](README.md) · **Español**

---

## ✨ Funcionalidades

| Funcionalidad | Descripción |
|---------|-------------|
| ↩️ **`/rtp back`** *(NUEVO en v1.1.0)* | Deshace el último RTP y regresa donde estabas — la ubicación previa al teletransporte se guarda en memoria y expira tras un TTL configurable |
| ✨ **Efectos de llegada** *(NUEVO en v1.2.0)* | Invulnerabilidad al aterrizar (segundos configurables), explosión de partículas y sonido tras un RTP exitoso — todo activable/desactivable en `config.yml` |
| ⚡ **Caché de ubicaciones pre-generadas** *(NUEVO en v1.2.0)* | Las ubicaciones seguras se pre-calculan en segundo plano (1 por tick por mundo) para que `/rtp` aterrice al instante tras el warmup; se rellena automáticamente cuando el deque baja de un umbral |
| 🗺️ **Descubrimiento de biomas** *(NUEVO en v1.2.0)* | La primera vez que un jugador aterriza en un bioma nuevo, salta una notificación de título + chat + sonido; los descubrimientos persisten entre reinicios en `discoveries.yml` |
| 🎯 **Distribución anular** | Radio mínimo/máximo configurable por mundo; el muestreo de área uniforme evita la acumulación cerca del spawn |
| ⚡ **Búsqueda de ubicación asíncrona** | La carga de chunks y las comprobaciones de seguridad se ejecutan fuera del hilo principal — sin lag en el servidor con radios grandes |
| 🌿 **Filtros de bioma** | Whitelist o blacklist por mundo (`OCEAN`, `DEEP_OCEAN`, `RIVER`, etc.) |
| ⏱️ **Cuenta atrás de warmup** | Temporizador basado en título que se cancela al moverse o recibir daño |
| 🏷️ **Cooldowns por tier** | Nodos de permiso `safertp.cooldown.<n>` — el más bajo gana (`safertp.cooldown.0` = sin cooldown) |
| 💰 **Soporte de coste Vault** | Coste en dinero opcional por mundo, cobrado en el teletransporte exitoso |
| 📊 **PlaceholderAPI** | `%safertp_cooldown%`, `%safertp_can_use%` para scoreboards, TAB, chat |
| 🛠️ **Comandos de admin** | `/rtp other <player>`, `/rtp reload` |
| 🎨 **Estilo TTS-Studio** | Prefijo de chat unificado de la suite y banner de inicio enmarcado en consola |
| 📈 **Métricas bStats** | Plugin ID 31364 |

---

## 🔗 Integraciones

| Plugin | Versión | ¿Obligatorio? |
|--------|---------|--------------|
| Paper | 1.21.4+ | ✅ Obligatorio |
| Java | 21+ | ✅ Obligatorio |
| Vault | cualquiera | Opcional — coste en dinero por RTP |
| PlaceholderAPI | 2.11+ | Opcional |
| WorldGuard | 7+ | Opcional — evita regiones protegidas durante la búsqueda de RTP |

---

## 🚀 Inicio rápido

```bash
# 1. Copia el jar en tu carpeta de plugins
cp safertp-1.2.2.jar plugins/

# 2. Reinicia el servidor
#    Se generan worlds.yml, messages.yml y config.yml de muestra en el primer inicio.

# 3. (Opcional) Ajusta los radios por mundo en plugins/SafeRTP/worlds.yml

# 4. Los jugadores lo usan
/rtp                # teletransporte aleatorio en el mundo actual
/rtp <world>        # mundo específico (requiere safertp.world.<world>)
/rtp back           # deshace el último /rtp (NUEVO en v1.1.0)
```

> 💡 **Consejo:** Ejecuta `/rtp reload` después de cualquier cambio en el YAML — sin reinicio.

---

## ↩️ `/rtp back` — Deshaz tu último teletransporte

Nuevo en **v1.1.0**. Cuando un jugador ejecuta `/rtp`, su ubicación se captura el instante antes de que el teletransporte se active. Tiene una ventana configurable (`back.ttl-seconds`, por defecto 300 s = 5 min) para ejecutar `/rtp back` y regresar exactamente donde estaba.

```yaml
# plugins/SafeRTP/config.yml
back:
  enabled: true            # pon false para desactivar la función completamente
  ttl-seconds: 300         # cuánto tiempo se mantiene válida una ubicación capturada
  cooldown-seconds: 60     # cooldown entre llamadas sucesivas a /rtp back
```

- **Solo en memoria** — sin escrituras en SQLite, sin I/O en el camino caliente.
- **Expira automáticamente** — una limpieza periódica purga las entradas obsoletas cada 60 s.
- **Cooldown independiente** — `/rtp back` tiene su propio bucket; usarlo no consume el cooldown de `/rtp` y viceversa.
- **Gateado por permiso** — otorga `safertp.back.nocooldown` para saltarse el cooldown.

---

## ⚙️ Resumen de configuración

Añade una sección de mundo en `worlds.yml`, ejecuta `/rtp reload` y listo.

```yaml
# plugins/SafeRTP/worlds.yml
cooldown-default: 300
warmup-seconds: 5

worlds:
  world:
    enabled: true
    min-radius: 1000                   # anillo interior (evita el área de spawn)
    max-radius: 10000                  # anillo exterior
    biome-blacklist:
      - OCEAN
      - DEEP_OCEAN
      - RIVER
    max-attempts: 50                   # cuántas ubicaciones candidatas se comprueban
    cost: 0                            # coste Vault (0 = gratis); requiere Vault
  world_nether:
    enabled: true
    min-radius: 200
    max-radius: 2000
    max-attempts: 100
    cost: 100
```

| Archivo | Propósito |
|---------|-----------|
| `config.yml` | Toggle global para `/rtp back`, TTL, cooldown independiente |
| `worlds.yml` | Radio por mundo, filtros de bioma, max-attempts, coste Vault |
| `messages.yml` | Cada cadena visible por el jugador, formateada con MiniMessage y completamente reemplazable |

---

## 🎮 Comandos

| Comando | Descripción | Permiso | Por defecto |
|---------|-------------|---------|-------------|
| `/rtp` | Teletransporta a una ubicación aleatoria segura en tu mundo actual | `safertp.use` | true |
| `/rtp <world>` | Teletransporta en un mundo específico | `safertp.use` + `safertp.world.<world>` | true / false |
| `/rtp back` | Deshace tu último `/rtp` (NUEVO en v1.1.0) | `safertp.back` | true |
| `/rtp other <player>` | Fuerza el RTP de otro jugador | `safertp.admin` | op |
| `/rtp reload` | Recarga `config.yml`, `worlds.yml`, `messages.yml` | `safertp.admin` | op |

Aliases: `/randomtp`, `/wild`.

---

## 🛠️ Permisos

| Permiso | Descripción | Por defecto |
|---------|-------------|-------------|
| `safertp.use` | Ejecutar `/rtp` | `true` |
| `safertp.back` | Ejecutar `/rtp back` (NUEVO en v1.1.0) | `true` |
| `safertp.back.nocooldown` | Saltarse el cooldown de `/rtp back` (NUEVO en v1.1.0) | `op` |
| `safertp.world.<world>` | Permitir RTP en un mundo específico (ponlo a false para cerrar un mundo) | `true` |
| `safertp.bypass.cooldown` | Saltarse el cooldown principal de `/rtp` | `op` |
| `safertp.bypass.cost` | Saltarse el coste en dinero de Vault | `op` |
| `safertp.cooldown.<n>` | Cooldown por tier — establece tu cooldown de `/rtp` en *n* segundos, el más bajo gana | – |
| `safertp.admin` | `/rtp other`, `/rtp reload` | `op` |

**Tiers de cooldown:** otorga `safertp.cooldown.60` para dar a un jugador un cooldown de 60 segundos en `/rtp`. El nodo coincidente más bajo gana; `safertp.cooldown.0` elimina el cooldown completamente.

---

## 📊 PlaceholderAPI

| Placeholder | Devuelve |
|-------------|---------|
| `%safertp_cooldown%` | Segundos hasta el próximo `/rtp` (0 si está listo) |
| `%safertp_can_use%` | `true` / `false` |

PlaceholderAPI se detecta automáticamente al iniciar; los placeholders se desactivan silenciosamente si el plugin no está presente.

---

## 🐛 Reportar bugs

Abre un issue en el repositorio de GitHub con:

- Versión de SafeRTP (`/version SafeRTP`)
- Tipo y versión del servidor (build de Paper, versión de Java)
- Un `worlds.yml` mínimo que reproduzca el problema
- Fragmento del log del servidor — especialmente el stack trace si lo hay

---

## 📜 Licencia

SafeRTP se distribuye bajo la licencia de plugin gratuito de SpigotMC — gratis para usar en cualquier servidor. La reventa y redistribución sin autorización están prohibidas.

---

<sub>SafeRTP es un plugin de TTS-Studio · © TTS-Studio · naturaleza más segura, un clic a la vez.</sub>
