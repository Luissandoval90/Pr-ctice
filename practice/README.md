# PracticeMC — Plugin de Practice para Paper 1.21.11

Plugin de **Practice completo** para **Paper 1.21.11** (Java 21) con diseño **pastel**, TAB y scoreboard
cuidados, y arquitectura optimizada para no consumir TPS.

> Servidor objetivo: `play.baltiamc.net`

---

## 1. Contenido del ZIP

```
PracticeMC-Paper-1.21.11.zip
├── PracticeMC-1.0.0.jar        ← el plugin (colócalo en la carpeta /plugins)
├── README.md                   ← este documento
├── configuracion/
│   ├── config.yml              ← copia de la configuración principal (opcional)
│   └── messages.yml            ← copia de todos los mensajes (opcional)
└── fuente/                     ← código fuente completo (por si quieres modificarlo)
    ├── com/practice/**.java
    └── resources/{plugin.yml, config.yml, messages.yml}
```

Para recompilar la fuente necesitas un proyecto Maven/Gradle con `paper-api 1.21.11-R0.1-SNAPSHOT`
como dependencia (Java 21). El JAR incluido ya está listo para usar, no hace falta compilar nada.

Al arrancar el servidor, el plugin crea automáticamente:

```
plugins/PracticeMC/
├── config.yml                  ← ajustes generales
├── messages.yml                ← textos y diseño (MiniMessage)
├── kits.yml                    ← 15 kits predefinidos (editables)
├── arenas.yml                  ← arenas de duelo y FFA
└── data/stats.yml              ← estadísticas (ELO, K/D, rachas, kits…)
```

---

## 2. Instalación

1. Servidor **Paper 1.21.11** con **Java 21** o superior.
2. Copia `PracticeMC-1.0.0.jar` en la carpeta `plugins/` del servidor.
3. Reinicia el servidor.
4. En el primer arranque el plugin **genera automáticamente una arena de duelo y una arena FFA**
   (plataformas pastel sobre el spawn del mundo principal) para que puedas jugar al instante.
5. Ajusta el spawn real del lobby: `/spawn` y después `/practice setspawn`.
6. (Opcional) Crea tus propias arenas con `/arena crear <nombre>` y el resto de subcomandos.

> Si prefieres no generar arenas automáticas, pon `arenas.generar-por-defecto: false`
> en `config.yml` antes de reiniciar.

---

## 3. Funcionalidades

### Duelos
- **1v1 con retos**: `/duel <jugador> [kit] [rondas]`.
- **2v2 y equipos** con parties: `/party crear`, `/party invitar`, `/party duelo`.
- **Cola/matchmaking por kit**: `/duel queue <kit>` (empareja por ELO ±250 configurable).
- **Rondas**: mejor de 1, 3, 5, 7 o 9 con cuenta atrás, marcador y resumen.
- **Espectadores**: `/spectate <jugador>` o `/duel espectar <jugador>`.
- **Ciclo completo** de partida: cuenta atrás → pelea → muerte → resultado de ronda →
  siguiente ronda → resumen final → vuelta al lobby.
- **Abandonos** detectados (salida del servidor, `/spawn`, etc.) con derrota automática.

### Kits (15 predefinidos y editables en `kits.yml`)
`nodebuff`, `debuff`, `gapple`, `combo`, `sumo`, `boxing`, `archer`, `builduhc`, `soup`,
`iron`, `diamond`, `skywars`, `hcf`, `duo_nodebuff`, `team_sumo`, `team_nodebuff`, `ffa`.

- Reglas por kit: hambre, regeneración natural, daño por caída, romper/colocar bloques y rondas.
- Edición: `/kit crear <nombre>` (copia tu inventario), `/kit borrar`, `/kit tipo`, `/kit rondas`,
  `/kit descripcion`.

### FFA
- Arenas FFA con reaparición automática y respawn aleatorio.
- Rachas de kills con anuncios globales cada 5 kills.
- Kits FFA y estadísticas específicas (kills, muertes, mejor racha).

### Partidas y social
- **Parties** de hasta 16 jugadores con invitaciones, expulsiones, disolución, lista y chat propio (`@mensaje`).
- **Menús interactivos** (brújula del lobby): Duelos, Kits, FFA, Estadísticas, Ranking y Party.
- **Chat con diseño pastel**, hover con estadísticas y click para susurrar.

### Progresión
- **ELO** con recompensas por victoria/derrota, bonus por racha y multiplicador de rondas.
- **Estadísticas**: ELO, elo máximo, kills, muertes, K/D, victorias, derrotas, winrate,
  racha actual, mejor racha, FFA, tiempo jugado y registro por kit (`/stats [jugador]`).
- **Clasificaciones**: `/top [kills|elo|wins|streak|ffakills]` y menú de ranking.

### Lobby y protección
- Spawn con vuelo desactivado, inventario limpio, modo aventura y objetos del menú.
- Protección total en el lobby (romper, colocar, daño, hambre, cubos, explosiones, ítems).
- Anti-void, clima despejado y hora fija.

---

## 4. Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/practice` | Administración (`reload`, `setspawn`, `spawn`, `fly`, `elo`, `stats`, `save`, `info`, `arenas`, `build`, `clearchat`) | `practice.admin` / `practice.staff` |
| `/duel <jugador> [kit] [rondas]` | Retar a un jugador | `practice.play` |
| `/duel queue <kit>` | Entrar en la cola del kit | `practice.play` |
| `/duel aceptar` / `/duel rechazar` | Responder a un reto | `practice.play` |
| `/duel dejar` | Salir de la cola o de la partida | `practice.play` |
| `/duel espectar <jugador>` | Observar una partida | `practice.play` |
| `/party <crear\|invitar\|aceptar\|rechazar\|salir\|expulsar\|disolver\|lista\|chat\|duelo>` | Sistema de parties | `practice.play` |
| `/ffa [arena]` / `/ffa leave` / `/ffa lista` | Combate libre | `practice.play` |
| `/kit <nombre>` / `/kit lista` | Obtener un kit / listar | `practice.play` |
| `/kit crear\|borrar\|tipo\|rondas\|descripcion` | Administrar kits | `practice.admin` |
| `/arena <crear\|pos1\|pos2\|spawn1\|spawn2\|espectador\|tipo\|guardar\|borrar\|lista\|tp\|enable\|disable\|construir>` | Administrar arenas | `practice.admin` |
| `/stats [jugador]` | Ver estadísticas | `practice.play` |
| `/top [categoria]` | Clasificación | `practice.play` |
| `/spawn` | Volver al lobby | `practice.play` |
| `/menu [kits\|stats\|ffa\|top\|party]` | Menú principal | `practice.play` |
| `/spectate [jugador]` | Observar partidas | `practice.staff` |

Alias: `/prac`, `/padmin`, `/d`, `/clan`, `/grupo`, `/pt`, `/freeforall`, `/allvsl`, `/kits`,
`/arenas`, `/mapas`, `/estadisticas`, `/elo`, `/perfil`, `/hub`, `/lobby`, `/leaderboard`,
`/ranking`, `/mejores`, `/menus`, `/espectar`, `/spec`.

---

## 5. Permisos

| Permiso | Descripción | Por defecto |
|---|---|---|
| `practice.play` | Jugar (duelos, kits, FFA, parties) | Todos |
| `practice.admin` | Administración total (arenas, kits, reload, elo) | OP |
| `practice.staff` | Herramientas de staff (stats, arenas, fly, clearchat) | OP |
| `practice.vip` | Rango VIP pastel en TAB, nametag y chat | `false` |
| `practice.fly` | Volar en el lobby | OP |

---

## 6. Diseño pastel (TAB + Scoreboard)

### Paleta oficial
| Color | Hex | Uso |
|---|---|---|
| Rosa | `#FFD1DC` | Acentos, énfasis |
| Azul claro | `#B0E0E6` | Títulos, información |
| Verde menta | `#C1E1C1` | Valores positivos / victorias |
| Lila | `#E6D5F7` | Elementos secundarios |
| Amarillo suave | `#FFF5BA` | ELO, avisos |
| Rojo pastel | `#FFB3BA` | Errores, derrotas |
| Gris claro | `#E8E8F0` | Texto principal |
| Gris suave | `#BFC4CC` | Etiquetas |

Todo el texto usa **MiniMessage** (soporta degradados y hexadecimales), por lo que puedes
editar `messages.yml` y `config.yml` sin recompilar nada.

### TAB (tablist)
- Cabecera y pie con degradado rosa→azul, IP `play.baltiamc.net`, Discord, jugadores online y TPS.
- Nombre de cada jugador: **rango pastel + nombre**, y orden automático por rango y ELO
  (`setPlayerListOrder`, sin reenviar la tabla completa).
- **Nametags** por encima de la cabeza con rango y estado:
  `| kit` en partida, `| FFA x kills`, `| buscando <kit>` en cola o `| <ELO> ELO` en el lobby.
- Colisión de nametags desactivada (evita empujones, mejora el PvP).

### Scoreboard (sidebar)
- Título con degradado: `BALTIA` (rosa→menta).
- **5 diseños automáticos según el contexto**: lobby, cola, partida, FFA y espectador.
- Datos en vivo: ELO, K/D, racha, online, ping, kit, arena, rival, vida del rival, marcador de
  rondas, duración, kills/muertes de FFA, etc.
- Todos los diseños son editables en `messages.yml` → sección `scoreboard:`.

---

## 7. Rendimiento (optimizaciones aplicadas)

- **Tareas únicas** (3 tareas periódicas: scoreboard 20 t, TAB 40 t, lógica 10 t) en lugar de tareas por jugador.
- **Envío diferencial al scoreboard**: solo se reenvía la línea que cambia; los scores y el título
  solo se actualizan si su valor cambia.
- **TAB cacheado**: cabecera y pie se parsean una vez por refresco (o cada 5 s por el TPS) y se
  envían a todos; el orden de la lista solo se actualiza cuando cambia.
- **Guardado asíncrono** de estadísticas (nunca en el hilo principal) y solo cuando hay cambios.
- **Nametags con el scoreboard principal** (0 paquetes por jugador) y colisiones desactivadas.
- **Balanceo de carga de arenas**: se elige la arena menos usada recientemente entre las 3 primeras.
- Sin dependencias externas: no hace falta WorldGuard, PlaceholderAPI ni ProtocolLib.

---

## 8. Primer arranque recomendado

1. `/practice setspawn` en el punto exacto del lobby.
2. Revisa `config.yml` → sección `rendimiento` si quieres afinar intervalos.
3. Revisa `messages.yml` → sección `scoreboard` para personalizar el diseño de la sidebar.
4. Crea una arena de verdad: `/arena crear mi_arena`, ve a la zona, marca
   `/arena pos1 mi_arena`, `/arena pos2 mi_arena`, luego `/arena spawn1`, `/arena spawn2`,
   `/arena espectador` y por último `/arena guardar`.
5. Añade tus kits: prepárate el inventario y `/kit crear <nombre>`.

### Notas técnicas
- Compilado con `javac`/ECJ a **Java 21 (class file v65)** para Paper 1.21.11.
- Toda la API usada (425 referencias) fue verificada contra las fuentes oficiales de
  `paper-api` 1.21.11 y Adventure 5.x.
- Compatible con `/reload confirm` (comando `/practice reload` recarga config, mensajes, kits y arenas).
