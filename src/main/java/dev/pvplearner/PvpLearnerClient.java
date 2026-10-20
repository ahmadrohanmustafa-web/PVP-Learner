package dev.pvplearner;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.lwjgl.glfw.GLFW;

public final class PvpLearnerClient implements ClientModInitializer {
    private static boolean coachEnabled = true;
    private static boolean training = false;
    private static boolean companionRequested = false;
    private static String selectedKit = "sword";
    private static String lastTip = "Use /pvp kit sword to begin";
    private static int ticks = 0;
    private static boolean wasSwinging = false;
    private static LivingEntity pendingTarget = null;
    private static LivingEntity preTickTarget = null;
    private static float preTickHealth = 0.0F;
    private static int preTickHurtTime = 0;
    private static float preTickCooldown = 1.0F;
    private static float pendingCooldown = 1.0F;
    private static float pendingHealth = 0.0F;
    private static int pendingHurtTime = 0;
    private static int pendingAttackTicks = -1;
    private static int crosshairFeedbackTicks = 0;
    private static int crosshairFeedbackColor = 0xFFFFFFFF;
    private static int totalSwings = 0;
    private static int confirmedHits = 0;
    private static int mistakes = 0;
    private static int resumeAttackAt = -1;
    private static final KeyBinding.Category KIT_KEY_CATEGORY = KeyBinding.Category.create(Identifier.of("pvp_learner", "controls"));
    private static final List<String> KIT_ORDER = List.of("sword", "axe", "crystal", "bow", "minecart", "mace", "spear", "uhc", "pot");
    private static final List<KeyBinding> DIRECT_KIT_KEYS = new ArrayList<>();
    private static KeyBinding nextKitKey;
    private static KeyBinding previousKitKey;

    @Override
    public void onInitializeClient() {
        nextKitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pvp_learner.next_kit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KIT_KEY_CATEGORY));
        previousKitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pvp_learner.previous_kit", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, KIT_KEY_CATEGORY));
        registerDirectKitKey("sword", GLFW.GLFW_KEY_F6);
        registerDirectKitKey("axe", GLFW.GLFW_KEY_F7);
        registerDirectKitKey("crystal", GLFW.GLFW_KEY_F8);
        registerDirectKitKey("bow", GLFW.GLFW_KEY_F9);
        registerDirectKitKey("minecart", GLFW.GLFW_KEY_F10);
        registerDirectKitKey("mace", GLFW.GLFW_KEY_F11);
        registerDirectKitKey("spear", GLFW.GLFW_KEY_F12);
        registerDirectKitKey("uhc", GLFW.GLFW_KEY_H);
        registerDirectKitKey("pot", GLFW.GLFW_KEY_P);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pvp")
                .executes(ctx -> {
                    say("Commands: /pvp kit <sword|axe|crystal|bow|minecart|uhc|pot|mace|spear>, /pvp summon, /pvp dismiss, /pvp start, /pvp stop, /pvp coach <on|off>, /pvp bot <spawn|remove|list|attack|stop|settings>. Hotkeys: J/K cycle; F6-F12 kits; H UHC; P pot.");
                    return 1;
                })
                .then(ClientCommandManager.literal("kit")
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String kit = StringArgumentType.getString(ctx, "name").toLowerCase();
                            if (!KIT_ORDER.contains(kit)) {
                                say("Unknown kit. Try sword, axe, crystal, bow, minecart, mace, spear, uhc, or pot.");
                                return 0;
                            }
                            switchKit(kit);
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

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                preTickTarget = null;
                return;
            }
            preTickCooldown = client.player.getAttackCooldownProgress(0.0F);
            if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
                preTickTarget = living;
                preTickHealth = living.getHealth();
                preTickHurtTime = living.hurtTime;
            } else {
                preTickTarget = null;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) {
                wasSwinging = false;
                pendingTarget = null;
                pendingAttackTicks = -1;
                return;
            }
            ticks++;
            if (nextKitKey != null && nextKitKey.wasPressed()) cycleKit(1);
            if (previousKitKey != null && previousKitKey.wasPressed()) cycleKit(-1);
            for (int i = 0; i < DIRECT_KIT_KEYS.size(); i++) {
                if (DIRECT_KIT_KEYS.get(i).wasPressed()) switchKit(KIT_ORDER.get(i));
            }
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
        pendingTarget = client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living ? living : null;
        pendingCooldown = preTickCooldown;
        if (pendingTarget != null) {
            // START_CLIENT_TICK captures the target before vanilla processes this tick's attack input.
            if (pendingTarget == preTickTarget) {
                pendingHealth = preTickHealth;
                pendingHurtTime = preTickHurtTime;
            } else {
                pendingHealth = pendingTarget.getHealth();
                pendingHurtTime = pendingTarget.hurtTime;
            }
        } else {
            pendingHealth = 0.0F;
            pendingHurtTime = 0;
        }
    }

    private static void registerMistake(MinecraftClient client) {
        LivingEntity target = pendingTarget;
        boolean hadTarget = target != null;
        boolean cooldownLow = pendingCooldown < 0.85F;
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
            case "bow" -> items = new String[] {
                "minecraft:bow 1", "minecraft:arrow 64", "minecraft:spectral_arrow 16",
                "minecraft:shield 1", "minecraft:diamond_helmet 1", "minecraft:diamond_chestplate 1",
                "minecraft:diamond_leggings 1", "minecraft:diamond_boots 1", "minecraft:golden_apple 8"
            };
            case "minecart" -> items = new String[] {
                "minecraft:netherite_sword 1", "minecraft:tnt_minecart 16", "minecraft:rail 64",
                "minecraft:powered_rail 32", "minecraft:obsidian 32", "minecraft:flint_and_steel 1",
                "minecraft:ender_pearl 8", "minecraft:diamond_helmet 1", "minecraft:diamond_chestplate 1",
                "minecraft:diamond_leggings 1", "minecraft:diamond_boots 1"
            };
            case "spear" -> items = new String[] {
                "minecraft:netherite_spear 1", "minecraft:shield 1", "minecraft:ender_pearl 8",
                "minecraft:golden_apple 8", "minecraft:diamond_helmet 1", "minecraft:diamond_chestplate 1",
                "minecraft:diamond_leggings 1", "minecraft:diamond_boots 1"
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

    private static void registerDirectKitKey(String kit, int keyCode) {
        DIRECT_KIT_KEYS.add(KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pvp_learner.kit_" + kit, InputUtil.Type.KEYSYM, keyCode, KIT_KEY_CATEGORY)));
    }

    private static void cycleKit(int direction) {
        int current = KIT_ORDER.indexOf(selectedKit);
        if (current < 0) current = 0;
        switchKit(KIT_ORDER.get(Math.floorMod(current + direction, KIT_ORDER.size())));
    }

    private static void switchKit(String kit) {
        if (!KIT_ORDER.contains(kit)) {
            say("Unknown kit. Try sword, axe, crystal, bow, minecart, mace, spear, uhc, or pot.");
            return;
        }
        selectedKit = kit;
        applyPlayerKit(kit);
        lastTip = "Kit: " + kit + ". Track the target and time your swings.";
        say(lastTip);
        if (isPvpBotLoaded()) setupPvpBotCompanion(kit);
        else say("Kit selected. Practice with real players, or install PvP BOT + HeroBot for an AI opponent.");
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
        if (client.player == null) return;
        if (client.player.isOnFire() && !client.options.hudHidden) {
            int screenW = context.getScaledWindowWidth();
            int screenH = context.getScaledWindowHeight();
            for (int i = 0; i < 12; i++) {
                int flameW = Math.max(10, screenW / 24);
                int fx = i * screenW / 12;
                int flicker = (int)(Math.sin(ticks * 0.32 + i * 1.7) * 8);
                int flameH = screenH / 7 + flicker;
                context.fill(fx, screenH - flameH, Math.min(screenW, fx + flameW), screenH, 0x42FF4B16);
                context.fill(fx + 3, screenH - flameH / 2, Math.min(screenW, fx + flameW / 2), screenH, 0x22FFD34F);
            }
        }
        if (!coachEnabled) return;

        int width = 330;
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = 42;
        int height = 84;
        context.fill(x, y, x + width, y + height, 0xC9141824);
        int pulse = 150 + (int)(Math.sin(ticks * 0.18) * 70);
        context.fill(x, y, x + width, y + 2, (pulse << 24) | 0x0057D6FF);
        context.drawTextWithShadow(client.textRenderer, Text.literal("PVP LEARNER  •  " + selectedKit.toUpperCase()), x + 10, y + 7, 0xFF7DD3FC);
        context.drawTextWithShadow(client.textRenderer, Text.literal(training ? "TRAINING: LIVE" : "TRAINING: PAUSED"), x + 10, y + 21, training ? 0xFF75F0A2 : 0xFFFFC56E);
        String stats = "Hits " + confirmedHits + "  |  Swings " + totalSwings + "  |  Mistakes " + mistakes;
        context.drawTextWithShadow(client.textRenderer, Text.literal(stats), x + 10, y + 34, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer, Text.literal(lastTip), x + 10, y + 48, 0xFFFFE7A3);
        context.drawTextWithShadow(client.textRenderer, Text.literal("J Previous  |  K Next  |  F6-F12 Arsenal  |  H UHC  |  P Pot"), x + 10, y + 61, 0xFFB7C9E8);
        if (client.getNetworkHandler() != null) {
            var entries = client.getNetworkHandler().getPlayerList();
            String names = entries.stream().limit(3).map(entry -> entry.getProfile().name()).reduce((a, b) -> a + ", " + b).orElse("No players listed");
            if (entries.size() > 3) names += " +" + (entries.size() - 3);
            context.drawTextWithShadow(client.textRenderer, Text.literal("ONLINE: " + names), x + 10, y + 73, 0xFF9EE7D5);
        }

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
