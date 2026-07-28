# LootTablePeaker

A server-side Fabric dev tool for Minecraft 1.21.10 that stops loot containers from being opened, so you can inspect their loot tables without ever resolving them.

![Loot Table Peeker Screenshot](https://pub-24a4e0e7ea8544a5b6f73c3a23512589.r2.dev/images/c689a5812b4748249468b955a1aeac84.png)

## Modes

| Mode | Behaviour |
| --- | --- |
| `off` | Containers open normally. |
| `title` | Cancels the interaction and shows the loot table id as a title. |
| `preview` | Cancels the interaction and opens a rerollable preview of the loot the container would generate. |

In `preview` mode, right-clicking a loot container opens a read-only chest GUI laid out to match the real container's slot count, filled with a rolled sample of its loot table. The bottom row holds an info book (table id, item and slot counts, seed, roll number) and a **Reroll** button that rolls the table again. Nothing is written back to the block entity, so the container keeps its unresolved loot table however many times you preview it — and no items can be taken out of the preview.

The first roll uses the container's own loot table seed when it has one, so it shows exactly what you would have gotten. Rerolls use fresh random seeds, each shown in the info book.

## Commands
- `/lootpeek` - shows the current mode
- `/lootpeek off` - disables peeking server-wide
- `/lootpeek title` - enables title mode server-wide
- `/lootpeek preview` - enables preview mode server-wide

`/lootpeek on` still works as an alias for `title`.

*(Requires OP level 2)*

## Config

Stored server-wide in `config/loot_table_peeker.json` as `{"mode": "preview"}`. Config files from before modes existed (`{"enabled": true}`) are migrated automatically — `true` becomes `title`.
