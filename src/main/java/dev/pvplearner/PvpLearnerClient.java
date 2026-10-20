package dev.pvplearner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("pvp")
                .executes(ctx -> { say("PvP Learner: /pvp kit <sword|axe|crystal|uhc|pot|mace>, /pvp start, /pvp stop, /pvp coach <on|off>"); return 1; })
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("kit")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> {
                            String kit = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name").toLowerCase();
                            if (!java.util.Set.of("sword", "axe", "crystal", "uhc", "pot", "mace").contains(kit)) { say("Unknown kit. Try sword, axe, crystal, uhc, pot, or mace."); return 0; }
                            selectedKit = kit; lastTip = "Kit selected: " + kit + ". Start training when ready."; say(lastTip); return 1;
                        })))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("start").executes(ctx -> { training = true; lastTip = "Training started. Keep your crosshair on your opponent and watch your spacing."; say(lastTip); return 1; }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("stop").executes(ctx -> { training = false; lastTip = "Training stopped. Use /pvp to choose another exercise."; say(lastTip); return 1; }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("coach")
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("on").executes(ctx -> { coachEnabled = true; say("PvP coach HUD enabled."); return 1; }))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("off").executes(ctx -> { coachEnabled = false; say("PvP coach HUD hidden."); return 1; })))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("summon").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.getNetworkHandler() == null) { say("You must be in a world first."); return 0; }
                    client.getNetworkHandler().sendChatCommand("summon minecraft:zombie ~ ~ ~3 {CustomName:'{\\"text\\":\\"PvP Coach\\"}',CustomNameVisible:1b,PersistenceRequired:1b,CanPickUpLoot:0b}");
                    say("Summoning a basic PvP sparring zombie nearby. This needs cheats enabled in single-player or command permission on a server.");
                    return 1;
                }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("dismiss").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null || client.getNetworkHandler() == null) { say("You must be in a world first."); return 0; }
                    client.getNetworkHandler().sendChatCommand("kill @e[type=minecraft:zombie,name=\\"PvP Coach\\",distance=..32,limit=1]");
                    say("Asked Minecraft to remove the nearest PvP Coach zombie within 32 blocks.");
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
