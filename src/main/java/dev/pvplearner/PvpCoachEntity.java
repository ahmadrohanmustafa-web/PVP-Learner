package dev.pvplearner;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;

/** A hostile sparring NPC rendered with the standard Steve player model. */
public final class PvpCoachEntity extends ZombieEntity {
    public PvpCoachEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
        setCanPickUpLoot(false);
        setPersistent();
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty difficulty) {
        super.initEquipment(random, difficulty);
        equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }
}
