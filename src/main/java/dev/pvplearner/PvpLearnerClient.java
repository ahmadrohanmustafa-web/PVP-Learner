package dev.pvplearner;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Set;

public final class PvpLearnerClient implements ClientModInitializer {
    private static boolean coachEnabled = true;
    private static boolean training = false;
    private static boolean companionRequested = false;
    private static String selectedKit = "sword";
    private static String lastTip = "Use /pvp kit sword to begin";
    private static int ticks = 0;
    private static boolean wasSwinging = false;
    private static Entity pendingTarget = null;
    private static float pendingHealth = 0.0F;
    private static int pendingHurtTime = 0;
    private static int pendingAttackTicks = -1;
    private static int crosshairFeedbackTicks = 0;
    private static int crosshairFeedbackColor = 0xFFFFFFFF;
    private static int totalSwings = 0;
    private static int confirmedHits = 0;
    private static int mistakes = 0;
    private static int resumeAttackAt = -1;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pvp")
                .executes(ctx -> {
                    say("Commands: /pvp kit <sword|axe|crystal|uhc|pot|mace>, /pvp summon, /pvp dismiss, /pvp start, /pvp stop, /pvp coach <on|off>, /pvp bot <spawn|remove|list|attack|stop|settings>");
                    return 1;
                })
                .then(ClientCommandManager.literal("kit")
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String kit = StringArgumentType.getString(ctx, "name").toLowerCase();
                            if (!Set.of("sword", "axe", "crystal", "uhc", "pot", "mace").contains(kit)) {
                                say("Unknown kit. Try sword, axe, crystal, uhc, pot, or mace.");
                                return 0;
                            }
                            selectedKit = kit;
                            applyPlayerKit(kit);
                            lastTip = "Kit: " + kit + ". Track the target and time your swings.";
                            say(lastTip);
                            if (isPvpBotLoaded()) {
                                setupPvpBotCompanion(kit);
                            } else {
                                say("Fighter not detected. Install PvP BOT + its required HeroBot dependency.");
                            }
                            return 1;
                        })))
                .then(ClientCommandManager.literal("start").executes(ctx -> {
                    training = true;
                    lastTip = "Training resumed. Keep your crosshair on the opponent.";
                    say(lastTip);
                    resumeCompanionAttack();
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(ctx -> {
                    training = false;
                    resumeAttackAt = -1;
                    stopCompanionAttack();
                    lastTip = "Training paused. Use /pvp start when ready.";
                    say(lastTip);
                    return 1;
                }))
                .then(ClientCommandManager.literal("coach")
                    .then(ClientCommandManager.literal("on").executes(ctx -> {
                        coachEnabled = true;
                        say("Coach HUD enabled.");
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("off").executes(ctx -> {
                        coachEnabled = false;
                        say("Coach HUD hidden.");
                        return 1;
                    })))
                .then(ClientCommandManager.literal("summon").executes(ctx -> {
                    if (!isPvpBotLoaded()) {
                        say("PvP BOT is not loaded. Install PvP BOT and its required HeroBot dependency first.");
                        return 0;
                    }
                    setupPvpBotCompanion(selectedKit);
                    return 1;
                }))
                .then(ClientCommandManager.literal("dismiss").executes(ctx -> {
                    if (!isPvpBotLoaded()) {
                        say("PvP BOT is not loaded.");
                        return 0;
                    }
                    sendBotCommand("remove PvPLearnerBot");
                    companionRequested = false;
                    resumeAttackAt = -1;
                    say("Requested removal of PvPLearnerBot.");
                    return 1;
                }))
                .then(ClientCommandManager.literal("bot")
                    .then(ClientCommandManager.literal("spawn")
                        .executes(ctx -> { sendBotCommand("spawn PvPLearnerBot"); companionRequested = true; say("Requested PvPLearnerBot spawn."); return 1; })
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                sendBotCommand("spawn " + name);
                                say("Requested spawn for " + name + ".");
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                sendBotCommand("remove " + name);
                                if (name.equals("PvPLearnerBot")) companionRequested = false;
                                say("Requested removal of " + name + ".");
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("list").executes(ctx -> { sendBotCommand("bot-management list"); say("Requested the active bot list; check server chat."); return 1; }))
                    .then(ClientCommandManager.literal("attack")
                        .then(ClientCommandManager.argument("bot", StringArgumentType.word())
                            .then(ClientCommandManager.argument("target", StringArgumentType.word())
                                .executes(ctx -> {
                                    String bot = StringArgumentType.getString(ctx, "bot");
                                    String target = StringArgumentType.getString(ctx, "target");
                                    sendBotCommand("bot-management attack " + bot + " " + target);
                                    say("Ordered " + bot + " to attack " + target + ".");
                                    return 1;
                                }))))
                    .then(ClientCommandManager.literal("stop")
                        .then(ClientCommandManager.argument("bot", StringArgumentType.word())
                            .executes(ctx -> {
                                String bot = StringArgumentType.getString(ctx, "bot");
                                sendBotCommand("bot-management stop-attack " + bot);
                                say("Ordered " + bot + " to stop attacking.");
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("settings")
                        .then(ClientCommandManager.argument("setting", StringArgumentType.word())
                            .then(ClientCommandManager.argument("value", StringArgumentType.word())
                                .executes(ctx -> {
                                    String setting = StringArgumentType.getString(ctx, "setting");
                                    String value = StringArgumentType.getString(ctx, "value");
                                    sendBotCommand("settings " + setting + " " + value);
                                    say("Sent PvP BOT setting: " + setting + " " + value + ".");
                                    return 1;
                                })))))
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) {
                wasSwinging = false;
                pendingTarget = null;
                pendingAttackTicks = -1;
                return;
            }
            ticks++;
            if (crosshairFeedbackTicks > 0) crosshairFeedbackTicks--;

            // A confirmed hit is detected from the target's health/hurt animation after a swing.
            boolean swinging = client.player.handSwinging;
            if (training && coachEnabled && swinging && !wasSwinging) {
                beginAttackCheck(client);
            }
            wasSwinging = swinging;

            if (pendingAttackTicks >= 0) {
                pendingAttackTicks++;
                if (pendingTarget != null && pendingTarget.isAlive()
                    && (pendingTarget.getHealth() < pendingHealth || pendingTarget.hurtTime > pendingHurtTime)) {
                    confirmedHits++;
                    showCrosshairFeedback(true);
                    lastTip = "HIT CONFIRMED! Good timing. Keep strafing between hits.";
                    pendingAttackTicks = -1;
                    pendingTarget = null;
                } else if (pendingAttackTicks >= 4) {
                    registerMistake(client);
                }
            }

            if (resumeAttackAt > 0 && ticks >= resumeAttackAt) {
                resumeAttackAt = -1;
                if (training && companionRequested) {
                    resumeCompanionAttack();
                    say("Tip delivered. Fight resumed — try that correction now.");
                }
            }

            if (training && coachEnabled && ticks % 40 == 0 && pendingAttackTicks < 0) {
                if (client.player.getAttackCooldownProgress(0.0F) < 0.85F) {
                    lastTip = "Practice: wait until your attack cooldown is nearly full.";
                } else if (client.player.isOnGround()) {
                    lastTip = "Practice: strafe sideways while keeping the target in your crosshair.";
                }
            }
        });

        HudElementRegistry.addLast(Identifier.of("pvp_learner", "coach_hud"), (drawContext, tickCounter) -> renderCoach(drawContext));
    }

    private static void beginAttackCheck(MinecraftClient client) {
        totalSwings++;
        pendingAttackTicks = 0;
        pendingTarget = client.crosshairTarget instanceof EntityHitResult hit ? hit.getEntity() : null;
        if (pendingTarget != null) {
            pendingHealth = pendingTarget.getHealth();
            pendingHurtTime = pendingTarget.hurtTime;
        } else {
            pendingHealth = 0.0F;
            pendingHurtTime = 0;
        }
    }

    private static void registerMistake(MinecraftClient client) {
        Entity target = pendingTarget;
        boolean hadTarget = target != null;
        boolean cooldownLow = client.player != null && client.player.getAttackCooldownProgress(0.0F) < 0.85F;
        pendingAttackTicks = -1;
        pendingTarget = null;
        mistakes++;
        showCrosshairFeedback(false);

        if (!training || !coachEnabled) return;
        if (!hadTarget) {
            lastTip = "MISS: put your crosshair on the opponent BEFORE swinging.";
            say("Mistake: you swung without aiming at the opponent. Aim first, then attack.");
        } else if (cooldownLow) {
            lastTip = "EARLY SWING: wait for your attack cooldown.";
            say("Mistake: you attacked too early. Wait for the attack cooldown to refill, then hit.");
        } else {
            lastTip = "NO DAMAGE: check reach and aim at the opponent's body.";
            say("Mistake: no damage registered. Get within reach and keep your crosshair on the opponent.");
        }

        if (companionRequested && isPvpBotLoaded()) {
            stopCompanionAttack();
            resumeAttackAt = ticks + 60;
            say("Coach pause: the bot will wait for 3 seconds while you read the tip.");
        }
    }

    private static void showCrosshairFeedback(boolean hit) {
        crosshairFeedbackTicks = 10;
        crosshairFeedbackColor = hit ? 0xFF48FF83 : 0xFFFF4D5D;
    }

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
                "minecraft:diamond_sword 1", "minecraft:splash_potion[minecraft:potion_contents={potion:\\"minecraft:strong_healing\\"}] 16",
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
        network.sendChatCommand("pvpbot kit delete-kit " + savedKit);
        network.sendChatCommand("pvpbot kit create-kit " + savedKit);
        network.sendChatCommand("pvpbot remove PvPLearnerBot");
        network.sendChatCommand("pvpbot spawn PvPLearnerBot");
        network.sendChatCommand("pvpbot kit give-kit PvPLearnerBot " + savedKit);
        network.sendChatCommand("pvpbot settings combat true");
        network.sendChatCommand("pvpbot settings target-players true");
        network.sendChatCommand("pvpbot settings auto-target true");
        network.sendChatCommand("pvpbot bot-management attack PvPLearnerBot " + playerName);

        companionRequested = true;
        training = true;
        totalSwings = 0;
        confirmedHits = 0;
        mistakes = 0;
        resumeAttackAt = -1;
        lastTip = "Training live. Hit the bot, and the coach will review missed swings.";
        say("Requested PvPLearnerBot with the " + kit + " kit. Training is ON.");
        say("If a command is rejected, check permissions/server chat. Commands are routed through /pvp.");
    }

    private static void stopCompanionAttack() {
        if (companionRequested && isPvpBotLoaded()) {
            sendBotCommand("bot-management stop-attack PvPLearnerBot");
        }
    }

    private static void resumeCompanionAttack() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (companionRequested && isPvpBotLoaded() && client.player != null) {
            sendBotCommand("bot-management attack PvPLearnerBot " + client.player.getName().getString());
        }
    }

    private static void sendBotCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            say("Join a world first.");
            return;
        }
        if (!isPvpBotLoaded()) {
            say("PvP BOT is not loaded. Install PvP BOT and HeroBot first.");
            return;
        }
        client.getNetworkHandler().sendChatCommand("pvpbot " + command);
    }

    private static void renderCoach(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !coachEnabled) return;

        int width = 330;
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = 42;
        int height = training ? 67 : 54;
        context.fill(x, y, x + width, y + height, 0xC9141824);
        context.fill(x, y, x + width, y + 2, 0xFF57D6FF);
        context.drawTextWithShadow(client.textRenderer, Text.literal("PVP LEARNER  •  " + selectedKit.toUpperCase()), x + 10, y + 7, 0xFF7DD3FC);
        context.drawTextWithShadow(client.textRenderer, Text.literal(training ? "TRAINING: LIVE" : "TRAINING: PAUSED"), x + 10, y + 21, training ? 0xFF75F0A2 : 0xFFFFC56E);
        String stats = "Hits " + confirmedHits + "  |  Swings " + totalSwings + "  |  Mistakes " + mistakes;
        context.drawTextWithShadow(client.textRenderer, Text.literal(stats), x + 10, y + 34, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, Text.literal(lastTip), x + 10, y + 48, 0xFFFFE7A3);

        if (crosshairFeedbackTicks > 0 && !client.options.hudHidden) {
            int cx = context.getScaledWindowWidth() / 2;
            int cy = context.getScaledWindowHeight() / 2;
            int color = crosshairFeedbackColor;
            // Small opaque center plus colored arms temporarily overlays vanilla's crosshair.
            context.fill(cx - 7, cy - 7, cx + 7, cy + 7, 0xA8000000);
            context.fill(cx - 1, cy - 6, cx + 2, cy - 2, color);
            context.fill(cx - 1, cy + 2, cx + 2, cy + 6, color);
            context.fill(cx - 6, cy - 1, cx - 2, cy + 2, color);
            context.fill(cx + 2, cy - 1, cx + 6, cy + 2, color);
        }
    }

    private static void say(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(Text.literal("[PvP Learner] " + message), false);
    }
}
