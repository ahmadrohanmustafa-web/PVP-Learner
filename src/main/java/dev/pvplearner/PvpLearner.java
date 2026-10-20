package dev.pvplearner;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public final class PvpLearner implements ModInitializer {
    private static final Identifier COACH_ID = Identifier.of("pvp_learner", "pvp_coach");
    private static final RegistryKey<EntityType<?>> COACH_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, COACH_ID);

    public static final EntityType<PvpCoachEntity> PVP_COACH = Registry.register(
        Registries.ENTITY_TYPE,
        COACH_ID,
        EntityType.Builder.create(PvpCoachEntity::new, SpawnGroup.MONSTER)
            .dimensions(0.6F, 1.95F)
            .build(COACH_KEY)
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(PVP_COACH, ZombieEntity.createZombieAttributes());
    }
}
