> 🌐 [English](../Commands-and-Permissions.md) · **Español**

# 🎮 Comandos y Permisos

---

## 💬 Comandos

Toda la funcionalidad de SafeRTP se expone a través del comando base `/rtp` (alias: `/randomtp`, `/wild`).

| Comando | Descripción | Permiso Requerido |
|---------|-------------|-------------------|
| `/rtp` | Abre el menú de destinos (un botón por mundo; con `menu.enabled: false`, directo a un sitio seguro de tu mundo) | `safertp.use` |
| `/rtp <mundo>` | Teletransportarte a un mundo específico | `safertp.use` + `safertp.world.<nombre>` |
| `/rtp other <jugador>` | Teletransportar a otro jugador (omite su enfriamiento) | `safertp.admin` |
| `/rtp reload` | Recarga en caliente `config.yml`, `worlds.yml` y `messages.yml` | `safertp.admin` |

### Detalles de los Comandos

#### `/rtp`
Inicia una solicitud de teletransporte aleatorio en tu mundo **actual**. El flujo:

1. Se validan el mundo, el enfriamiento y el coste.
2. Comienza una cuenta atrás de preparación (5 segundos por defecto) — un Título muestra los segundos restantes.
3. Moverse o recibir daño durante la preparación **cancela** el teletransporte.
4. SafeRTP elige un punto aleatorio en el anillo configurado, carga el chunk de forma asíncrona, ejecuta las [comprobaciones de seguridad](Configuration#-comprobaciones-de-seguridad-explicadas) y lo reintenta hasta `max-attempts` veces.
5. Si tiene éxito, se te teletransporta, se te cobra (si `cost > 0`) y se te pone en enfriamiento.

```
> /rtp
◈ SafeRTP: Buscando localización segura...
◈ SafeRTP: ¡Teletransportado a world (3412, 72, -8721)!
◈ SafeRTP: Se cobraron 100.00 del saldo para el RTP.
```

#### `/rtp <mundo>`
Igual que `/rtp` pero apunta a un mundo específico. Requiere el permiso por mundo `safertp.world.<nombremundo>` (o `safertp.admin`).

```
> /rtp world_nether
◈ SafeRTP: El RTP no está habilitado en este mundo.
```

#### `/rtp other <jugador>`
Comando de administrador — envía a otro jugador en línea a un RTP en **su** mundo actual.

- Omite el enfriamiento del objetivo.
- Sigue ejecutando el proceso de seguridad y preparación.
- El jugador debe estar en línea y coincidir exactamente (`Bukkit.getPlayerExact`).

```
> /rtp other Steve
◈ SafeRTP: Teletransportaste a Steve a world.
```

#### `/rtp reload`
Relee los tres archivos de configuración sin reiniciar. Los nuevos valores de radio, enfriamiento, coste y bioma entran en vigor en la **siguiente** llamada a `/rtp` — las preparaciones en curso no se interrumpen.

```
> /rtp reload
◈ SafeRTP: SafeRTP recargado correctamente.
```

---

## 🔑 Permisos

### Permisos de Jugador

| Permiso | Descripción | Por Defecto |
|---------|-------------|-------------|
| `safertp.use` | Usar `/rtp` en los mundos a los que el jugador tiene acceso | `true` (todos los jugadores) |
| `safertp.world.<nombre>` | Permitir RTP en el mundo con ese nombre (p. ej. `safertp.world.world`) | `true` *(verificado junto con `safertp.use`)* |

### Permisos de Administrador

| Permiso | Descripción | Por Defecto |
|---------|-------------|-------------|
| `safertp.admin` | Usar `/rtp other <jugador>`, `/rtp reload` y cualquier mundo independientemente del permiso por mundo | `op` |

### Permisos de Omisión

| Permiso | Descripción | Por Defecto |
|---------|-------------|-------------|
| `safertp.bypass.cooldown` | Nunca se pone en enfriamiento tras un RTP exitoso | `op` |
| `safertp.bypass.cost` | El coste de Vault nunca se cobra | `op` |

### Niveles de Enfriamiento por Jugador

SafeRTP elige el permiso `safertp.cooldown.<seconds>` que coincida con el **menor** valor. Otorga un enfriamiento más corto a rangos más altos:

| Permiso | Enfriamiento | Rango Típico |
|---------|-------------|--------------|
| `safertp.cooldown.300` | 300s (5 min) | *(coincide con el defecto — explícito)* |
| `safertp.cooldown.120` | 120s (2 min) | VIP |
| `safertp.cooldown.60` | 60s (1 min) | Premium |
| `safertp.cooldown.10` | 10s | Staff |

> 📝 **Nota:** Si un jugador tiene múltiples permisos `safertp.cooldown.<n>`, gana el **menor** valor. El enfriamiento por defecto (`cooldown-default` en `worlds.yml`) es el fallback cuando ningún permiso coincide.

> 💡 **Consejo:** `safertp.bypass.cooldown` cortocircuita completamente el sistema de niveles — no se realiza ninguna comprobación de `safertp.cooldown.<n>`.

---

## 🛠️ Ejemplos con LuckPerms

```bash
# Grupo por defecto — RTP básico solo en el mundo principal
/lp group default permission set safertp.use true
/lp group default permission set safertp.world.world true

# VIP — enfriamiento de 2 minutos
/lp group vip permission set safertp.cooldown.120 true

# Premium — enfriamiento de 1 minuto
/lp group premium permission set safertp.cooldown.60 true

# Staff — omisión de enfriamiento y coste, comandos de administrador
/lp group staff permission set safertp.admin true
/lp group staff permission set safertp.bypass.cooldown true
/lp group staff permission set safertp.bypass.cost true

# Abrir el Nether para miembros de pago
/lp group premium permission set safertp.world.world_nether true
```

---

## 📋 Tabla Resumen de Permisos

| Permiso | Jugador | VIP | Premium | Staff | OP |
|---------|---------|-----|---------|-------|----|
| `safertp.use` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `safertp.world.world` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `safertp.world.world_nether` | ❌ | ❌ | ✅ | ✅ | ✅ |
| `safertp.cooldown.120` | ❌ | ✅ | ✅ | ❌ | ❌ |
| `safertp.cooldown.60` | ❌ | ❌ | ✅ | ❌ | ❌ |
| `safertp.bypass.cooldown` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `safertp.bypass.cost` | ❌ | ❌ | ❌ | ✅ | ✅ |
| `safertp.admin` | ❌ | ❌ | ❌ | ✅ | ✅ |

---

🏠 [Inicio](Home) · 📦 [Instalación](Installation) · ⚙️ [Configuración](Configuration) · 📊 [PlaceholderAPI](PlaceholderAPI)
