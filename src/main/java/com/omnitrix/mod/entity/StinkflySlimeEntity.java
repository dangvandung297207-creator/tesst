package com.omnitrix.mod.entity;

import com.omnitrix.mod.registry.ModRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class StinkflySlimeEntity extends ThrowableProjectile {

    public StinkflySlimeEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public StinkflySlimeEntity(Level level, LivingEntity shooter) {
        super(ModRegistry.STINKFLY_SLIME.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            double px = getX() + (random.nextDouble() - 0.5) * 0.2;
            double py = getY() + (random.nextDouble() - 0.5) * 0.2;
            double pz = getZ() + (random.nextDouble() - 0.5) * 0.2;

            this.level().addParticle(ParticleTypes.ITEM_SLIME, px, py, pz, 0, -0.05, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        if (!this.level().isClientSide) {
            if (target instanceof LivingEntity living) {
                living.hurt(this.damageSources().thrown(this, this.getOwner()), 4.0f);
                // Heavy slowness & blindness (sticky toxic slime trap)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
            }
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
