# LootTablePeaker

A server-side developer tool that stops loot containers from being opened, so you can inspect their loot tables without ever resolving them.

![Loot Table Peeker Screenshot](https://pub-24a4e0e7ea8544a5b6f73c3a23512589.r2.dev/images/c689a5812b4748249468b955a1aeac84.png)

## Supported versions

Built with [Stonecutter](https://stonecutter.kikugie.dev/) from one shared source tree — each Minecraft version × loader is a Gradle node under `versions/`.

| Minecraft | Fabric | NeoForge |
| --- | --- | --- |
| 1.20.1 | ✅ | — |
| 1.21 / 1.21.1 | ✅ | ✅ |
| 1.21.11 | ✅ | ✅ |
| 26.1 – 26.1.2 | ✅ | ✅ |
| 26.2 | ✅ | ✅ |

1.20.1 is Fabric-only: NeoForge had not split from Forge at that version, so it ships as `net.neoforged:forge` and needs a different Gradle plugin than the modern NeoForge nodes.

The mod is **server-side only** on both loaders — the preview GUI is built from vanilla chest screens, so vanilla clients need nothing installed.

## Modes

| Mode | Behaviour |
| --- | --- |
| `off` | Containers open normally. |
| `title` | Cancels the interaction and shows the loot table id as a title. |
| `preview` | Cancels the interaction and opens a rerollable preview of the loot the container would generate. |

In `preview` mode, right-clicking a loot container opens a read-only chest GUI laid out to match the real container's slot count, filled with a rolled sample of its loot table. The bottom row holds an info book (table id, item and slot counts, seed, roll number) and a **Reroll** button that rolls the table again. Nothing is written back to the block entity, so the container keeps its unresolved loot table however many times you preview it — and no items can be taken out of the preview.

The first roll uses the container's own loot table seed when it has one, so it shows exactly what you would have gotten. Rerolls use fresh random seeds, each shown in the info book.

An empty preview is ambiguous, so when nothing shows up the reason is spelled out with a marker in the middle of the loot area:

| Marker | Meaning |
| --- | --- |
| Structure void — *Rolled nothing* | The table exists and ran, but produced no items. Often legitimate; try rerolling. |
| Barrier — *Loot table not found* | No loot table is registered under that id. Usually a typo or a missing datapack. |
| Barrier — *Roll failed* | The roll threw an exception, e.g. the table needs loot context a chest interaction can't supply. The message is in the tooltip, the stack trace in the server log. |

The info book repeats the reason on its `Rolled:` line.

## Commands
- `/lootpeek` - shows the current mode
- `/lootpeek off` - disables peeking server-wide
- `/lootpeek title` - enables title mode server-wide
- `/lootpeek preview` - enables preview mode server-wide

`/lootpeek on` still works as an alias for `title`.

*(Requires OP level 2)*

## Config

Stored server-wide in `config/loot_table_peeker.json` as `{"mode": "preview"}`. Config files from before modes existed (`{"enabled": true}`) are migrated automatically — `true` becomes `title`.

## Building

Every node builds independently:

```bash
./gradlew :1.21.1-fabric:build
```

Build every version and loader at once, collecting the jars into `build/libs/{version}/`:

```bash
./gradlew buildAndCollect
```

Switching which version the source tree is checked out as (this rewrites the versioned comments in `src/`):

```bash
./gradlew "Set active project to 26.2-neoforge"
```

Mod metadata and per-version dependency versions live in `stonecutter.properties.toml`; the node matrix is declared in `settings.gradle.kts`.
