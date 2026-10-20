# PvP Learner — Arsenal Edition

Minecraft Java Edition **1.21.11**, Fabric, Java 21.

PvP Learner is a client-side PvP training companion and kit hotkey helper. It preserves the existing coach HUD and optional PvP BOT integration. It does not bundle third-party mods.

## Features

- **Quick kit switching:** `J` previous kit, `K` next kit; direct keys are listed below. Change them in Options → Controls → PvP Learner.
- **Coach HUD:** training state, hit/swing/mistake counters, live tip, animated top accent and green hit/red miss crosshair feedback.
- **Player list:** shows up to three names from the current multiplayer tab list.
- **PvP BOT integration (optional):** sends documented bot commands when PvP BOT and its required HeroBot dependency are installed.
- **Vanilla kit loadouts:** sword, axe, crystal, bow, minecart, mace, spear, UHC and potion.

## Hotkeys

| Key | Action |
|---|---|
| J / K | Previous / next kit |
| F6 | Sword + shield |
| F7 | Axe |
| F8 | Crystal |
| F9 | Bow |
| F10 | Minecart |
| F11 | Mace |
| F12 | Spear |
| H | UHC |
| P | Potion |

Keys are configurable in **Options → Controls → Key Binds → PvP Learner**.

## Commands

- `/pvp` — help
- `/pvp kit <sword|axe|crystal|bow|minecart|mace|spear|uhc|pot>`
- `/pvp summon`, `/pvp dismiss`
- `/pvp start`, `/pvp stop`
- `/pvp coach on` / `/pvp coach off`
- `/pvp bot spawn [name]`, `/pvp bot remove <name>`, `/pvp bot list`
- `/pvp bot attack <bot> <target>`, `/pvp bot stop <bot>`
- `/pvp bot settings <setting> <value>`

## Multiplayer and permissions

The HUD, keybindings and hit feedback are client-side and can be used while connected to multiplayer servers. **Giving kits uses ordinary Minecraft commands** (`/clear` and `/give`), so it requires command permission; this mod intentionally does not bypass server permissions or modify server rules. On servers that prohibit kit commands, use the hotkeys for local selection/coach features and obtain gear normally. Player names are read from the vanilla player list. Bot practice requires a server/world where PvP BOT commands are allowed.

The client-side mod does not create an authoritative server-side kit system. For fair multiplayer kit queues, install a server-approved kit plugin/mod or have the server owner implement kits.

## Install

1. Install **Minecraft Java 1.21.11** and launch it once.
2. Install **Java 21**.
3. Install Fabric Loader for **1.21.11** and Fabric API for the same Minecraft version.
4. Download the PvP Learner JAR from the GitHub Actions artifact or build it yourself.
5. Put the JAR in `%AppData%\.minecraft\mods` on Windows (or your launcher's instance `mods` folder).
6. Start the Fabric 1.21.11 profile. Check Options → Controls for the PvP Learner keys.

### Optional practice opponent

Install [PvP BOT Fabric](https://modrinth.com/mod/pvp-bot-fabric) version supporting Minecraft 1.21.11 and all dependencies listed by its version page, including HeroBot where required. The third-party mod is not bundled.

## Build locally

Install Java 21 and Gradle 8.14, open a terminal in the repository folder, then run:

```bash
gradle build --no-daemon
```

Output: `build/libs/pvp-learner-0.1.0.jar` and a sources JAR. If you use a wrapper in your local checkout, `./gradlew build` (macOS/Linux) or `gradlew.bat build` (Windows) is also fine.

## CI artifact and size check

Pushes to the feature branch and `main` trigger GitHub Actions. The workflow uploads the compiled mod JAR and source archive, and records file sizes. The compiled JAR's actual size is reported by CI; source code is not padded just to hit a number. If you require a single distributable archive above 200 KB, bundle the compiled JAR plus sources and this README; do not assume the compiled JAR alone exceeds 200 KB.

## Notes / limitations

- The spear kit uses `minecraft:netherite_spear` and depends on the exact item IDs present in the 1.21.11 mappings/game build. If the server rejects this item ID, the rest of the kit can still be used and the item ID must be updated to match the target version.
- A successful Gradle build proves compilation only. Test each kit in a command-enabled singleplayer world and then on a multiplayer server where kit commands are permitted.
- No custom combat damage or server authority is added; vanilla combat mechanics remain in control.
