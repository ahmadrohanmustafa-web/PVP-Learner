package dev.pvplearner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PvpLearnerClient implements ClientModInitializer {
    private static boolean coachEnabled = true;
    private static boolean training = false;
    private static String selectedKit = "sword";
    private static String lastTip = "Choose a kit with .pvp kit sword";
    private static int ticks = 0;

    @Override
    public void onInitializeClient() {
        // Register the custom coach renderer; without this Minecraft crashes when the entity is visible.
        EntityRendererRegistry.register(PvpLearner.PVP_COACH, PvpCoachRenderer::new);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("pvp")
                .executes(ctx -> { say("PvP Learner: /pvp kit <sword|axe|crystal|uhc|pot|mace>, /pvp start, /pvp stop, /pvp coach <on|off>"); return 1; })
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("kit")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> {
                            String kit = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name").toLowerCase();
                            if (!java.util.Set.of("sword", "axe", "crystal", "uhc", "pot", "mace").contains(kit)) { say("Unknown kit. Try sword, axe, crystal, uhc, pot, or mace."); return 0; }
                            selectedKit = kit;
                            applyPlayerKit(kit);
                            lastTip = "Kit applied: " + kit + ". Your inventory was cleared first.";
                            say(lastTip);
                            if (isPvpBotLoaded()) {
                                setupPvpBotCompanion(kit);
                            } else {
                                say("Optional fighter not detected. Install PvP BOT + its required HeroBot dependency to train against a real PvP bot.");
                            }
                            return 1;
                        })))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("start").executes(ctx -> { training = true; lastTip = "Training started. Keep your crosshair on your opponent and watch your spacing."; say(lastTip); return 1; }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("stop").executes(ctx -> { training = false; lastTip = "Training stopped. Use /pvp to choose another exercise."; say(lastTip); return 1; }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("coach")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("on").executes(ctx -> { coachEnabled = true; say("PvP coach HUD enabled."); return 1; }))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("off").executes(ctx -> { coachEnabled = false; say("PvP coach HUD hidden."); return 1; })))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("summon").executes(ctx -> {
                    if (!isPvpBotLoaded()) { say("PvP BOT is not loaded. Install PvP BOT and its required HeroBot dependency first."); return 0; }
                    setupPvpBotCompanion(selectedKit);
                    return 1;
                }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("dismiss").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.getNetworkHandler() == null) { say("You must be in a world first."); return 0; }
                    if (isPvpBotLoaded()) {
                        client.getNetworkHandler().sendChatCommand("pvpbot remove PvPLearnerBot");
                        say("Sent PvP BOT the command to remove PvPLearnerBot.");
                    } else {
                        client.getNetworkHandler().sendChatCommand("kill @e[type=pvp_learner:pvp_coach,distance=..32,limit=1]");
                        say("Sent the legacy coach removal command.");
                    }
                    return 1;
                }))
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            ticks++;
            if (training && coachEnabled && ticks % 20 == 0) {
                // Deliberately conservative: only give general practice prompts until real combat tracking is implemented.
                if (client.player.getAttackCooldownProgress(0.0F) < 0.8F) lastTip = "Tip: wait for your attack cooldown before swinging.";
                else if (client.player.isOnGround()) lastTip = "Tip: practise strafing while keeping your crosshair on the target.";
            }
        });

        HudElementRegistry.addLast(Identifier.of("pvp_learner", "coach_hud"), (drawContext, tickCounter) -> renderCoach(drawContext));
    }

    // Client commands are sent to the integrated/dedicated server, so the world must allow commands.
    private static void applyPlayerKit(String kit) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        client.getNetworkHandler().sendChatCommand("clear @s");

        String[] items;
        switch (kit) {
            case "axe" -> items = new String[] {
                "minecraft:diamond_axe 1", "minecraft:shield 1", "minecraft:iron_helmet 1",
                "minecraft:iron_chestplate 1", "minecraft:iron_leggings 1", "minecraft:iron_boots 1",
                "minecraft:golden_apple 8", "minecraft:ender_pearl 8"
            };
            case "crystal" -> items = new String[] {
                "minecraft:netherite_sword 1", "minecraft:obsidian 64", "minecraft:end_crystal 64",
                "minecraft:totem_of_undying 1", "minecraft:diamond_helmet 1", "minecraft:diamond_chestplate 1",
                "minecraft:diamond_leggings 1", "minecraft:diamond_boots 1"
            };
            case "uhc" -> items = new String[] {
                "minecraft:diamond_sword 1", "minecraft:bow 1", "minecraft:arrow 32",
                "minecraft:water_bucket 1", "minecraft:lava_bucket 1", "minecraft:golden_apple 16",
                "minecraft:iron_helmet 1", "minecraft:iron_chestplate 1", "minecraft:iron_leggings 1",
                "minecraft:iron_boots 1"
            };
            case "pot" -> items = new String[] {
                "minecraft:diamond_sword 1", "minecraft:splash_potion[minecraft:potion_contents={potion:\"minecraft:strong_healing\"}] 16",
                "minecraft:ender_pearl 16", "minecraft:diamond_helmet 1", "minecraft:diamond_chestplate 1",
                "minecraft:diamond_leggings 1", "minecraft:diamond_boots 1"
            };
            case "mace" -> items = new String[] {
                "minecraft:mace 1", "minecraft:wind_charge 32", "minecraft:ender_pearl 16",
                "minecraft:diamond_helmet 1", "minecraft:diamond_chestplate 1",
                "minecraft:diamond_leggings 1", "minecraft:diamond_boots 1"
            };
            default -> items = new String[] {
                "minecraft:diamond_sword 1", "minecraft:shield 1", "minecraft:iron_helmet 1",
                "minecraft:iron_chestplate 1", "minecraft:iron_leggings 1", "minecraft:iron_boots 1",
                "minecraft:golden_apple 8", "minecraft:ender_pearl 8"
            };
        }
        for (String item : items) client.getNetworkHandler().sendChatCommand("give @s " + item);
    }

    private static boolean isPvpBotLoaded() {
        return FabricLoader.getInstance().isModLoaded("pvp_bot");
    }

    /**
     * Uses only the documented PvP BOT chat commands. PvP BOT remains optional;
     * this mod does not bundle, link against, or copy its source code.
     */
    private static void setupPvpBotCompanion(String kit) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            say("Join a world before starting a practice bot.");
            return;
        }
        if (!isPvpBotLoaded()) {
            say("PvP BOT is not loaded. Install PvP BOT and its required HeroBot dependency first.");
            return;
        }

        String savedKit = "pvl_" + kit;
        String playerName = client.player.getName().getString();
        var network = client.getNetworkHandler();

        // Save the exact items just given to the player as a PvP BOT kit.
        // Deleting first allows changing a kit and rebuilding it with the same name.
        network.sendChatCommand("pvpbot kit delete-kit " + savedKit);
        network.sendChatCommand("pvpbot kit create-kit " + savedKit);

        // Use one predictable name so the documented remove/equip/attack commands
        // can address this training opponent without needing to parse server output.
        network.sendChatCommand("pvpbot remove PvPLearnerBot");
        network.sendChatCommand("pvpbot spawn PvPLearnerBot");
        network.sendChatCommand("pvpbot kit give-kit PvPLearnerBot " + savedKit);
        network.sendChatCommand("pvpbot settings combat true");
        network.sendChatCommand("pvpbot settings target-players true");
        network.sendChatCommand("pvpbot settings auto-target true");
        network.sendChatCommand("pvpbot bot-management attack PvPLearnerBot " + playerName);

        lastTip = "PvP BOT companion requested: " + kit + " kit";
        say("Sent PvP BOT commands to save kit '" + savedKit + "', spawn PvPLearnerBot, equip it, and attack you.");
        say("This requires cheats/command permission. Check chat for any rejected command; PvP BOT is a separate mod.");
    }

    private static void renderCoach(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !coachEnabled) return;
        int x = 8, y = 36;
        context.fill(x - 4, y - 5, x + 174, y + (training ? 61 : 43), 0xB8171923);
        context.drawTextWithShadow(client.textRenderer, Text.literal("PVP LEARNER"), x, y, 0xFF7DD3FC);
        context.drawTextWithShadow(client.textRenderer, Text.literal("Kit: " + selectedKit + (training ? "  |  TRAINING" : "  |  READY")), x, y + 13, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, Text.literal("Coach: " + (training ? "ON" : "WAITING")), x, y + 26, 0xFFB8BBC8);
        context.drawTextWithShadow(client.textRenderer, Text.literal(lastTip), x, y + 39, 0xFFFFE7A3);
    }

    private static void say(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(Text.literal("[PvP Learner] " + message), false);
    }
}
