package com.animeki.flight;

import com.animeki.ability.AbilityService;
import com.animeki.ability.FeedbackReason;
import com.animeki.combat.CombatService;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.ki.KiService;
import com.animeki.network.AnimeKiNetwork;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * High speed dash with afterimages, optional invulnerability frames, impact damage and the chase
 * follow up that makes knocked up enemies reachable in the air.
 */
public final class DashService {
    public static final String COOLDOWN_ID = "animeki:dash";

    private static final int DASH_TICKS = 5;
    private static final Map<UUID, Long> INVULNERABLE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> HIT_THIS_DASH = new ConcurrentHashMap<>();

    private DashService() {
    }

    public static boolean tryDash(ServerPlayer player, float forward, float strafe, boolean sprint) {
        AnimeKiServerConfig.Dash config = AnimeKiServerConfig.DASH;
        if (!config.enabled.get()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        long gameTime = level.getGameTime();
        FlightState state = ModAttachments.flight(player);

        int remaining = AbilityService.cooldownRemaining(player, COOLDOWN_ID);
        if (remaining > 0) {
            AbilityService.feedback(player, FeedbackReason.ON_COOLDOWN, COOLDOWN_ID);
            return false;
        }

        boolean airborne = !player.onGround();
        if (airborne) {
            if (!config.airDashEnabled.get()) {
                return false;
            }
            if (state.airDashCharges() <= 0) {
                AbilityService.feedback(player, FeedbackReason.REFUSED, COOLDOWN_ID);
                return false;
            }
        }

        if (!KiService.tryConsume(player, config.kiCost.get(), true)) {
            AbilityService.feedback(player, FeedbackReason.NOT_ENOUGH_KI, COOLDOWN_ID);
            return false;
        }

        // Chase attack: follow a launched enemy instead of free dashing.
        LivingEntity chase = CombatService.chaseTarget(player);
        Vec3 direction;
        if (chase != null) {
            direction = chase.position().add(0.0D, chase.getBbHeight() * 0.5D, 0.0D)
                    .subtract(player.getEyePosition());
        } else {
            direction = inputDirection(player, forward, strafe);
        }
        if (direction.lengthSqr() < 1.0E-5D) {
            return false;
        }
        direction = direction.normalize();

        double speed = airborne ? config.airSpeed.get() : config.speed.get();
        state.startDash(direction, speed, DASH_TICKS, gameTime);
        if (airborne) {
            state.setAirDashCharges(state.airDashCharges() - 1);
        }
        player.setDeltaMovement(direction.scale(speed).add(0.0D, Math.max(0.0D, player.getDeltaMovement().y * 0.4D), 0.0D));
        player.hurtMarked = true;
        player.resetFallDistance();

        int invulnerability = config.invulnerabilityTicks.get();
        if (invulnerability > 0) {
            INVULNERABLE_UNTIL.put(player.getUUID(), gameTime + invulnerability);
        }
        HIT_THIS_DASH.put(player.getUUID(), new HashSet<>());

        AbilityService.setCooldown(player, COOLDOWN_ID, config.cooldownTicks.get());

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DASH.get(), SoundSource.PLAYERS,
                1.0F, 1.15F);
        VfxDispatcher.playTracking(player, VfxEvent.DASH_IMPACT, player.position().add(0.0D, 1.0D, 0.0D), direction,
                1.0F, VfxEvent.DASH_IMPACT.defaultColor());
        VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.35F, 6);
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.18F, 8);
        if (speed >= config.sonicBoomSpeed.get()) {
            VfxDispatcher.playTracking(player, VfxEvent.SONIC_BOOM, player.position().add(0.0D, 1.0D, 0.0D),
                    direction, 1.0F, VfxEvent.SONIC_BOOM.defaultColor());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SONIC_BOOM.get(),
                    SoundSource.PLAYERS, 1.0F, 0.9F);
        }
        AnimeKiNetwork.syncVisualState(player);
        return true;
    }

    /** Dash direction from raw input, falling back to the look vector. */
    private static Vec3 inputDirection(ServerPlayer player, float forward, float strafe) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 lookForward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 lookRight = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 combined = lookForward.scale(forward).add(lookRight.scale(strafe));
        if (combined.lengthSqr() < 1.0E-4D && Math.abs(forward) < 0.01F && Math.abs(strafe) < 0.01F) {
            return player.getViewVector(1.0F);
        }
        return combined;
    }

    public static void tick(ServerPlayer player) {
        FlightState state = ModAttachments.flight(player);
        if (state.dashTicks() <= 0) {
            INVULNERABLE_UNTIL.remove(player.getUUID());
            HIT_THIS_DASH.remove(player.getUUID());
            return;
        }
        state.tickDash();
        AnimeKiServerConfig.Dash config = AnimeKiServerConfig.DASH;
        Vec3 direction = state.dashDirection();
        Vec3 velocity = direction.scale(state.dashSpeed());

        // Follow the target during a chase dash so the strike actually lands.
        LivingEntity chase = CombatService.chaseTarget(player);
        if (chase != null) {
            double distance = player.position().distanceTo(chase.position());
            if (distance > 3.0D) {
                Vec3 towards = chase.position().add(0.0D, chase.getBbHeight() * 0.5D, 0.0D)
                        .subtract(player.getEyePosition());
                if (towards.lengthSqr() > 1.0E-4D) {
                    velocity = towards.normalize().scale(Math.max(state.dashSpeed(), distance * 0.35D));
                }
            } else {
                CombatService.finishChase(player, chase);
                // End the dash immediately: the chase already connected.
                state.setDashTicks(0);
                return;
            }
        }

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.resetFallDistance();

        ServerLevel level = (ServerLevel) player.level();
        VfxDispatcher.playTracking(player, VfxEvent.SPARK, player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D),
                direction, 0.8F, VfxEvent.SPARK.defaultColor());

        if (config.impactDamage.get() > 0.0D) {
            damageAlongPath(player, level, direction);
        }
    }

    private static void damageAlongPath(ServerPlayer player, ServerLevel level, Vec3 direction) {
        Set<Integer> alreadyHit = HIT_THIS_DASH.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        double speed = state(player).dashSpeed();
        Vec3 from = player.position().subtract(direction.scale(speed));
        Vec3 to = player.position().add(direction.scale(speed * 0.5D));
        List<LivingEntity> hit = Targeting.entitiesAlongSegment(level, from, to, 1.1D, player,
                entity -> CombatService.isValidTarget(player, entity));
        for (LivingEntity entity : hit) {
            if (!alreadyHit.add(entity.getId())) {
                continue;
            }
            float damage = (float) (AnimeKiServerConfig.DASH.impactDamage.get()
                    * com.animeki.ki.KiMath.meleeMultiplier(player));
            entity.invulnerableTime = 0;
            if (entity.hurt(ModDamageTypes.of(level, ModDamageTypes.MARTIAL, player), damage)) {
                entity.push(direction.x * 1.1D, 0.25D, direction.z * 1.1D);
                entity.hurtMarked = true;
            }
            VfxDispatcher.impact(level, VfxEvent.DASH_IMPACT, entity.getBoundingBox().getCenter(), direction, 1.0F);
        }
    }

    private static FlightState state(ServerPlayer player) {
        return ModAttachments.flight(player);
    }

    public static boolean isDashing(ServerPlayer player) {
        return state(player).dashTicks() > 0;
    }

    /** Invulnerability frames granted by the dash, consumed by the damage hook. */
    public static boolean isInvulnerable(ServerPlayer player) {
        Long until = INVULNERABLE_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static double dashCooldownFraction(ServerPlayer player) {
        int remaining = AbilityService.cooldownRemaining(player, COOLDOWN_ID);
        int total = AnimeKiServerConfig.DASH.cooldownTicks.get();
        if (total <= 0) {
            return 0.0D;
        }
        return MathUtil.clamp(remaining / (double) total, 0.0D, 1.0D);
    }

    @Nullable
    public static Long invulnerableUntil(ServerPlayer player) {
        return INVULNERABLE_UNTIL.get(player.getUUID());
    }
}
