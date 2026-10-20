package dev.pvplearner;

import net.fabricmc.api.ClientModInitializer;
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
                            equipNearestCoach(kit);
                            lastTip = "Kit applied: " + kit + ". Your inventory was cleared first.";
                            say(lastTip + " If commands are blocked, enable cheats or use a world where you have permission.");
                            return 1;
                        })))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("start").executes(ctx -> { training = true; lastTip = "Training started. Keep your crosshair on your opponent and watch your spacing."; say(lastTip); return 1; }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("stop").executes(ctx -> { training = false; lastTip = "Training stopped. Use /pvp to choose another exercise."; say(lastTip); return 1; }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("coach")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("on").executes(ctx -> { coachEnabled = true; say("PvP coach HUD enabled."); return 1; }))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("off").executes(ctx -> { coachEnabled = false; say("PvP coach HUD hidden."); return 1; })))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("summon").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.getNetworkHandler() == null) { say("You must be in a world first."); return 0; }
                    client.getNetworkHandler().sendChatCommand("summon pvp_learner:pvp_coach ~ ~ ~3");
                    equipNearestCoach(selectedKit);
                    say("Summoning the PvP Coach with the selected kit. Note: advanced PvP actions such as crystal placement are not implemented yet.");
                    return 1;
                }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("dismiss").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.getNetworkHandler() == null) { say("You must be in a world first."); return 0; }
                    client.getNetworkHandler().sendChatCommand("kill @e[type=pvp_learner:pvp_coach,distance=..32,limit=1]");
                    say("Asked Minecraft to remove the nearest custom PvP Coach within 32 blocks.");
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

    private static void equipNearestCoach(String kit) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;
        String target = "@e[type=pvp_learner:pvp_coach,sort=nearest,limit=1,distance=..32]";
        String mainHand = switch (kit) {
            case "axe" -> "minecraft:diamond_axe";
            case "crystal" -> "minecraft:netherite_sword";
            case "mace" -> "minecraft:mace";
            default -> "minecraft:diamond_sword";
        };
        String helmet = kit.equals("crystal") || kit.equals("pot") || kit.equals("mace") ? "minecraft:diamond_helmet" : "minecraft:iron_helmet";
        String chest = kit.equals("crystal") || kit.equals("pot") || kit.equals("mace") ? "minecraft:diamond_chestplate" : "minecraft:iron_chestplate";
        String legs = kit.equals("crystal") || kit.equals("pot") || kit.equals("mace") ? "minecraft:diamond_leggings" : "minecraft:iron_leggings";
        String boots = kit.equals("crystal") || kit.equals("pot") || kit.equals("mace") ? "minecraft:diamond_boots" : "minecraft:iron_boots";
        String offhand = kit.equals("sword") || kit.equals("axe") ? "minecraft:shield" : "minecraft:totem_of_undying";
        client.getNetworkHandler().sendChatCommand("item replace entity " + target + " weapon.mainhand with " + mainHand);
        client.getNetworkHandler().sendChatCommand("item replace entity " + target + " weapon.offhand with " + offhand);
        client.getNetworkHandler().sendChatCommand("item replace entity " + target + " armor.head with " + helmet);
        client.getNetworkHandler().sendChatCommand("item replace entity " + target + " armor.chest with " + chest);
        client.getNetworkHandler().sendChatCommand("item replace entity " + target + " armor.legs with " + legs);
        client.getNetworkHandler().sendChatCommand("item replace entity " + target + " armor.feet with " + boots);
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
