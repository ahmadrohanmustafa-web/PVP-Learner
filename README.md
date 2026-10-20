# PvP Learner

Minecraft Java **1.21.11**, Fabric, Java 21.

PvP Learner is a lightweight training companion. It does **not** copy or bundle another mod's code. When the optional **PvP BOT** mod is installed, PvP Learner uses its documented chat commands to create a real combat bot and give it the same selected practice kit.

## Required for bot fights

- Minecraft Java 1.21.11
- Fabric Loader and Fabric API
- **PvP BOT** for Fabric, version **0.0.15** is listed for Minecraft 1.21.11: https://modrinth.com/mod/pvp-bot-fabric
- PvP BOT's required **HeroBot** dependency. Install dependencies listed on the PvP BOT version page as well.

PvP BOT is optional: without it, PvP Learner's HUD and its own player-kit commands can still load, but it cannot create the real PvP opponent.

## Commands

- `/pvp kit sword`
- `/pvp kit axe`
- `/pvp kit crystal`
- `/pvp kit uhc`
- `/pvp kit pot`
- `/pvp kit mace`

Selecting a kit attempts to:
1. Clear your inventory and give you the selected kit.
2. Save the resulting inventory as a PvP BOT kit named `pvl_<kit>`.
3. Remove the previous `PvPLearnerBot`, spawn a replacement, and give it that kit.
4. Enable combat/target-player settings and tell the bot to attack you.

Other commands:
- `/pvp summon` — request the companion bot again using the currently selected kit.
- `/pvp dismiss` — request removal of `PvPLearnerBot`.
- `/pvp start` / `/pvp stop` — toggle the PvP Learner HUD's training prompts.
- `/pvp coach on` / `/pvp coach off` — show/hide the HUD.

**Permission note:** kit giving, kit creation, and bot management are performed through Minecraft chat commands. They require a world/server where you have permission to run those commands (for singleplayer, cheats/commands must be enabled). If the server rejects a command, PvP Learner cannot bypass that permission.

## Current limitations

- The companion integration relies on PvP BOT's documented command interface, not a direct API. PvP BOT's README says its APIs were removed in version 0.0.15.
- Kit saving/replacing and command ordering must be tested in-game with PvP BOT and HeroBot installed. A successful compile cannot prove server commands were accepted.
- The HUD still gives basic practice prompts. It does not yet measure hit accuracy, analyze a full fight, save detailed statistics, or generate a genuine performance report.
- Crystal and other specialized kits are given to the bot, but actual combat behavior comes from the installed PvP BOT version and its settings.

## Build on GitHub

1. Open the **Actions** tab: https://github.com/ahmadrohanmustafa-web/PVP-Learner/actions
2. Select the latest **Build PvP Learner** run and wait for it to finish.
3. If it succeeds, download the `pvp-learner-1.21.11` artifact, extract the ZIP, and place the PvP Learner JAR in `.minecraft/mods`.
4. Install PvP BOT and its required dependencies separately. PvP Learner does not bundle them.

## Build locally

Install Java 21 and Gradle 8.14, then run `gradle build`. The output should be in `build/libs/`.

Do not treat a successful build as proof that the bot fought correctly. In-game testing with the companion mod is still required.
