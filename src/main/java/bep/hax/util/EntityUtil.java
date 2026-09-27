package bep.hax.util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.fox.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
public class EntityUtil {
    public static boolean isMonster(Entity entity) {
        return entity instanceof Monster ||
            entity instanceof Slime ||
            entity instanceof Ghast ||
            entity instanceof Phantom ||
            entity instanceof Shulker;
    }
    public static boolean isNeutral(Entity entity) {
        return entity instanceof ZombifiedPiglin ||
            entity instanceof Piglin ||
            entity instanceof EnderMan ||
            entity instanceof Wolf ||
            entity instanceof Llama ||
            entity instanceof TraderLlama ||
            entity instanceof Bee ||
            entity instanceof net.minecraft.world.entity.monster.spider.Spider ||
            entity instanceof CaveSpider ||
            entity instanceof PolarBear ||
            entity instanceof Panda ||
            entity instanceof Dolphin ||
            entity instanceof IronGolem;
    }
    public static boolean isAggressive(Entity entity) {
        if (entity instanceof EnderMan enderman) {
            return enderman.isAngry();
        }
        if (entity instanceof ZombifiedPiglin zombifiedPiglin) {
            return zombifiedPiglin.isAggressive();
        }
        if (entity instanceof Wolf wolf) {
            return wolf.isAggressive();
        }
        if (entity instanceof Piglin piglin) {
            return piglin.isAggressive();
        }
        if (entity instanceof Bee bee) {
            return bee.isAngry();
        }
        if (entity instanceof PolarBear polarBear) {
            return polarBear.isAggressive();
        }
        if (entity instanceof Llama llama) {
            return llama.isAggressive();
        }
        if (entity instanceof IronGolem ironGolem) {
            return ironGolem.isAggressive();
        }
        if (entity instanceof net.minecraft.world.entity.monster.spider.Spider || entity instanceof CaveSpider) {
            return entity.level().getSkyDarken() >= 0.5f;
        }
        return false;
    }
    public static boolean isPassive(Entity entity) {
        return entity instanceof Animal ||
            entity instanceof AmbientCreature ||
            entity instanceof WaterAnimal ||
            entity instanceof Villager ||
            entity instanceof Squid ||
            entity instanceof Bat;
    }
    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }
    public static EntityCategory getEntityCategory(Entity entity) {
        if (isPlayer(entity)) return EntityCategory.PLAYER;
        if (isMonster(entity)) return EntityCategory.MONSTER;
        if (isNeutral(entity)) return EntityCategory.NEUTRAL;
        if (isPassive(entity)) return EntityCategory.PASSIVE;
        return EntityCategory.OTHER;
    }
    public static boolean isLivingTarget(Entity entity) {
        return entity instanceof Player ||
            entity instanceof Mob;
    }
    public static String getEntityName(Entity entity) {
        if (entity instanceof Player player) {
            return player.getGameProfile().name();
        }
        return EntityType.getKey(entity.getType()).getPath();
    }
    public static boolean isUndead(Entity entity) {
        return entity instanceof Zombie ||
            entity instanceof Skeleton ||
            entity instanceof WitherSkeleton ||
            entity instanceof Stray ||
            entity instanceof Husk ||
            entity instanceof Drowned ||
            entity instanceof ZombieVillager ||
            entity instanceof ZombifiedPiglin ||
            entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss ||
            entity instanceof Phantom;
    }
    public static boolean isArthropod(Entity entity) {
        return entity instanceof net.minecraft.world.entity.monster.spider.Spider ||
            entity instanceof CaveSpider ||
            entity instanceof Silverfish ||
            entity instanceof Endermite ||
            entity instanceof Bee;
    }
    public static boolean isVehicle(Entity entity) {
        return entity instanceof Boat ||
            entity instanceof Minecart ||
            entity instanceof MinecartFurnace ||
            entity instanceof MinecartChest;
    }
    public static float getHealth(Entity entity) {
        if (entity instanceof LivingEntity e) {
            return e.getHealth() + e.getAbsorptionAmount();
        }
        return 0.0f;
    }
    public static BlockPos getRoundedBlockPos(Entity entity) {
        return new BlockPos(entity.getBlockX(), (int) Math.round(entity.getY()), entity.getBlockZ());
    }
    public enum EntityCategory {
        PLAYER,
        MONSTER,
        NEUTRAL,
        PASSIVE,
        OTHER
    }
}