package com.animeki.flight;

import com.animeki.config.AnimeKiServerConfig;
import com.animeki.ki.KiMath;
import com.animeki.ki.KiService;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.network.Payloads;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModSounds;
import com.animeki.transformation.TransformationService;
import com.animeki.util.MathUtil;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ki powered survival flight.
 *
 * <p>Design notes: the server owns the state (Ki drain, lockouts, availability, validation) and the
 * client integrates the same velocity model locally for a smooth feel - the parameters are sent to
 * the client by the server, so prediction cannot drift from the authoritative numbers. Flight never
 * touches vanilla creative flight; it is an independent mechanic that simply replaces the player's
 * motion while active.</p>
 */
public final class FlightService {
    private FlightService() {
    }

    public static FlightState get(net.minecraft.world.entity.player.Player player) {
        return ModAttachments.flight(player);
    }

    public static boolean canFly(ServerPlayer player) {
        AnimeKiServerConfig.Flight config = AnimeKiServerConfig.FLIGHT;
        if (!config.enabled.get()) {
            return false;
        }
        if (config.requireTransformation.get() && !com.animeki.registry.ModAttachments.transformation(player).isActive()) {
            return false;
        }
        return player.isAlive() && !player.isSpectator();
    }

    public static boolean toggle(ServerPlayer player) {
        FlightState state = get(player);
        if (state.isActive()) {
            setActive(player, false, true);
            return false;
        }
        if (!canFly(player)) {
            return false;
        }
        long gameTime = player.level().getGameTime();
        if (state.isDisabled(gameTime)) {
            return false;
        }
        setActive(player, true, true);
        return true;
    }

    public static void setActive(ServerPlayer player, boolean active, boolean effects) {
        FlightState state = get(player);
        if (state.isActive() == active) {
            return;
        }
        state.setActive(active);
        player.setNoGravity(active);
        player.resetFallDistance();
        state.clearInput();
        if (active) {
            state.setVelocity(player.getDeltaMovement());
            sendFlightParams(player);
            if (effects) {
                VfxDispatcher.playOnPlayer(player, VfxEvent.AURA_BURST, Vec3.ZERO, 0.7F);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLIGHT_LOOP.get(),
                        SoundSource.PLAYERS, 1.0F, 1.2F);
                VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.08F, 6);
            }
        } else {
            state.setBoost(false);
            if (effects) {
                VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, -0.05F, 6);
            }
        }
        AnimeKiNetwork.syncPlayerState(player);
        AnimeKiNetwork.syncVisualState(player);
    }

    public static void sendFlightParams(ServerPlayer player) {
        AnimeKiServerConfig.Flight config = AnimeKiServerConfig.FLIGHT;
        AnimeKiNetwork.sendToPlayer(player, new Payloads.FlightParams(
                config.speed.get() * KiMath.flightMultiplier(player),
                config.boostMultiplier.get(),
                config.verticalSpeed.get() * KiMath.flightMultiplier(player),
                config.acceleration.get(),
                config.drag.get(),
                config.maxSpeed.get(),
                config.enabled.get()));
    }

    /** Client input, already validated on the server before it is applied. */
    public static void handleInput(ServerPlayer player, float forward, float strafe, float vertical, boolean boost) {
        FlightState state = get(player);
        if (!state.isActive()) {
            return;
        }
        float clampedForward = MathUtil.clamp(forward, -1.0F, 1.0F);
        float clampedStrafe = MathUtil.clamp(strafe, -1.0F, 1.0F);
        float clampedVertical = MathUtil.clamp(vertical, -1.0F, 1.0F);
        state.setInput(clampedForward, clampedStrafe, clampedVertical, player.level().getGameTime());
        state.setBoost(boost);
    }

    public static void tick(ServerPlayer player) {
        FlightState state = get(player);
        long gameTime = player.level().getGameTime();

        if (state.isActive()) {
            if (!canFly(player) || state.isDisabled(gameTime) || !player.isAlive()) {
                setActive(player, false, true);
                return;
            }
            AnimeKiServerConfig.Flight config = AnimeKiServerConfig.FLIGHT;
            double drain = config.kiDrainPerTick.get() + (state.isBoosting() ? config.boostKiDrainPerTick.get() : 0.0D);
            if (!KiService.drain(player, drain)) {
                VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 1.0F);
                setActive(player, false, true);
                return;
            }
            state.setTicks(state.ticks() + 1);
            integrate(player, state);
            if (state.ticks() % 4 == 0) {
                emitTrail(player, state);
            }
            return;
        }

        // Recharge air dashes while grounded and keep the velocity field in sync when idle.
        if (player.onGround()) {
            int max = AnimeKiServerConfig.DASH.airDashCharges.get();
            if (state.airDashCharges() < max) {
                state.setAirDashCharges(max);
            }
            if (state.isActive()) {
                return;
            }
            if (player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) {
                return;
            }
        }
    }

    /** Shared velocity model, mirrored by the client for prediction. */
    private static void integrate(ServerPlayer player, FlightState state) {
        AnimeKiServerConfig.Flight config = AnimeKiServerConfig.FLIGHT;
        double speed = config.speed.get() * KiMath.flightMultiplier(player);
        double verticalSpeed = config.verticalSpeed.get() * KiMath.flightMultiplier(player);
        if (state.isBoosting()) {
            speed *= config.boostMultiplier.get();
        }

        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));

        Vec3 wanted = forward.scale(state.forwardInput() * speed)
                .add(right.scale(state.strafeInput() * speed))
                .add(0.0D, state.inputVertical() * verticalSpeed, 0.0D);

        Vec3 current = state.velocity();
        double acceleration = config.acceleration.get();
        Vec3 velocity;
        if (wanted.lengthSqr() < 1.0E-6D) {
            velocity = current.scale(config.drag.get());
        } else {
            velocity = MathUtil.approach(current, wanted, acceleration * Math.max(1.0D, wanted.length()));
        }
        if (velocity.length() > config.maxSpeed.get()) {
            velocity = velocity.normalize().scale(config.maxSpeed.get());
        }

        velocity = avoidTerrain(player, velocity);
        state.setVelocity(velocity);
        player.setDeltaMovement(velocity);
        player.fallDistance = 0.0F;
        player.hurtMarked = true;
    }

    /** Stops the player from flying through walls: clips the intended motion and damps the axis. */
    private static Vec3 avoidTerrain(ServerPlayer player, Vec3 velocity) {
        if (velocity.lengthSqr() < 1.0E-6D) {
            return velocity;
        }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(velocity.scale(1.35D));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return velocity;
        }
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        double dot = velocity.normalize().dot(normal);
        if (dot >= 0.0D) {
            return velocity;
        }
        return velocity.subtract(normal.scale(velocity.dot(normal) * 1.05D)).scale(0.6D);
    }

    private static void emitTrail(ServerPlayer player, FlightState state) {
        double speed = state.velocity().length();
        if (speed < 0.25D) {
            return;
        }
        VfxDispatcher.playTracking(player, VfxEvent.FLIGHT_BOOST,
                player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D), state.velocity(),
                (float) Math.min(1.6D, speed), VfxEvent.FLIGHT_BOOST.defaultColor());
        if (state.isBoosting()) {
            VfxDispatcher.camera(player, CameraEffectType.ZOOM, 0.10F, 8);
        }
    }

    /** Taking damage knocks the player out of the sky for a short while. */
    public static void onDamage(ServerPlayer player, float amount) {
        FlightState state = get(player);
        if (!state.isActive()) {
            return;
        }
        int lockout = AnimeKiServerConfig.FLIGHT.disableOnDamageTicks.get();
        if (lockout <= 0) {
            return;
        }
        state.disableUntil(player.level().getGameTime() + lockout);
        setActive(player, false, true);
    }

    public static void sync(ServerPlayer player) {
        sendFlightParams(player);
        AnimeKiNetwork.syncVisualState(player);
    }

    /** Utility used by other systems (dash, vanish) to interrupt flight cleanly. */
    public static void stopIfActive(ServerPlayer player) {
        if (get(player).isActive()) {
            setActive(player, false, true);
        }
    }

    public static boolean isFlying(ServerPlayer player) {
        return get(player).isActive() && !TransformationService.get(player).phase().locksMovement();
    }
}
