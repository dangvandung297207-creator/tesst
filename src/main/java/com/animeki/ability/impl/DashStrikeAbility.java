package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.ability.AbilityStopReason;
import com.animeki.ability.ActiveAbility;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.flight.FlightState;
import com.animeki.ki.KiMath;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModSounds;
import com.animeki.util.MathUtil;
import com.animeki.util.Targeting;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Dash Strike - the offensive dash.
 *
 * <p>A short burst of speed that damages everything it passes through. Direction comes from the
 * movement keys (transformed by the server into world space), falling back to the aim when standing
 * still, so it works equally well as a gap closer and as an escape with benefits.</p>
 */
public final class DashStrikeAbility extends Ability {
    public static final String ID = "animeki:dash_strike";

    private static final int DASH_TICKS = 6;

    private final Set<Integer> hitEntities = new HashSet<>();
    private Vec3 direction = Vec3.ZERO;

    public DashStrikeAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(AnimeKiServerConfig.ABILITIES.dashStrike.kiCost.get())
                .cooldownTicks(AnimeKiServerConfig.ABILITIES.dashStrike.cooldownTicks.get())
                .chargeTicks(0)
                .range(AnimeKiServerConfig.ABILITIES.dashStrike.speed.get() * DASH_TICKS)
                .damage(AnimeKiServerConfig.ABILITIES.dashStrike.damage.get())
                .castType(AbilityStats.CastType.SCRIPTED)
                .build());
    }

    @Override
    public String animationId() {
        return "dash_strike";
    }

    @Override
    public boolean canStart(ServerPlayer player, AbilityContext context) {
        return AnimeKiServerConfig.DASH.enabled.get();
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.DashStrike config = AnimeKiServerConfig.ABILITIES.dashStrike;
        this.hitEntities.clear();
        this.direction = resolveDirection(player, context);

        FlightState state = ModAttachments.flight(player);
        state.startDash(direction, config.speed.get(), DASH_TICKS, level.getGameTime());
        player.setDeltaMovement(direction.scale(config.speed.get()));
        player.hurtMarked = true;
        player.resetFallDistance();

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DASH.get(), SoundSource.PLAYERS,
                1.2F, 1.05F);
        VfxDispatcher.playTracking(player, VfxEvent.DASH_IMPACT, player.position().add(0.0D, 1.0D, 0.0D), direction,
                1.2F, VfxEvent.DASH_IMPACT.defaultColor());
        VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.4F, 6);
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.22F, 8);
        if (config.speed.get() >= AnimeKiServerConfig.DASH.sonicBoomSpeed.get()) {
            VfxDispatcher.playTracking(player, VfxEvent.SONIC_BOOM, player.position().add(0.0D, 1.0D, 0.0D),
                    direction, 1.0F, VfxEvent.SONIC_BOOM.defaultColor());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SONIC_BOOM.get(),
                    SoundSource.PLAYERS, 1.0F, 0.95F);
        }
    }

    @Override
    public void onTick(ServerPlayer player, AbilityContext context, ActiveAbility active) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.DashStrike config = AnimeKiServerConfig.ABILITIES.dashStrike;
        FlightState state = ModAttachments.flight(player);

        if (active.ticks() > DASH_TICKS) {
            state.setDashTicks(0);
            active.markFinished();
            return;
        }

        player.setDeltaMovement(direction.scale(config.speed.get()));
        player.hurtMarked = true;
        player.resetFallDistance();

        double travelled = config.speed.get();
        Vec3 from = player.position().subtract(direction.scale(travelled));
        Vec3 to = player.position().add(direction.scale(travelled * 0.5D));
        List<LivingEntity> hit = Targeting.entitiesAlongSegment(level, from, to, 1.2D, player,
                entity -> entity.isAlive() && !entity.isSpectator());
        for (LivingEntity target : hit) {
            if (!hitEntities.add(target.getId())) {
                continue;
            }
            float damage = (float) (config.damage.get() * KiMath.meleeMultiplier(player));
            target.invulnerableTime = 0;
            if (target.hurt(ModDamageTypes.of(level, ModDamageTypes.MARTIAL, player), damage)) {
                Vec3 push = direction.scale(config.knockback.get());
                target.push(push.x, 0.35D, push.z);
                target.hurtMarked = true;
            }
            VfxDispatcher.impact(level, VfxEvent.DASH_IMPACT, target.getBoundingBox().getCenter(), direction, 1.2F);
            VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.5F, 6);
        }

        if (player.horizontalCollision) {
            VfxDispatcher.playTracking(player, VfxEvent.DASH_IMPACT, player.position(), direction, 1.0F,
                    VfxEvent.DASH_IMPACT.defaultColor());
            state.setDashTicks(0);
            active.markFinished();
        }
    }

    @Override
    public void onStop(ServerPlayer player, AbilityContext context, AbilityStopReason reason) {
        ModAttachments.flight(player).setDashTicks(0);
    }

    /** Movement keys first, aim second, so the strike always goes somewhere sensible. */
    private Vec3 resolveDirection(ServerPlayer player, AbilityContext context) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 strafe = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 combined = forward.scale(context.inputForward()).add(strafe.scale(context.inputStrafe()));
        if (combined.lengthSqr() < 1.0E-4D) {
            return context.direction();
        }
        Vec3 flat = MathUtil.horizontal(combined);
        if (player.isShiftKeyDown()) {
            return new Vec3(flat.x, -0.35D, flat.z).normalize();
        }
        return flat.normalize();
    }
}
