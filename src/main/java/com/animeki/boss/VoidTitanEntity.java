package com.animeki.boss;

import com.animeki.AnimeKi;
import com.animeki.beam.BeamService;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.network.Payloads;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * VOID TITAN - the original test boss shipped with the framework.
 *
 * <p>The entity only owns state (health, phases, Ki, aura, boss bar) and helpers; the decision
 * making lives in {@link VoidTitanController}. That split is what makes the framework reusable:
 * a different boss is a different controller plus a different entity type, nothing else.</p>
 *
 * <p>Phases: ground brawler, airborne artillery, transformed final form.</p>
 */
public class VoidTitanEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(VoidTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_RAGING =
            SynchedEntityData.defineId(VoidTitanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(VoidTitanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(VoidTitanEntity.class, EntityDataSerializers.INT);

    public static final int MAX_KI = 2000;
    public static final double BASE_HEALTH = 420.0D;
    public static final double BASE_DAMAGE = 13.0D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final VoidTitanController controller = new VoidTitanController(this);

    private boolean transformed;
    private int ki = MAX_KI;
    private int actionTicks;
    private long lastPhaseTransition;

    public VoidTitanEntity(EntityType<? extends VoidTitanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        this.setPersistenceRequired();
        // The titan uses its own controller instead of vanilla goals.
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        applyConfigHealth();
    }

    public static AttributeSupplier.Builder createTitanAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.ARMOR, 6.0D);
    }

    private void applyConfigHealth() {
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        double value = BASE_HEALTH * AnimeKiServerConfig.BOSS.healthMultiplier.get();
        health.setBaseValue(value);
        this.setHealth((float) value);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 0);
        builder.define(DATA_RAGING, false);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_ACTION, 0);
    }

    // ------------------------------------------------------------------ state

    public BossPhase phase() {
        return BossPhase.byIndex(entityData.get(DATA_PHASE));
    }

    public void setPhase(BossPhase phase) {
        entityData.set(DATA_PHASE, phase.index());
    }

    public boolean isRaging() {
        return entityData.get(DATA_RAGING);
    }

    public void setRaging(boolean raging) {
        entityData.set(DATA_RAGING, raging);
    }

    public boolean isCharging() {
        return entityData.get(DATA_CHARGING);
    }

    public void setCharging(boolean charging) {
        entityData.set(DATA_CHARGING, charging);
    }

    public int actionState() {
        return entityData.get(DATA_ACTION);
    }

    public void setActionState(int action) {
        entityData.set(DATA_ACTION, action);
    }

    public int ki() {
        return ki;
    }

    public void setKi(int value) {
        this.ki = Math.max(0, Math.min(MAX_KI, value));
    }

    public boolean useKi(int amount) {
        if (ki < amount) {
            return false;
        }
        ki -= amount;
        return true;
    }

    public double damageMultiplier() {
        double base = phase().damageMultiplier() * AnimeKiServerConfig.BOSS.damageMultiplier.get();
        if (isRaging()) {
            base += AnimeKiServerConfig.BOSS.rageDamageBonus.get();
        }
        return base;
    }

    public ServerLevel serverLevel() {
        return (ServerLevel) level();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void checkDespawn() {
        // Bosses never despawn.
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (actionTicks > 0) {
            actionTicks--;
        }
        // The titan is fire immune; keep it from visually catching fire in daylight.
        if (!level().isClientSide && this.getRemainingFireTicks() > 0) {
            this.setRemainingFireTicks(0);
        }
        if (level().isClientSide) {
            controller.clientTick();
            return;
        }
        if (!AnimeKiServerConfig.BOSS.enabled.get()) {
            return;
        }
        // Slow Ki regeneration keeps the fight attrition based.
        if (this.tickCount % 20 == 0) {
            ki = Math.min(MAX_KI, ki + 40);
        }
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        controller.tick();
    }

    // ------------------------------------------------------------- battle hooks

    /** Called by the controller for every melee hit. */
    public void meleeHit(LivingEntity target) {
        float damage = (float) (BASE_DAMAGE * damageMultiplier());
        target.invulnerableTime = 0;
        if (target.hurt(ModDamageTypes.source(serverLevel(), ModDamageTypes.BOSS, this, this), damage)) {
            Vec3 push = target.position().subtract(position()).normalize().scale(0.9D);
            target.push(push.x, 0.35D, push.z);
            target.hurtMarked = true;
        }
        Vec3 impact = target.getBoundingBox().getCenter();
        VfxDispatcher.impact(serverLevel(), VfxEvent.HEAVY_HIT, impact, null, 1.1F);
        serverLevel().playSound(null, impact.x, impact.y, impact.z, ModSounds.MELEE_HEAVY.get(),
                SoundSource.HOSTILE, 1.3F, 0.75F);
        // The titan borrows the same Ki-charged melee shake as the player.
        VfxDispatcher.cameraNear(serverLevel(), impact, 24.0D, CameraEffectType.SHAKE, 0.35F, 5, this.getId());
    }

    /** Applies the phase transition presentation and buffs, then unlocks the phase abilities. */
    public void advancePhase(BossPhase next) {
        setPhase(next);
        setCharging(false);
        this.invulnerableTime = 40;
        lastPhaseTransition = level().getGameTime();
        if (next == BossPhase.PHASE_3) {
            transformed = true;
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.28D * next.speedMultiplier());
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(BASE_DAMAGE * next.damageMultiplier() * AnimeKiServerConfig.BOSS.damageMultiplier.get());
        }
        if (next.flying()) {
            this.setNoGravity(true);
        }

        Vec3 center = position().add(0.0D, getBbHeight() * 0.5D, 0.0D);
        serverLevel().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_PHASE.get(), SoundSource.HOSTILE, 2.0F,
                0.8F);
        VfxDispatcher.play(serverLevel(), VfxEvent.TRANSFORMATION_SHOCKWAVE, center, null, 2.2F, 0xBF7BFF);
        VfxDispatcher.cameraNear(serverLevel(), center, 64.0D, CameraEffectType.SHAKE, 1.5F, 30, getId());
        VfxDispatcher.cameraNear(serverLevel(), center, 64.0D, CameraEffectType.FLASH, 0.7F, 10, getId());
        VfxDispatcher.impact(serverLevel(), VfxEvent.GROUND_IMPACT, position(), null, 2.0F);
        syncBossState();
    }

    /** Sends the boss bar description used by clients that want to draw their own phase UI. */
    public void syncBossState() {
        Payloads.BossState payload = new Payloads.BossState(getId(), getType().getDescriptionId(),
                getHealth(), getMaxHealth(), (byte) phase().index(), isRaging());
        AnimeKiNetwork.sendToNear(serverLevel(), position(), 96.0D, payload);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            // Ki gain when struck, so a long fight does not starve the boss of abilities.
            ki = Math.min(MAX_KI, ki + (int) (amount * 4.0F));
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        controller.onDeath();
        bossEvent.removeAllPlayers();
        VfxDispatcher.play(serverLevel(), VfxEvent.ULTIMATE_IMPACT, position().add(0.0D, 1.5D, 0.0D), null, 3.0F,
                0xBF7BFF);
        VfxDispatcher.cameraNear(serverLevel(), position(), 64.0D, CameraEffectType.SHAKE, 2.0F, 40, getId());
        serverLevel().playSound(null, getX(), getY(), getZ(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 2.5F,
                0.6F);
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            BeamService.stopOwner(serverLevel(), this, false);
            bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    // ------------------------------------------------------------- multiplayer

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
        syncBossState();
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    public ServerBossEvent bossEvent() {
        return bossEvent;
    }

    public boolean transformed() {
        return transformed;
    }

    public long lastPhaseTransition() {
        return lastPhaseTransition;
    }

    public int actionTicks() {
        return actionTicks;
    }

    public void setActionTicks(int ticks) {
        this.actionTicks = ticks;
    }

    @Nullable
    public LivingEntity findTarget() {
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            return this.getTarget();
        }
        net.minecraft.world.entity.player.Player nearest = level().getNearestPlayer(this, 48.0D);
        if (nearest instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()
                && !player.isCreative()) {
            this.setTarget(player);
            return player;
        }
        return null;
    }

    // ------------------------------------------------------------------ nbt

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AnimekiPhase", phase().index());
        tag.putInt("AnimekiKi", ki);
        tag.putBoolean("AnimekiRaging", isRaging());
        tag.putBoolean("AnimekiTransformed", transformed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setPhase(BossPhase.byIndex(tag.getInt("AnimekiPhase")));
        ki = tag.getInt("AnimekiKi");
        setRaging(tag.getBoolean("AnimekiRaging"));
        transformed = tag.getBoolean("AnimekiTransformed");
        if (phase().flying()) {
            setNoGravity(true);
        }
    }

    /** Marks the entity for a client update; used after teleports and dashes. */
    public void syncMotion() {
        this.hurtMarked = true;
        Entity self = this;
        self.setDeltaMovement(self.getDeltaMovement());
    }

    static void debugLog(String message) {
        AnimeKi.LOGGER.debug("[Void Titan] {}", message);
    }

    /** Convenience used by the controller to check line of sight to a target. */
    public boolean canSee(LivingEntity target) {
        return this.hasLineOfSight(target);
    }

    /** Set by the controller while a scripted sequence plays. */
    public void lookAtTarget(LivingEntity target) {
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    public void stopMoving() {
        this.getNavigation().stop();
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, movement.y, 0.0D);
        syncMotion();
    }

    public void faceTowards(Vec3 target) {
        double dx = target.x - getX();
        double dz = target.z - getZ();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }
}
