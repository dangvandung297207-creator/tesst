package com.animeki.entity;

import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModEntities;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Ki Blast projectile.
 *
 * <p>Movement is sub-stepped ray collision (no per tick block scanning, no vanilla explosion), which
 * keeps it precise at high speed and cheap on a full server. Damage, size and effects are decided by
 * the server when the entity is created; the client only receives the synched visual fields
 * (size, colour, charged flag) and draws the glow itself.</p>
 */
public class KiBlastEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHARGED =
            SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);

    private double damage = 6.0D;
    private double knockback = 0.5D;
    private double explosionRadius = 2.0D;
    private boolean destructive;
    private boolean ultimate;
    private int maxLifeTicks = 100;
    private int lifeTicks;
    @Nullable
    private UUID ownerUUID;

    public KiBlastEntity(EntityType<? extends KiBlastEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public KiBlastEntity(Level level, LivingEntity owner, float size, int color, boolean charged,
                         double damage, double knockback, double explosionRadius, boolean destructive,
                         int maxLifeTicks) {
        this(level, owner, size, color, charged, damage, knockback, explosionRadius, destructive, false,
                maxLifeTicks, 0.95D);
    }

    public KiBlastEntity(Level level, LivingEntity owner, float size, int color, boolean charged,
                         double damage, double knockback, double explosionRadius, boolean destructive,
                         boolean ultimate, int maxLifeTicks, double speed) {
        this(ModEntities.KI_BLAST.get(), level);
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        this.knockback = knockback;
        this.explosionRadius = explosionRadius;
        this.destructive = destructive;
        this.ultimate = ultimate;
        this.maxLifeTicks = maxLifeTicks;
        setSize(size);
        setColor(color);
        setCharged(charged);
        setPos(owner.getEyePosition().add(owner.getViewVector(1.0F).scale(0.9D)));
        setDeltaMovement(owner.getViewVector(1.0F).normalize().scale(speed));
        if (!level.isClientSide) {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE, 0.6F);
        builder.define(DATA_COLOR, 0xFF9FD8FF);
        builder.define(DATA_CHARGED, false);
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    public void setSize(float value) {
        entityData.set(DATA_SIZE, value);
    }

    public int color() {
        return entityData.get(DATA_COLOR);
    }

    public void setColor(int value) {
        entityData.set(DATA_COLOR, value);
    }

    public boolean charged() {
        return entityData.get(DATA_CHARGED);
    }

    public void setCharged(boolean value) {
        entityData.set(DATA_CHARGED, value);
    }

    @Override
    public void tick() {
        super.tick();
        lifeTicks++;
        if (lifeTicks > maxLifeTicks) {
            impact(position());
            return;
        }
        if (level().isClientSide) {
            return;
        }
        Vec3 movement = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(movement);

        // Block collision first so a blast never travels through a wall.
        BlockHitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            setPos(blockHit.getLocation());
            impact(blockHit.getLocation());
            return;
        }

        // Entity collision using the swept bounding box.
        AABB swept = getBoundingBox().expandTowards(movement).inflate(0.35D);
        List<Entity> entities = level().getEntities(this, swept, this::canHitEntity);
        if (!entities.isEmpty()) {
            Entity target = entities.get(0);
            setPos(target.getBoundingBox().getCenter());
            impact(target.getBoundingBox().getCenter());
            return;
        }

        setPos(end);
        if (movement.lengthSqr() > 1.0E-6D) {
            double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
            setYRot((float) (Math.atan2(movement.z, movement.x) * (180.0D / Math.PI)) - 90.0F);
            setXRot((float) (-(Math.atan2(movement.y, horizontal) * (180.0D / Math.PI))));
        }
    }

    private boolean canHitEntity(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator() || entity == this) {
            return false;
        }
        if (ownerUUID != null && ownerUUID.equals(entity.getUUID())) {
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.player.Player player && !player.isAlive()) {
            return false;
        }
        return entity instanceof LivingEntity;
    }

    /** Applies damage, knockback, optional terrain scarring and the impact effect, then despawns. */
    private void impact(Vec3 at) {
        if (level() instanceof ServerLevel serverLevel) {
            Entity owner = ownerUUID == null ? null : serverLevel.getEntity(ownerUUID);
            double radius = Math.max(0.5D, explosionRadius);
            for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    new AABB(at, at).inflate(radius))) {
                double distance = living.position().distanceTo(at);
                if (distance > radius || living == owner) {
                    continue;
                }
                float scaled = (float) (damage * (1.0D - distance / (radius * 1.2D)));
                if (scaled <= 0.0F) {
                    continue;
                }
                living.invulnerableTime = 0;
                if (living.hurt(ModDamageTypes.source(serverLevel, ModDamageTypes.KI_BLAST, this, owner), scaled)) {
                    Vec3 push = living.position().subtract(at).normalize().scale(knockback);
                    living.push(push.x, Math.max(0.15D, push.y) * 0.7D, push.z);
                    living.hurtMarked = true;
                }
            }
            if (destructive) {
                DestructionService.apply(serverLevel, at,
                        ultimate ? DestructionProfile.ultimate(radius) : DestructionProfile.heavyImpact(radius),
                        owner, null);
            }
            VfxDispatcher.impact(serverLevel, charged() ? VfxEvent.ULTIMATE_IMPACT : VfxEvent.KI_BLAST_IMPACT, at,
                    getDeltaMovement(), charged() ? 1.4F : 1.0F);
            serverLevel.playSound(null, at.x, at.y, at.z, com.animeki.registry.ModSounds.KI_BLAST_IMPACT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, charged() ? 0.8F : 1.1F);
        }
        discard();
    }

    /**
     * Creates the projectile inside the level and plays its presentation.
     *
     * <p>Kept here so every caller (abilities, bosses, addons) spawns blasts with the identical
     * behaviour and effect package.</p>
     */
    public static KiBlastEntity fire(ServerLevel level, LivingEntity owner, Vec3 origin, Vec3 direction, float size,
                                     int color, boolean charged, boolean ultimate, double damage, double knockback,
                                     double explosionRadius, boolean destructive, double speed, int lifeTicks) {
        KiBlastEntity blast = new KiBlastEntity(level, owner, size, color, charged, damage, knockback,
                explosionRadius, destructive, ultimate, lifeTicks, speed);
        blast.setPos(origin);
        blast.setDeltaMovement(direction.normalize().scale(speed));
        level.addFreshEntity(blast);
        level.playSound(null, origin.x, origin.y, origin.z, com.animeki.registry.ModSounds.KI_BLAST_FIRE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.1F, charged ? 0.8F : 1.15F);
        VfxDispatcher.play(level, VfxEvent.KI_BLAST_FIRE, origin, direction, size * 1.4F, color);
        return blast;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        damage = tag.getDouble("damage");
        knockback = tag.getDouble("knockback");
        explosionRadius = tag.getDouble("explosionRadius");
        destructive = tag.getBoolean("destructive");
        ultimate = tag.getBoolean("ultimate");
        maxLifeTicks = tag.getInt("maxLife");
        lifeTicks = tag.getInt("life");
        if (tag.hasUUID("owner")) {
            ownerUUID = tag.getUUID("owner");
        }
        setSize(tag.getFloat("size"));
        setColor(tag.getInt("color"));
        setCharged(tag.getBoolean("charged"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("damage", damage);
        tag.putDouble("knockback", knockback);
        tag.putDouble("explosionRadius", explosionRadius);
        tag.putBoolean("destructive", destructive);
        tag.putBoolean("ultimate", ultimate);
        tag.putInt("maxLife", maxLifeTicks);
        tag.putInt("life", lifeTicks);
        if (ownerUUID != null) {
            tag.putUUID("owner", ownerUUID);
        }
        tag.putFloat("size", size());
        tag.putInt("color", color());
        tag.putBoolean("charged", charged());
    }
}
