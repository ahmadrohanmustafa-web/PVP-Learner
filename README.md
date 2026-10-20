# PvP Learner (prototype)

Minecraft Java **1.21.11**, Fabric, Java 21. This is a starter prototype, not the finished AI opponent.

## Current prototype features
- Client-side `/pvp` commands: `/pvp kit sword`, `/pvp kit axe`, `/pvp kit crystal`, `/pvp kit uhc`, `/pvp kit pot`, `/pvp kit mace`.
- `/pvp start` and `/pvp stop` toggle training mode.
- `/pvp coach on` and `/pvp coach off` show/hide a simple left-side HUD.
- Basic practice prompts based on the player's attack cooldown and movement state.
- GitHub Actions attempts to compile the mod and publishes the built JAR as an artifact if compilation succeeds.

## Important limitations
This initial prototype does **not** spawn a real player-like bot, equip kits, detect hit accuracy, implement crystal combat, save persistent training statistics, or generate a true post-fight report. Kit selection is currently a coaching setting only; these commands do not give items. These are follow-up development tasks, not completed features.

## Build on GitHub
1. Open the **Actions** tab.
2. Select **Build PvP Learner**.
3. If Actions are disabled, enable them in the repository's Actions settings.
4. Open the latest run and wait for it to finish.
5. If it succeeds, download the `pvp-learner-1.21.11` artifact from the run's **Artifacts** section. Extract the ZIP and put the `.jar` in `.minecraft/mods` alongside Fabric API.

## Build locally
Install Java 21 and Gradle 8.14, then run `gradle build`. The output should be in `build/libs/`.

Do not use this prototype as evidence that the real AI bot is already implemented. The GitHub Actions build must succeed before its JAR can be considered build-verified.
