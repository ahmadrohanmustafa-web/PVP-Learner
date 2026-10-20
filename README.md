# PvP Learner

Minecraft Java **1.21.11**, Fabric, Java 21.

PvP Learner is a lightweight PvP training companion. It does **not** copy or bundle another mod's code. When optional **PvP BOT** is installed, PvP Learner sends that mod's documented commands to create and control a real combat opponent.

## Required for bot fights

- Minecraft Java 1.21.11
- Fabric Loader and Fabric API
- **PvP BOT** for Fabric: version **0.0.15** lists Minecraft 1.21.11 compatibility: https://modrinth.com/mod/pvp-bot-fabric
- PvP BOT's required **HeroBot** dependency. Install dependencies listed on the PvP BOT version page too.

PvP BOT remains optional. Without it, the player kit and training HUD can load, but there is no external combat opponent.

## Commands (use /pvp)

- `/pvp kit sword`
- `/pvp kit axe`
- `/pvp kit crystal`
- `/pvp kit uhc`
- `/pvp kit pot`
- `/pvp kit mace`
- `/pvp summon` — request the companion again.
- `/pvp dismiss` — remove the companion.
- `/pvp start` and `/pvp stop` — resume/pause training and the opponent's attack.
- `/pvp coach on` or `/pvp coach off` — show/hide the coach HUD.
- `/pvp bot spawn [name]`
- `/pvp bot remove <name>`
- `/pvp bot list`
- `/pvp bot attack <bot> <target>`
- `/pvp bot stop <bot>`
- `/pvp bot settings <setting> <value>` — for example, `/pvp bot settings combat true`.

PvP Learner exposes its bot controls under **`/pvp`**. Internally it forwards these actions to PvP BOT's documented `/pvpbot` commands, because PvP BOT owns the actual bots and does not expose a public API in version 0.0.15.

## Coaching behavior

- The compact coach panel is centered near the top of the screen, slightly below the top edge.
- It shows selected kit, training state, confirmed hits, swings, mistakes, and the latest tip.
- A short **green** crosshair flash indicates a detected hit; a **red** flash indicates a swing where no damage was detected.
- While training, a missed swing triggers a chat explanation. The companion is ordered to stop attacking for about three seconds, then attack resumes so the player can practise the correction.
- It currently detects common errors: swinging without an entity under the crosshair, swinging too early, or failing to register damage despite targeting an entity. This is basic local feedback, not a complete analysis of every PvP mistake; hit feedback can be affected by server latency and unusual PvP mechanics.

## Permission note

Kit creation, item giving, and bot management are sent as normal Minecraft commands. They require a world/server where you have permission to run those commands (for singleplayer, cheats/commands must be enabled). PvP Learner cannot bypass server permissions. If the server rejects a command, read the server chat for the reason.

## Build on GitHub

1. Open the **Actions** tab: https://github.com/ahmadrohanmustafa-web/PVP-Learner/actions
2. Select the newest **Build PvP Learner** run and wait for it to finish.
3. If successful, download the `pvp-learner-1.21.11` artifact, extract it, and put the PvP Learner JAR in `.minecraft/mods`.
4. Install PvP BOT and its dependencies separately. PvP Learner does not bundle them.

## Build locally

Install Java 21 and Gradle 8.14, then run `gradle build`. The output should be in `build/libs/`.

A successful build verifies compilation, not the in-game behavior of the external PvP BOT commands. Test in a world with command permissions.
