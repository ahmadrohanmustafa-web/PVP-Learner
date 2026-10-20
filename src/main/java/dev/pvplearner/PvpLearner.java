package dev.pvplearner;

import net.fabricmc.api.ModInitializer;

/** PvP Learner uses PvP BOT as an optional companion through its documented commands. */
public final class PvpLearner implements ModInitializer {
    @Override
    public void onInitialize() {
        // No custom zombie/entity is registered. Combat is delegated to optional PvP BOT.
    }
}
