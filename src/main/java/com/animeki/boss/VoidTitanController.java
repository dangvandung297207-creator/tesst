package com.animeki.boss;

import com.animeki.beam.BeamService;
import com.animeki.beam.BeamType;
import com.animeki.beam.BeamTypes;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.entity.KiBlastEntity;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Decision making for the Void Titan.
 *
 * <p>A phase aware state machine: pick a target, weigh the abilities unlocked by the current phase
 * (respecting individual cooldowns), run the chosen sequence and fall back to a chase. Every move
 * is presented with sound, VFX and camera shake, and only the moves that should scar the terrain
 * do so - through the shared, budgeted destruction service.</p>
 */
final class VoidTitanController {
    /** Runtime state of the controller. */
    private enum Action {
        IDLE,
        CHASE,
        MELEE,
        DASH,
        SLAM,
        VOLLEY,
        BEAM,
        TELEPORT,
        TRANSITION,
        ULTIMATE,
        RECOVER,
        DEAD
    }

    private static final double MELEE_REACH = 3.6D;
    private static final int MELEE_HIT_INTERVAL = 7;

    private final VoidTitanEntity titan;
    private final Map<BossAbility, Long> nextUse = new EnumMap<>(BossAbility.class);
    private final Set<BossAbility> usedDuringFight = EnumSet.noneOf(BossAbility.class);

    private Action action = Action.IDLE;
    private BossPhase pendingPhase = BossPhase.PHASE_1;
    private int actionTick;
    private int meleeHits;
    private int beamId = -1;
    private int volleyFired;
    private Vec3 ultimateCenter = Vec3.ZERO;
    private int ultimatePhase;
    private LivingEntity cachedTarget;

    VoidTitanController(VoidTitanEntity titan) {
        this.titan = titan;
    }

    private ServerLevel level() {
        return titan.serverLevel();
    }

    // ------------------------------------------------------------------ client

    /** Purely cosmetic twitches; the heavy aura work is done by the client renderer. */
    void clientTick() {
        if (!titan.isCharging() || titan.tickCount % 3 != 0) {
            return;
        }
        // Client side particles only - never sent over the network.
        titan.level().addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                titan.getRandomX(1.4D), titan.getY() + titan.getRandom().nextDouble() * titan.getBbHeight(),
                titan.getRandomZ(1.4D), 0.0D, 0.03D, 0.0D);
    }

    // ------------------------------------------------------------------ server

    void tick() {
        if (action == Action.DEAD) {
            return;
        }
        if (!AnimeKiServerConfig.BOSS.enabled.get()) {
            titan.stopMoving();
            return;
        }
        if (titan.isDeadOrDying()) {
            onDeath();
            return;
        }

        if (action != Action.TRANSITION && !titan.transformed()) {
            BossPhase desired = desiredPhase();
            if (desired != titan.phase()) {
                beginTransition(desired);
                return;
            }
        }
        if (!titan.isRaging() && titan.getHealth() / titan.getMaxHealth() < 0.18F) {
            titan.setRaging(true);
            titan.setCharging(true);
            titan.setActionTicks(20);
            level().playSound(null, titan.getX(), titan.getY(), titan.getZ(), ModSounds.BOSS_ROAR.get(),
                    SoundSource.HOSTILE, 2.0F, 0.7F);
            VfxDispatcher.play(level(), VfxEvent.TRANSFORMATION_SHOCKWAVE, titan.position().add(0.0D, 1.5D, 0.0D),
                    null, 1.6F, 0xFF4D4D);
            titan.syncBossState();
        }

        actionTick++;
        LivingEntity target = titan.findTarget();
        if (target == null && action != Action.TRANSITION && action != Action.ULTIMATE) {
            titan.stopMoving();
            action = Action.IDLE;
            return;
        }
        cachedTarget = target;

        switch (action) {
            case IDLE -> chooseAction(target);
            case CHASE -> tickChase(target);
            case MELEE -> tickMelee(target);
            case DASH -> tickDash(target);
            case SLAM -> tickSlam(target);
            case VOLLEY -> tickVolley(target);
            case BEAM -> tickBeam(target);
            case TELEPORT -> tickTeleport(target);
            case TRANSITION -> tickTransition();
            case ULTIMATE -> tickUltimate();
            case RECOVER -> tickRecover();
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ phases

    private BossPhase desiredPhase() {
        float fraction = titan.getHealth() / titan.getMaxHealth();
        double phase2 = AnimeKiServerConfig.BOSS.phase2Threshold.get();
        double phase3 = AnimeKiServerConfig.BOSS.phase3Threshold.get();
        if (fraction <= phase3) {
            return BossPhase.PHASE_3;
        }
        if (fraction <= phase2) {
            return BossPhase.PHASE_2;
        }
        return BossPhase.PHASE_1;
    }

    private void beginTransition(BossPhase next) {
        action = Action.TRANSITION;
        actionTick = 0;
        titan.stopMoving();
        titan.setCharging(true);
        pendingPhase = next;
        VfxDispatcher.play(level(), VfxEvent.KI_CHARGE_TICK, titan.position().add(0.0D, 1.0D, 0.0D), null, 2.0F,
                0xBF7BFF);
        level().playSound(null, titan.getX(), titan.getY(), titan.getZ(), ModSounds.TRANSFORM_BUILDUP.get(),
                SoundSource.HOSTILE, 2.0F, 0.8F);
    }

    private void tickTransition() {
        titan.stopMoving();
        Vec3 center = titan.position().add(0.0D, titan.getBbHeight() * 0.5D, 0.0D);
        int duration = 60;
        if (actionTick % 5 == 0) {
            VfxDispatcher.play(level(), VfxEvent.KI_CHARGE_TICK, center, null, 1.5F, 0xBF7BFF);
            VfxDispatcher.cameraNear(level(), center, 48.0D, CameraEffectType.SHAKE, 0.25F, 4, actionTick);
        }
        if (actionTick >= duration) {
            titan.setCharging(false);
            titan.advancePhase(pendingPhase);
            areaDamage(titan.position(), 8.0D, 10.0D * titan.damageMultiplier(), 1.4D);
            if (destructionAllowed()) {
                DestructionService.apply(level(), titan.position(),
                        DestructionProfile.transformationBlast(6.0D), titan, null);
            }
            action = Action.RECOVER;
            actionTick = 0;
            titan.setActionTicks(30);
        }
    }

    // ------------------------------------------------------------------ moves

    private void chooseAction(@Nullable LivingEntity target) {
        if (target == null) {
            action = Action.IDLE;
            return;
        }
        BossAbility chosen = pickAbility();
        if (chosen == null) {
            action = Action.CHASE;
            actionTick = 0;
            return;
        }
        nextUse.put(chosen, level().getGameTime() + chosen.cooldownTicks(titan.phase()));
        actionTick = 0;
        meleeHits = 0;
        volleyFired = 0;
        switch (chosen) {
            case MELEE_COMBO -> action = Action.MELEE;
            case DASH_SLAM -> {
                action = Action.DASH;
                Vec3 direction = target.position().subtract(titan.position()).normalize();
                titan.faceTowards(target.position());
                titan.setDeltaMovement(direction.x * 1.35D, 0.35D, direction.z * 1.35D);
                titan.syncMotion();
            }
            case GROUND_SLAM -> {
                action = Action.SLAM;
                titan.setDeltaMovement(0.0D, 0.75D, 0.0D);
                titan.syncMotion();
            }
            case KI_VOLLEY -> action = Action.VOLLEY;
            case BEAM_SWEEP -> {
                action = Action.BEAM;
                BeamType type = BeamTypes.VOID;
                Vec3 origin = titan.getEyePosition().add(titan.getViewVector(1.0F).scale(1.2D));
                var beam = BeamService.create(level(), titan, type, origin,
                        aimDirection(target), titan.damageMultiplier() * 0.6D, 140);
                beamId = beam.id();
                titan.setCharging(true);
            }
            case TELEPORT -> action = Action.TELEPORT;
            case AURA_BURST -> {
                action = Action.SLAM;
                titan.setDeltaMovement(0.0D, 0.45D, 0.0D);
                titan.syncMotion();
            }
            case ULTIMATE -> {
                action = Action.ULTIMATE;
                ultimatePhase = 0;
                ultimateCenter = target.position();
                titan.setCharging(true);
                titan.setDeltaMovement(0.0D, 0.0D, 0.0D);
                titan.syncMotion();
            }
        }
        usedDuringFight.add(chosen);
    }

    @Nullable
    private BossAbility pickAbility() {
        long now = level().getGameTime();
        BossAbility best = null;
        int totalWeight = 0;
        for (BossAbility ability : BossAbility.values()) {
            if (!ability.available(titan.phase())) {
                continue;
            }
            if (nextUse.getOrDefault(ability, 0L) > now) {
                continue;
            }
            if (ability == BossAbility.ULTIMATE && titan.ki() < 800) {
                continue;
            }
            // Prefer abilities that have not been used yet so fights stay varied.
            int weight = ability.weight() * (usedDuringFight.contains(ability) ? 1 : 2);
            totalWeight += weight;
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = level().getRandom().nextInt(totalWeight);
        for (BossAbility ability : BossAbility.values()) {
            if (!ability.available(titan.phase()) || nextUse.getOrDefault(ability, 0L) > now) {
                continue;
            }
            if (ability == BossAbility.ULTIMATE && titan.ki() < 800) {
                continue;
            }
            roll -= ability.weight() * (usedDuringFight.contains(ability) ? 1 : 2);
            if (roll < 0) {
                best = ability;
                break;
            }
        }
        return best;
    }

    private void tickChase(LivingEntity target) {
        titan.lookAtTarget(target);
        double speed = 1.0D * titan.phase().speedMultiplier();
        titan.getNavigation().moveTo(target, speed);
        if (titan.distanceTo(target) <= MELEE_REACH + 0.75D) {
            titan.getNavigation().stop();
            action = Action.MELEE;
            actionTick = 0;
            return;
        }
        if (actionTick > 60) {
            action = Action.RECOVER;
            actionTick = 0;
        }
    }

    private void tickMelee(LivingEntity target) {
        titan.lookAtTarget(target);
        titan.stopMoving();
        if (actionTick % MELEE_HIT_INTERVAL == 0 && meleeHits < 3) {
            double reach = MELEE_REACH + (meleeHits == 2 ? 1.4D : 0.0D);
            if (titan.distanceTo(target) <= reach) {
                titan.meleeHit(target);
                if (meleeHits == 2) {
                    Vec3 push = target.position().subtract(titan.position()).normalize().scale(1.1D);
                    target.push(push.x, 0.5D, push.z);
                    target.hurtMarked = true;
                }
                meleeHits++;
            } else if (meleeHits == 0) {
                action = Action.CHASE;
                actionTick = 0;
                return;
            }
        }
        if (meleeHits >= 3 || actionTick > 40) {
            action = Action.RECOVER;
            actionTick = 0;
            titan.setActionTicks(15);
        }
    }

    private void tickDash(LivingEntity target) {
        Vec3 movement = titan.getDeltaMovement();
        if (titan.onGround() && actionTick > 4) {
            groundImpact(4.5D, 12.0D * titan.damageMultiplier(), 0.9D, 3.5D);
            action = Action.RECOVER;
            actionTick = 0;
            titan.setActionTicks(20);
            return;
        }
        if (actionTick > 30) {
            action = Action.RECOVER;
            actionTick = 0;
            return;
        }
        titan.setDeltaMovement(movement.x, movement.y - 0.045D, movement.z);
        titan.syncMotion();
    }

    private void tickSlam(LivingEntity target) {
        Vec3 movement = titan.getDeltaMovement();
        if (actionTick < 10) {
            titan.setDeltaMovement(movement.x * 0.6D, 0.62D, movement.z * 0.6D);
        } else {
            titan.setDeltaMovement(movement.x * 0.6D, Math.min(movement.y, -1.1D), movement.z * 0.6D);
            if (titan.onGround()) {
                groundImpact(6.5D, 15.0D * titan.damageMultiplier(), 1.1D, 5.0D);
                action = Action.RECOVER;
                actionTick = 0;
                titan.setActionTicks(25);
                return;
            }
        }
        titan.syncMotion();
    }

    private void tickVolley(LivingEntity target) {
        titan.lookAtTarget(target);
        titan.stopMoving();
        if (actionTick % 6 == 0 && volleyFired < 5) {
            if (titan.useKi(120)) {
                Vec3 direction = aimDirection(target);
                boolean charged = volleyFired == 4;
                KiBlastEntity blast = new KiBlastEntity(level(), titan, charged ? 1.2F : 0.75F, 0xBF7BFF, charged,
                        9.0D * titan.damageMultiplier(), 0.5D, charged ? 3.0D : 1.8D, false, 80);
                blast.setPos(titan.getEyePosition().add(direction.scale(1.4D)));
                blast.setDeltaMovement(direction.scale(charged ? 1.05D : 0.9D));
                level().addFreshEntity(blast);
                volleyFired++;
            }
        }
        if (volleyFired >= 5 || actionTick > 60) {
            action = Action.RECOVER;
            actionTick = 0;
            titan.setActionTicks(15);
        }
    }

    private void tickBeam(LivingEntity target) {
        titan.lookAtTarget(target);
        titan.stopMoving();
        if (beamId >= 0) {
            var beam = BeamService.byId(level(), beamId);
            if (beam == null) {
                titan.setCharging(false);
                action = Action.RECOVER;
                actionTick = 0;
                return;
            }
            beam.setOrigin(titan.getEyePosition().add(titan.getViewVector(1.0F).scale(1.2D)));
            beam.setDirection(aimDirection(target));
            if (actionTick > 120 || titan.ki() <= 0) {
                BeamService.stop(level(), beamId, true);
                beamId = -1;
                titan.setCharging(false);
                titan.setKi(Math.max(0, titan.ki() - 300));
                action = Action.RECOVER;
                actionTick = 0;
                titan.setActionTicks(40);
            }
        }
    }

    private void tickTeleport(LivingEntity target) {
        if (actionTick == 1) {
            Vec3 behind = target.position().subtract(target.getViewVector(1.0F).normalize().scale(3.0D));
            boolean moved = titan.randomTeleport(behind.x, behind.y, behind.z, false);
            if (!moved) {
                titan.randomTeleport(target.getX(), target.getY() + 1.0D, target.getZ(), false);
            }
            Vec3 center = titan.position().add(0.0D, titan.getBbHeight() * 0.5D, 0.0D);
            VfxDispatcher.play(level(), VfxEvent.VANISH, center, null, 1.4F, 0x9A6BFF);
            VfxDispatcher.play(level(), VfxEvent.AURA_BURST, center, null, 1.2F, 0x9A6BFF);
            level().playSound(null, titan.getX(), titan.getY(), titan.getZ(), ModSounds.VANISH.get(),
                    SoundSource.HOSTILE, 1.4F, 0.8F);
            titan.setActionState(1);
        }
        if (actionTick > 12) {
            action = Action.MELEE;
            actionTick = 0;
            meleeHits = 0;
            titan.setActionState(0);
        }
    }

    private void tickUltimate() {
        titan.stopMoving();
        Vec3 center = titan.position();
        switch (ultimatePhase) {
            case 0 -> {
                // Rise and charge.
                titan.setDeltaMovement(0.0D, 0.55D, 0.0D);
                titan.syncMotion();
                if (actionTick % 4 == 0) {
                    VfxDispatcher.play(level(), VfxEvent.KI_CHARGE_TICK, center.add(0.0D, 1.5D, 0.0D), null, 2.4F,
                            0xBF7BFF);
                    VfxDispatcher.cameraNear(level(), center, 64.0D, CameraEffectType.SHAKE, 0.3F, 5, actionTick);
                }
                if (actionTick >= 40) {
                    ultimatePhase = 1;
                    actionTick = 0;
                    VfxDispatcher.play(level(), VfxEvent.TARGET_MARKER, ultimateCenter, null, 9.0F, 0xBF7BFF);
                    level().playSound(null, center.x, center.y, center.z, ModSounds.ULTIMATE_CHARGE.get(),
                            SoundSource.HOSTILE, 2.4F, 0.7F);
                }
            }
            case 1 -> {
                // Hold position, gather the sphere.
                titan.setDeltaMovement(0.0D, 0.0D, 0.0D);
                titan.syncMotion();
                if (actionTick % 3 == 0) {
                    VfxDispatcher.play(level(), VfxEvent.ULTIMATE_CHARGE, center.add(0.0D, 2.5D, 0.0D), null,
                            1.0F + actionTick * 0.06F, 0xBF7BFF);
                }
                if (actionTick >= 50) {
                    ultimatePhase = 2;
                    actionTick = 0;
                }
            }
            case 2 -> {
                // Descend onto the marked area.
                Vec3 direction = ultimateCenter.subtract(center);
                if (direction.lengthSqr() > 1.0D) {
                    titan.setDeltaMovement(direction.normalize().scale(1.0D).add(0.0D, -0.35D, 0.0D));
                    titan.syncMotion();
                } else {
                    ultimatePhase = 3;
                    actionTick = 0;
                    ultimateImpact();
                }
            }
            default -> {
                if (actionTick > 60) {
                    action = Action.RECOVER;
                    actionTick = 0;
                    titan.setCharging(false);
                    titan.setActionTicks(60);
                }
            }
        }
    }

    private void ultimateImpact() {
        Vec3 center = ultimateCenter;
        double radius = 12.0D;
        double damage = 45.0D * titan.damageMultiplier();
        areaDamage(center, radius, damage, 2.0D);
        if (destructionAllowed()) {
            DestructionService.apply(level(), center, DestructionProfile.meteorImpact(9.0D), titan, null);
        }
        VfxDispatcher.play(level(), VfxEvent.ULTIMATE_IMPACT, center, null, 3.4F, 0xBF7BFF);
        VfxDispatcher.play(level(), VfxEvent.GROUND_IMPACT, center, null, 3.0F, 0x9A6BFF);
        VfxDispatcher.cameraNear(level(), center, 80.0D, CameraEffectType.SHAKE, 2.4F, 45, titan.getId());
        VfxDispatcher.cameraNear(level(), center, 80.0D, CameraEffectType.FLASH, 0.85F, 14, titan.getId());
        level().playSound(null, center.x, center.y, center.z, ModSounds.ULTIMATE_IMPACT.get(), SoundSource.HOSTILE,
                3.0F, 0.8F);
        titan.setKi(Math.max(0, titan.ki() - 800));
        titan.setCharging(false);
    }

    private void tickRecover() {
        titan.stopMoving();
        if (actionTick > Math.max(10, titan.actionTicks())) {
            action = Action.IDLE;
            actionTick = 0;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void groundImpact(double radius, double damage, double knockUp, double craterRadius) {
        Vec3 center = titan.position();
        areaDamage(center, radius, damage, knockUp);
        if (destructionAllowed()) {
            DestructionService.apply(level(), center, DestructionProfile.groundSlam(craterRadius), titan, null);
        }
        VfxDispatcher.play(level(), VfxEvent.GROUND_IMPACT, center, null, (float) radius * 0.4F, 0x9A6BFF);
        VfxDispatcher.cameraNear(level(), center, 48.0D, CameraEffectType.SHAKE, 1.2F, 20, titan.getId());
        level().playSound(null, center.x, center.y, center.z, ModSounds.GROUND_SLAM.get(), SoundSource.HOSTILE, 1.8F,
                0.7F);
    }

    private void areaDamage(Vec3 center, double radius, double damage, double knockUp) {
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius))) {
            if (entity == titan || !entity.isAlive()) {
                continue;
            }
            if (entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            double distance = entity.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float scaled = (float) (damage * (1.0D - distance / (radius * 1.15D)));
            if (scaled <= 0.0F) {
                continue;
            }
            entity.invulnerableTime = 0;
            if (entity.hurt(ModDamageTypes.source(level(), ModDamageTypes.BOSS, titan, titan), scaled)) {
                Vec3 push = entity.position().subtract(center);
                if (push.lengthSqr() < 1.0E-4D) {
                    push = new Vec3(0.0D, 1.0D, 0.0D);
                }
                push = push.normalize().scale(0.9D);
                entity.push(push.x, knockUp, push.z);
                entity.hurtMarked = true;
            }
        }
    }

    private Vec3 aimDirection(LivingEntity target) {
        Vec3 from = titan.getEyePosition();
        Vec3 to = target.getEyePosition().add(target.getDeltaMovement().scale(6.0D));
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-4D) {
            return titan.getViewVector(1.0F);
        }
        return direction.normalize();
    }

    private boolean destructionAllowed() {
        if (!AnimeKiServerConfig.BOSS.terrainDestruction.get() || !AnimeKiServerConfig.DESTRUCTION.enabled.get()) {
            return false;
        }
        if (AnimeKiServerConfig.BOSS.respectMobGriefing.get()
                && !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }
        return true;
    }

    void onDeath() {
        action = Action.DEAD;
        if (beamId >= 0) {
            BeamService.stop(level(), beamId, false);
            beamId = -1;
        }
        titan.setCharging(false);
    }
}
