package dev.pvplearner;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class PvpLearner implements ModInitializer {
    public static final EntityType<PvpCoachEntity> PVP_COACH = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("pvp_learner", "pvp_coach"),
        EntityType.Builder.create(PvpCoachEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.95F)
            .build("pvp_learner:pvp_coach")
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(PVP_COACH, ZombieEntity.createZombieAttributes());
    }
}
