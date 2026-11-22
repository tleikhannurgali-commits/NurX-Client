# NurX Client v0.3 — Exploration + original PvE/PvP Aura (Fabric 1.21.4)

An original, free Minecraft Java client-side mod. **Source code release, not a verified, working executable JAR.** Its dark-violet UI is independently written, inspired by modern clients; it does not contain or circumvent paid Wild Client code. No FunTime anti-cheat bypass is implemented or claimed.

## Exploration: six functional modules (20 total)

| Module | Default bind | Implemented behavior |
|---|---|---|
| StashFinder | J | Finds groups of **already loaded** nearby chests, barrels and shulker boxes; configurable 16–96-block scan radius and minimum 2–20 containers. Saves the groups as coordinate markers in JSON. |
| DungeonESP | N | Outlines **already loaded** vanilla monster spawners and trial spawners in pink, and vaults in turquoise, using world-space boxes. Range configurable 16–96. |
| LootESP | L | Outlines nearby ground-item entities; common items in blue, selected rare items or named custom loot in gold. Range configurable 8–64. |
| QuickLoot | K | With QuickLoot enabled, **hold Shift** in a standard chest/shulker GUI to quick-move visible stacks into your inventory, spaced 2–12 ticks apart. Stops without shift, does not navigate menus and skips container GUIs whose title doesn't look like a loot chest. |
| ExpeditionHUD | H | Compact counters for loaded nearby storage, spawners, vaults and saved markers; nearest marker coordinates and distance. |
| MarkSpot | M | **Action key:** adds an explicit manual waypoint at your current position; it is intentionally not a persistent toggle. |

The original 14 module names are retained (combat implementations upgraded): KillAura, AimAssist, TriggerBot, TargetHUD, PlayerESP, StorageESP (now chest/barrel/shulker/ender chest/hopper/dropper/dispenser/furnace/brewing stand), ArrayList, Watermark, Crosshair, Keystrokes, AutoSprint, Zoom, LowParticles, Fullbright.

### How waypoint persistence works

When StashFinder identifies at least N connected storage blocks (each within 12 blocks of another), it creates a marker at the cluster's center. Markers store the current **server address plus dimension**; markers within 24 blocks of an existing automatic find are deduplicated. Up to 96 recent markers (manual + detected) are kept in `.minecraft/config/nurx-client.json`. The latest discovery triggers a six-second pop-up. Marks survive restart. Manual marks have no storage count.

**Limitations that matter:** This code sees **only chunks and entities that the server has already sent**. It cannot inspect untransmitted coordinates or unopened container contents. Server-specific hidden/virtual dungeon chest locations are not guaranteed to be detected. A cluster of containers is only a heuristic for a base; natural structures can create false positives. `QuickLoot` only uses ordinary slot clicks that the server can reject; it is not tested on FunTime, and it may still interact with chest-looking custom server menus if you hold Shift. Use with care.

## Combat update: independently implemented Night-style features (v0.3)

This is **NOT Night DLC's own KillAura**. Night DLC advertises an anti-cheat-specific 1.21.4 FunTime R mode, but its original code and its proprietary bypass algorithms were **not extracted or copied**. This project's combat logic was newly written and **is not a proven FunTime bypass**.

KillAura (`R`) now includes independently configurable:

| Setting | Values | Initial value |
|---|---|---|
| Range | 2.0–3.2 blocks | 3.0 |
| FOV | 30–180 degrees | 110 |
| Turn speed | 2–90 degrees per tick | 24 |
| Rotation mode | `Smooth` or `Snap` | Smooth |
| Rotate | ON/OFF | ON |
| Hit angle | 5–50 degrees | 16 |
| Cooldown | 0.60–1.00 of vanilla weapon cooldown | 0.95 |
| Priority | Nearest / Low HP / Angle | Nearest |
| Sticky target | ON/OFF | ON |
| Players | ON/OFF | OFF |
| Hostile mobs | ON/OFF | ON |
| Animals | ON/OFF | OFF |
| Attack while using item | ON/OFF | OFF |

- Aims at a target below the eye line and only attacks when it is visible, in range, roughly aligned, and cooldown-ready. Uses the normal Minecraft `attackEntity` call; no fake packet-side rotations.
- Defaults prioritize hostile mobs for dungeons. Players and animals are **disabled by default**. Villagers, armor stands and tameable non-farm animals are not selected.
- `AimAssist` (`V`) now works with monsters as well as players (while holding attack); `TriggerBot` (`G`) supports configurable target categories and vanilla cooldown.
- TargetHUD now displays mobs and players. Existing key binds, waypoints and the JSON config are retained.
- ClickGUI now supports mouse-wheel scrolling inside category panels, so expanded combat settings remain reachable on smaller game windows.
- These features may violate server rules. No effectiveness claims can be made without running the actual Minecraft mod and testing in a permitted environment.

## Controls

- **Right Shift** opens the dark-violet GUI; ESC closes it.
- **Left click** toggles a module (or performs `MarkSpot`).
- **Right click** expands adjustable sliders.
- **Middle click** on a module, then press a key, to rebind. Backspace removes the bind.
- Five modules categories: Combat, Exploration, Visuals, Movement, Misc. On narrow windows, Left/Right arrows cycle categories.
- All module toggles and slider values persist across relaunches.

## Build the JAR (GitHub Actions)

1. Create an empty GitHub repo, then upload the **contents** of this ZIP to its root, keeping the `.github/workflows/build.yml` directory.
2. Open Actions → `Build Fabric mod` → `Run workflow` (or push the files to trigger a build).
3. If successful, download artifact `NurX-Client-Fabric-1.21.4` and extract `NurX-Client-1.21.4-0.3.0.jar`.
4. Install Fabric Loader for **Minecraft Java 1.21.4** and matching Fabric API. Put the built JAR into `.minecraft/mods`.

For local build, use Java 21 and Gradle 8.14, run `gradle --no-daemon build`. This project intentionally has no precompiled JAR or fetched libraries. **Compilation and runtime behavior have not been verified here** because the environment has no Gradle install or Fabric/Minecraft dependencies. The standalone Java `ClusterFinder` and `AuraMath` algorithms and the `Mod` settings smoke test were compiled and passed on Java 21; see `tests/`. GitHub compilation may reveal API/mapping mistakes to correct.

## Server rules

FunTime's posted mod restrictions include ESP and BaseFinder-style functionality. These features may lead to punishment or ban. This project does not claim to evade anti-cheat, bypass captchas, access restricted game data, or decrypt any server communication.

## Implementation overview

- `Expedition.java`: chunk-limited block-entity scanner, stash markers, world-scoped coordinates, persistence trigger.
- `ClusterFinder.java`: independently testable Java graph grouping; no hidden data.
- `QuickLoot.java`: opt-in, Shift-held, rate-limited standard inventory quick-move.
- `WorldOverlay.java`: 3D outlines including loot entities, storage, spawners, vaults.
- `HudOverlay.java`: status, alerts and existing UI.
- `ConfigStore.java`: JSON settings and waypoint persistence.

- `CombatLogic.java`: independent PvE/PvP target selection, cooldown-aware hit handling and selectable rotations.
- `AuraMath.java`: independently testable rotation/FOV math; see `tests/AuraMathTest.java`.
- `ClickGui.java`: scrollable settings panes and module key binds.

No external telemetry, session-token processing, downloaders, remote control, or automatic update mechanisms are included. Fabric/Minecraft runtime dependencies are resolved at build time.
