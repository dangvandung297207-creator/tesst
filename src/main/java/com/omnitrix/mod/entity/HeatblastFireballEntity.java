package com.omnitrix.mod.entity;

import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class HeatblastFireballEntity extends ThrowableProjectile {

    private boolean charged = false;

    public HeatblastFireballEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public HeatblastFireballEntity(Level level, LivingEntity shooter, boolean charged) {
        super(ModRegistry.HEATBLAST_FIREBALL.get(), shooter, level);
        this.charged = charged;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public boolean isCharged() {
        return charged;
    }

    public void setCharged(boolean charged) {
        this.charged = charged;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // High quality fire trail
            double px = getX() + (random.nextDouble() - 0.5) * 0.2;
            double py = getY() + (random.nextDouble() - 0.5) * 0.2;
            double pz = getZ() + (random.nextDouble() - 0.5) * 0.2;

            this.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.02, 0);
            this.level().addParticle(ParticleTypes.SMOKE, px, py, pz, 0, 0.05, 0);
            if (charged) {
                this.level().addParticle(ParticleTypes.LAVA, px, py, pz, 0, 0, 0);
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0, 0.01, 0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!this.level().isClientSide) {
            float damage = charged ? 14.0f : 7.0f;
            target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
            target.igniteForSeconds(charged ? 10 : 5);
            explode();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            explode();
        }
    }

    private void explode() {
        if (!this.level().isClientSide) {
            float radius = charged ? 2.5f : 1.2f;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius,
                    Level.ExplosionInteraction.NONE); // Controlled explosion without destroying player builds
            this.discard();
        }
    }
}
