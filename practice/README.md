# PracticeMC 2.0 — Paper 1.21.11

Plugin de practice para **Paper 1.21.11** (Java 21), con ranked real, divisiones pastel, torneos, editor de kits, hologramas y un HUD pensado para no gastar TPS.

Servidor: `play.baltiamc.net`

## Instalación

1. Paper **1.21.11** y Java **21**.
2. Copia `PracticeMC-1.0.0.jar` (el nombre del archivo sigue siendo ese; la versión interna es **2.0.0**) a `plugins/`.
3. Reinicia. En el primer arranque se generan una arena de duelo, una FFA y 17 kits.
4. Ponte en el lobby y usa `/practice setspawn`.
5. Opcional: `/practice holo` mirando donde quieras el ranking flotante.

Si actualizas desde la 1.0 y quieres el scoreboard nuevo, borra `plugins/PracticeMC/messages.yml` antes de reiniciar (las stats no se tocan). Las claves nuevas se mezclan solas si dejas el archivo viejo.

## Qué hay de nuevo en la 2.0

- **ELO real**: el cambio depende del rival (factor K 32), no de un +25 fijo. Se puede volver al modo plano con `elo.sistema-real: false`.
- **Divisiones pastel**: Hierro, Bronce, Plata, Oro, Platino, Diamante, Esmeralda, Amatista y Baltia. Salen en la TAB, el nametag, el chat y el resumen.
- **Cola ranked y normal**: click izquierdo = ranked, shift = normal, click derecho = retar.
- **Revanche** clicable al terminar un 1v1 (`/duel revanche`).
- **Torneos** de eliminación directa (4, 8 o 16) sin ELO, con premio en monedas.
- **Editor de kits** por jugador (`/kit editor <kit>` o soltar el kit en el menú).
- **Historial** de las últimas 15 partidas.
- **Monedas, cosméticos y 3 desafíos diarios**.
- **Amigos** con aviso al conectarse.
- **Ajustes** por jugador: scoreboard, retos, invitaciones, espectadores y sonidos.
- **Holograma** de top ELO en el lobby (TextDisplay, se actualiza cada 30 s).
- **BossBar** de la partida y combo en la actionbar.
- **Rollback** de bloques en kits de construcción (BuildUHC, SkyWars).
- Nametags **por jugador** (antes el sufijo se compartía entre todo un rango).
- Guardado **síncrono** al apagar, para no perder stats.

## Comandos

| Comando | Uso |
|---|---|
| `/duel <jugador> [kit] [rondas]` | Reto normal |
| `/duel queue <kit> [normal]` | Cola ranked (o normal) |
| `/duel aceptar` / `rechazar` / `revanche` | Responder o revancha |
| `/torneo crear <kit> [4\|8\|16]` | Abrir torneo |
| `/torneo unirse` / `empezar` / `salir` / `info` / `cancelar` | Gestionar torneo |
| `/amigo agregar\|aceptar\|rechazar\|eliminar\|lista` | Amigos |
| `/historial [jugador]` | Partidas recientes |
| `/opciones` | Ajustes, `/opciones division`, `/opciones desafios` |
| `/kit editor <nombre>` | Reordenar un kit |
| `/practice temporada reset` | Nueva temporada (admin) |
| `/practice monedas <jugador> <cantidad>` | Fijar monedas (admin) |
| `/practice holo` | Colocar el ranking flotante (admin) |

El resto (`/party`, `/ffa`, `/arena`, `/stats`, `/top`, `/spawn`, `/menu`, `/spectate`) sigue igual.

## Divisiones

| División | ELO |
|---|---|
| Hierro | 0–799 |
| Bronce | 800–999 |
| Plata | 1000–1199 |
| Oro | 1200–1399 |
| Platino | 1400–1599 |
| Diamante | 1600–1799 |
| Esmeralda | 1800–2099 |
| Amatista | 2100–2499 |
| Baltia | 2500+ |

## Rendimiento

Siguen valiendo las reglas de la 1.0: una tarea para el scoreboard, otra para la TAB, otra para la lógica. El scoreboard solo reenvía la línea que cambia. Los hologramas no se respawnean en cada refresco: solo se reescribe el texto. El rollback de arenas va en tandas de 250 bloques. Las stats se guardan en async y, al apagar, en el hilo principal para que el archivo quede escrito.

## Notas

- Compilado a Java 21 (class file v65) contra las firmas de `paper-api` 1.21.11.
- Sin dependencias: no hace falta PlaceholderAPI, WorldGuard ni ProtocolLib.
- `api-version: '1.21'` es el valor correcto para Paper 1.21.11.
