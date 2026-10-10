package com.omnitrix.mod.alien;

import com.omnitrix.mod.data.OmnitrixPlayerData;
import com.omnitrix.mod.entity.DiamondheadShardEntity;
import com.omnitrix.mod.entity.HeatblastFireballEntity;
import com.omnitrix.mod.entity.StinkflySlimeEntity;
import com.omnitrix.mod.network.ModNetwork;
import com.omnitrix.mod.network.payload.OmnitrixSyncPayload;
import com.omnitrix.mod.network.payload.OmnitrixVfxPayload;
import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AlienAbilities {

    public static void handleAbilityTrigger(ServerPlayer player, int slot, boolean charged) {
        OmnitrixPlayerData data = player.getData(ModRegistry.OMNITRIX_DATA);
        if (!data.isTransformed()) return;

        AlienForm form = data.getCurrentForm();

        // Check and apply cooldowns
        if (slot == 1) {
            if (data.getAbility1Cooldown() > 0) return;
        } else if (slot == 2) {
            if (data.getAbility2Cooldown() > 0) return;
        } else if (slot == 3) {
            if (data.getAbility3Cooldown() > 0) return;
        } else if (slot == 4) {
            // Ultimate: requires 100% ultimate charge
            if (data.getUltimateCharge() < 100) return;
        }

        switch (form) {
            case HEATBLAST -> handleHeatblast(player, data, slot, charged);
            case XLR8 -> handleXLR8(player, data, slot);
            case FOUR_ARMS -> handleFourArms(player, data, slot);
            case DIAMONDHEAD -> handleDiamondhead(player, data, slot);
            case CANNONBOLT -> handleCannonbolt(player, data, slot);
            case UPGRADE -> handleUpgrade(player, data, slot);
            case STINKFLY -> handleStinkfly(player, data, slot);
            case WILDMUTT -> handleWildmutt(player, data, slot);
            default -> {}
        }

        // Add small ultimate charge on ability usage (except when using ultimate)
        if (slot != 4 && data.getUltimateCharge() < 100) {
            data.setUltimateCharge(Math.min(100, data.getUltimateCharge() + 8));
        }

        // Sync data to player and trackers
        ModNetwork.sendToTrackingPlayers(player, new OmnitrixSyncPayload(player.getId(), data));
    }

    // ==========================================
    // 1. HEATBLAST
    // ==========================================
    private static void handleHeatblast(ServerPlayer player, OmnitrixPlayerData data, int slot, boolean charged) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Fire punches / stream
            Vec3 pos = player.getEyePosition().add(look.scale(1.5));
            AABB box = new AABB(pos.x - 1.5, pos.y - 1.5, pos.z - 1.5, pos.x + 1.5, pos.y + 1.5, pos.z + 1.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 7.0f);
                target.igniteForSeconds(5);
                target.knockback(0.8, -look.x, -look.z);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_FIREBALL.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
            data.setAbility1Cooldown(15);
        } else if (slot == 2) {
            // Ability 2: Fireball projectile (chargeable)
            HeatblastFireballEntity fireball = new HeatblastFireballEntity(level, player, charged);
            fireball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, charged ? 2.5f : 1.8f, 1.0f);
            level.addFreshEntity(fireball);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_FIREBALL.get(), SoundSource.PLAYERS, 1.0f, charged ? 0.8f : 1.0f);
            data.setAbility2Cooldown(charged ? 40 : 20);
        } else if (slot == 3) {
            // Ability 3: Ground Fire Slam
            Vec3 slamPos = player.position();
            AABB slamBox = new AABB(slamPos.x - 4, slamPos.y - 1, slamPos.z - 4, slamPos.x + 4, slamPos.y + 2, slamPos.z + 4);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, slamBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 10.0f);
                target.igniteForSeconds(8);
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.5, 0));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_FIRE_SLAM.get(), SoundSource.PLAYERS, 1.2f, 0.9f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FIRE_SLAM_BURST, 0xFF5500, 4));
            data.setAbility3Cooldown(80);
        } else if (slot == 4) {
            // Ultimate: Supernova Inferno Burst
            AABB novaBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, novaBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 22.0f);
                target.igniteForSeconds(15);
                Vec3 away = target.position().subtract(player.position()).normalize().scale(1.5);
                target.setDeltaMovement(away.x, 0.7, away.z);
            }
            level.explode(player, player.getX(), player.getY(), player.getZ(), 4.0f, Level.ExplosionInteraction.NONE);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0xFFAA00, 8));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 2. XLR8
    // ==========================================
    private static void handleXLR8(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Directional High Speed Dash
            Vec3 dashVec = new Vec3(look.x, 0.1, look.z).normalize().scale(2.2);
            player.setDeltaMovement(dashVec);
            player.hurtMarked = true;

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SPEED_DASH.get(), SoundSource.PLAYERS, 1.0f, 1.4f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.SPEED_AFTERIMAGE, 0x00E5FF, 0));
            data.setAbility1Cooldown(20);
        } else if (slot == 2) {
            // Ability 2: Rapid Melee Rush Combo
            AABB rushBox = player.getBoundingBox().inflate(2.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, rushBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 8.0f);
                target.knockback(0.4, -look.x, -look.z);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2, false, false));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SPEED_DASH.get(), SoundSource.PLAYERS, 1.0f, 1.8f);
            data.setAbility2Cooldown(35);
        } else if (slot == 3) {
            // Ability 3: Speed Vortex / Vacuum Spin
            AABB vortexBox = player.getBoundingBox().inflate(5.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, vortexBox, e -> e != player);
            for (LivingEntity target : targets) {
                Vec3 toPlayer = player.position().subtract(target.position()).normalize().scale(0.8);
                target.setDeltaMovement(toPlayer.x, 0.4, toPlayer.z);
                target.hurt(level.damageSources().playerAttack(player), 5.0f);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SPEED_DASH.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
            data.setAbility3Cooldown(70);
        } else if (slot == 4) {
            // Ultimate: Hyperdrive Overclock
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 5, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 240, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 1, false, false));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SPEED_DASH.get(), SoundSource.PLAYERS, 1.5f, 2.0f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0x00FFFF, 6));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 3. FOUR ARMS
    // ==========================================
    private static void handleFourArms(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Heavy Double Haymaker
            Vec3 punchPos = player.getEyePosition().add(look.scale(1.8));
            AABB punchBox = new AABB(punchPos.x - 1.5, punchPos.y - 1.5, punchPos.z - 1.5, punchPos.x + 1.5, punchPos.y + 1.5, punchPos.z + 1.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, punchBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 12.0f);
                target.knockback(1.8, -look.x, -look.z);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_GROUND_SLAM.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
            data.setAbility1Cooldown(25);
        } else if (slot == 2) {
            // Ability 2: Tremor Ground Slam
            Vec3 slamPos = player.position();
            AABB slamBox = new AABB(slamPos.x - 5, slamPos.y - 1, slamPos.z - 5, slamPos.x + 5, slamPos.y + 2, slamPos.z + 5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, slamBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 14.0f);
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.7, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_GROUND_SLAM.get(), SoundSource.PLAYERS, 1.2f, 0.8f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.GROUND_SLAM_DUST, 0x8B0000, 5));
            data.setAbility2Cooldown(60);
        } else if (slot == 3) {
            // Ability 3: Leap Attack (Colossal Jump)
            Vec3 leapVec = new Vec3(look.x * 1.5, 0.9, look.z * 1.5);
            player.setDeltaMovement(leapVec);
            player.hurtMarked = true;
            data.setAbility3Cooldown(80);
        } else if (slot == 4) {
            // Ultimate: Quad Clapper Shockwave
            AABB clapBox = player.getBoundingBox().inflate(9.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, clapBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 26.0f);
                Vec3 push = target.position().subtract(player.position()).normalize().scale(2.5);
                target.setDeltaMovement(push.x, 0.8, push.z);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_GROUND_SLAM.get(), SoundSource.PLAYERS, 2.0f, 0.6f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0xDD2222, 9));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 4. DIAMONDHEAD
    // ==========================================
    private static void handleDiamondhead(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Crystal Shards barrage (3 shards with small spread)
            for (int i = 0; i < 3; i++) {
                DiamondheadShardEntity shard = new DiamondheadShardEntity(level, player);
                shard.shootFromRotation(player, player.getXRot() + (player.getRandom().nextFloat() - 0.5f) * 6,
                        player.getYRot() + (player.getRandom().nextFloat() - 0.5f) * 6, 0.0f, 2.2f, 1.0f);
                level.addFreshEntity(shard);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_CRYSTAL_SHARD.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
            data.setAbility1Cooldown(20);
        } else if (slot == 2) {
            // Ability 2: Defensive Crystal Wall / Spikes
            BlockPos forward = player.blockPosition().relative(player.getDirection(), 2);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = forward.offset(dx, dy, 0);
                    if (level.getBlockState(p).isAir()) {
                        // Place decorative/defensive temporary ice/glass or crystal block
                        level.setBlockAndUpdate(p, Blocks.AMETHYST_BLOCK.defaultBlockState());
                    }
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_CRYSTAL_WALL.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.CRYSTAL_WALL_SUMMON, 0x00FFCC, 0));
            data.setAbility2Cooldown(100);
        } else if (slot == 3) {
            // Ability 3: Diamond Spikes Ground Eruption
            Vec3 pos = player.position().add(look.scale(3.0));
            AABB spikeBox = new AABB(pos.x - 2.5, pos.y - 1, pos.z - 2.5, pos.x + 2.5, pos.y + 2, pos.z + 2.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, spikeBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 12.0f);
                target.setDeltaMovement(0, 0.6, 0);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_CRYSTAL_SHARD.get(), SoundSource.PLAYERS, 1.0f, 0.7f);
            data.setAbility3Cooldown(60);
        } else if (slot == 4) {
            // Ultimate: Crystal Monolith Eruption
            AABB monoBox = player.getBoundingBox().inflate(7.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, monoBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 24.0f);
                target.setDeltaMovement(0, 1.0, 0);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_CRYSTAL_WALL.get(), SoundSource.PLAYERS, 1.5f, 0.6f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0x00FFCC, 7));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 5. CANNONBOLT
    // ==========================================
    private static void handleCannonbolt(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Toggle Rolling Ball Mode
            if (data.getCannonboltRolling() > 0) {
                data.setCannonboltRolling(0);
            } else {
                data.setCannonboltRolling(200); // 10 seconds of roll
                Vec3 roll = new Vec3(look.x, 0.05, look.z).normalize().scale(1.5);
                player.setDeltaMovement(roll);
                player.hurtMarked = true;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModRegistry.SOUND_ABILITY_ROLL_START.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            data.setAbility1Cooldown(10);
        } else if (slot == 2) {
            // Ability 2: Ball Slam / Ricochet Dash
            Vec3 slam = new Vec3(look.x, 0.2, look.z).normalize().scale(2.2);
            player.setDeltaMovement(slam);
            player.hurtMarked = true;
            data.setCannonboltRolling(60);
            data.setAbility2Cooldown(40);
        } else if (slot == 3) {
            // Ability 3: Heavy Shell Armor
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 2));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 140, 0));
            data.setAbility3Cooldown(90);
        } else if (slot == 4) {
            // Ultimate: Wrecking Ball Meteor
            AABB ballBox = player.getBoundingBox().inflate(6.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, ballBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 25.0f);
                target.knockback(2.2, -look.x, -look.z);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_ROLL_IMPACT.get(), SoundSource.PLAYERS, 1.5f, 0.8f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0xFFDD00, 6));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 6. UPGRADE
    // ==========================================
    private static void handleUpgrade(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Galvanic Optical Eye Beam
            Vec3 eye = player.getEyePosition();
            Vec3 end = eye.add(look.scale(16.0));
            AABB beamBox = new AABB(eye, end).inflate(1.2);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 9.0f);
                target.igniteForSeconds(3);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_TECH_BEAM.get(), SoundSource.PLAYERS, 1.0f, 1.3f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.TECH_GLOW_SURGE, 0x00FF00, 0));
            data.setAbility1Cooldown(25);
        } else if (slot == 2) {
            // Ability 2: Liquid Phase (intangibility & speed)
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 3, false, false));
            data.setAbility2Cooldown(80);
        } else if (slot == 3) {
            // Ability 3: Nanite Circuit Overload
            AABB aoe = player.getBoundingBox().inflate(6.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, aoe, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 11.0f);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_TECH_BEAM.get(), SoundSource.PLAYERS, 1.2f, 0.9f);
            data.setAbility3Cooldown(70);
        } else if (slot == 4) {
            // Ultimate: Technological Singularity Burst
            AABB singBox = player.getBoundingBox().inflate(10.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, singBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 25.0f);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_TECH_BEAM.get(), SoundSource.PLAYERS, 1.5f, 0.6f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0x00FF44, 10));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 7. STINKFLY
    // ==========================================
    private static void handleStinkfly(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Sticky Slime Spit
            StinkflySlimeEntity slime = new StinkflySlimeEntity(level, player);
            slime.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.8f, 1.0f);
            level.addFreshEntity(slime);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SLIME_SPIT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            data.setAbility1Cooldown(25);
        } else if (slot == 2) {
            // Ability 2: Wing Boost Flight Launch
            player.setDeltaMovement(new Vec3(look.x * 1.3, 0.85, look.z * 1.3));
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
            data.setAbility2Cooldown(50);
        } else if (slot == 3) {
            // Ability 3: Toxic Slime Carpet
            Vec3 pos = player.position();
            AABB carpetBox = new AABB(pos.x - 4, pos.y - 1, pos.z - 4, pos.x + 4, pos.y + 1, pos.z + 4);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, carpetBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            }
            data.setAbility3Cooldown(80);
        } else if (slot == 4) {
            // Ultimate: Lepidopterran Acid Deluge
            AABB acidBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, acidBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 20.0f);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 2));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 3));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SLIME_SPIT.get(), SoundSource.PLAYERS, 1.5f, 0.7f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0x88CC00, 8));
            data.setUltimateCharge(0);
        }
    }

    // ==========================================
    // 8. WILDMUTT
    // ==========================================
    private static void handleWildmutt(ServerPlayer player, OmnitrixPlayerData data, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        if (slot == 1) {
            // Ability 1: Vulpimancer Pounce & Bite
            Vec3 pounceVec = new Vec3(look.x * 1.8, 0.45, look.z * 1.8);
            player.setDeltaMovement(pounceVec);
            player.hurtMarked = true;

            AABB pounceBox = player.getBoundingBox().inflate(2.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pounceBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 10.0f);
            }
            data.setAbility1Cooldown(30);
        } else if (slot == 2) {
            // Ability 2: Enhanced Sonar Radar Pulse (scans all living entities in 32 blocks)
            AABB scanBox = player.getBoundingBox().inflate(32.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, scanBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, false));
            }
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, false, false));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SONAR_PULSE.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.SONAR_RADAR_PULSE, 0xFF8800, 32));
            data.setAbility2Cooldown(60);
        } else if (slot == 3) {
            // Ability 3: Feral Berserk Frenzy
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 2));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
            data.setAbility3Cooldown(90);
        } else if (slot == 4) {
            // Ultimate: Primal Apex Savage Maul
            AABB apexBox = player.getBoundingBox().inflate(6.0);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, apexBox, e -> e != player);
            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), 24.0f);
                target.knockback(1.5, -look.x, -look.z);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModRegistry.SOUND_ABILITY_SONAR_PULSE.get(), SoundSource.PLAYERS, 1.5f, 0.6f);
            ModNetwork.sendToTrackingPlayers(player,
                    new OmnitrixVfxPayload(player.getId(), OmnitrixVfxPayload.VfxType.FLASH_SHOCKWAVE, 0xFF6600, 6));
            data.setUltimateCharge(0);
        }
    }
}
