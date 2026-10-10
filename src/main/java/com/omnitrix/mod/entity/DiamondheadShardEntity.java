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

public class DiamondheadShardEntity extends ThrowableProjectile {

    public DiamondheadShardEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public DiamondheadShardEntity(Level level, LivingEntity shooter) {
        super(ModRegistry.DIAMONDHEAD_SHARD.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Crystalline glint and shard particle trail
            double px = getX() + (random.nextDouble() - 0.5) * 0.15;
            double py = getY() + (random.nextDouble() - 0.5) * 0.15;
            double pz = getZ() + (random.nextDouble() - 0.5) * 0.15;

            this.level().addParticle(ParticleTypes.CRIT, px, py, pz, 0, 0, 0);
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!this.level().isClientSide) {
            target.hurt(this.damageSources().thrown(this, this.getOwner()), 6.5f);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
