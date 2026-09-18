package com.animeki.beam;

import com.animeki.config.AnimeKiServerConfig;
import com.animeki.destruction.DestructionProfile;
import com.animeki.destruction.DestructionService;
import com.animeki.ki.KiService;
import com.animeki.network.AnimeKiNetwork;
import com.animeki.network.Payloads;
import com.animeki.registry.ModDamageTypes;
import com.animeki.registry.ModSounds;
import com.animeki.util.MathUtil;
import com.animeki.util.Targeting;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server side beam simulation, collision and power clash resolution.
 *
 * <p>Design rules that keep this cheap on a busy server:</p>
 * <ul>
 *     <li>a beam is <b>one</b> object, never a chain of entities, so length costs nothing;</li>
 *     <li>block collision is a single clipped ray per beam per tick;</li>
 *     <li>entity collision is one bounding box query against the segment;</li>
 *     <li>damage is gated per entity with the configured interval;</li>
 *     <li>terrain erosion is probabilistic and goes through the budgeted destruction service;</li>
 *     <li>clients receive a small snapshot every other tick and interpolate in between.</li>
 * </ul>
 *
 * <p>Power clash: when two beams of different owners come within a fraction of their combined width,
 * both are cut short at the collision point, their outputs are compared and the clash point is pushed
 * towards the weaker fighter. Losing fighters have their beam burst into an explosion; equal fighters
 * keep wrestling until one runs out of Ki or the duration cap is hit.</p>
 */
public final class BeamService {
    /** Beams are synced every other tick; the client interpolates to keep the beam smooth. */
    private static final int SYNC_INTERVAL = 2;
    private static final int EFFECT_INTERVAL = 2;
    /** Distance at which two beams are considered to touch, as a fraction of the combined widths. */
    private static final double CLASH_WIDTH_FACTOR = 0.85D;

    private static final Map<ResourceKey<Level>, Map<Integer, BeamInstance>> BEAMS = new HashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private BeamService() {
    }

    // ------------------------------------------------------------------ lifecycle

    public static BeamInstance create(ServerLevel level, Entity owner, BeamType type, Vec3 origin, Vec3 direction,
                                      double damageScale, int maxDurationTicks) {
        BeamTypes.bootstrap();
        Map<Integer, BeamInstance> beams = BEAMS.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        BeamInstance beam = new BeamInstance(NEXT_ID.getAndIncrement(), owner, type, origin, direction, damageScale,
                maxDurationTicks, level.getGameTime());
        beams.put(beam.id(), beam);
        level.playSound(null, origin.x, origin.y, origin.z, ModSounds.BEAM_FIRE.get(), SoundSource.PLAYERS, 1.6F, 1.0F);
        VfxDispatcher.play(level, VfxEvent.BEAM_FIRE, origin, direction, (float) type.width() * 1.6F, type.coreColor());
        syncNow(level);
        return beam;
    }

    @Nullable
    public static BeamInstance byId(ServerLevel level, int beamId) {
        Map<Integer, BeamInstance> beams = BEAMS.get(level.dimension());
        return beams == null ? null : beams.get(beamId);
    }

    public static List<BeamInstance> beams(ServerLevel level) {
        Map<Integer, BeamInstance> beams = BEAMS.get(level.dimension());
        if (beams == null || beams.isEmpty()) {
            return List.of();
        }
        List<BeamInstance> active = new ArrayList<>(beams.size());
        for (BeamInstance beam : beams.values()) {
            if (!beam.stopped()) {
                active.add(beam);
            }
        }
        return active;
    }

    /** Ends one beam, optionally with an impact burst at its tip. */
    public static void stop(ServerLevel level, int beamId, boolean burst) {
        Map<Integer, BeamInstance> beams = BEAMS.get(level.dimension());
        if (beams == null) {
            return;
        }
        BeamInstance beam = beams.remove(beamId);
        if (beam == null) {
            return;
        }
        beam.markStopped();
        if (beam.inClash()) {
            // Never leave the partner pointing at a dead beam.
            BeamInstance partner = beams.get(beam.clashPartnerId());
            if (partner != null) {
                partner.setClash(null);
            }
            beam.setClash(null);
        }
        if (burst) {
            Vec3 tip = beam.origin().add(beam.direction().scale(beam.length()));
            VfxDispatcher.impact(level, VfxEvent.BEAM_IMPACT, tip, beam.direction(), 1.3F);
            level.playSound(null, tip.x, tip.y, tip.z, ModSounds.BEAM_IMPACT.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
        }
        syncNow(level);
    }

    /** Ends every beam owned by the given entity (used when a channel is cancelled). */
    public static void stopOwner(ServerLevel level, Entity owner, boolean burst) {
        for (BeamInstance beam : beams(level)) {
            if (beam.ownerId() == owner.getId()) {
                stop(level, beam.id(), burst);
            }
        }
    }

    /** Cleans up when a level unloads. */
    public static void stopLevel(ServerLevel level) {
        BEAMS.remove(level.dimension());
    }

    // ------------------------------------------------------------------ simulation

    public static void tick(ServerLevel level) {
        Map<Integer, BeamInstance> beams = BEAMS.get(level.dimension());
        if (beams == null || beams.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        beams.values().removeIf(BeamInstance::stopped);

        List<BeamInstance> active = new ArrayList<>(beams.values());
        for (BeamInstance beam : active) {
            tickBeam(level, beam, gameTime);
        }
        active = beams(level);
        resolveClashes(level, active, gameTime);

        if (gameTime % SYNC_INTERVAL == 0) {
            syncNow(level);
        }
    }

    private static void tickBeam(ServerLevel level, BeamInstance beam, long gameTime) {
        Entity owner = beam.owner(level);
        if (owner == null || !owner.isAlive()) {
            stop(level, beam.id(), true);
            return;
        }
        beam.refreshOriginFromOwner(level);

        if (beam.expired(gameTime)) {
            stop(level, beam.id(), true);
            return;
        }

        // 1. Terrain: a single clipped ray decides where the beam stops.
        Vec3 from = beam.origin();
        Vec3 to = from.add(beam.direction().scale(beam.type().range()));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                owner));
        boolean blocked = hit.getType() != HitResult.Type.MISS;
        double length = blocked ? from.distanceTo(hit.getLocation()) : beam.type().range();
        if (beam.inClash()) {
            // The clash point is a hard stop so nothing leaks through during a struggle.
            length = Math.min(length, Math.max(1.0D, from.distanceTo(beam.clashPoint())));
        }
        beam.setLength(length);
        Vec3 end = beam.end();

        // 2. Entities along the segment, damage gated per entity.
        double damageRadius = beam.type().width() + 0.4D;
        int interval = Math.max(1, AnimeKiServerConfig.ABILITIES.beam.entityDamageInterval.get());
        for (LivingEntity target : Targeting.entitiesAlongSegment(level, from, end, damageRadius, owner,
                entity -> entity.isAlive() && !entity.isSpectator())) {
            if (!beam.canDamage(target, gameTime, interval)) {
                continue;
            }
            float damage = (float) (beam.type().damagePerTick() * beam.damageScale());
            target.invulnerableTime = 0;
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.BEAM, owner, owner), damage)) {
                Vec3 push = beam.direction().scale(AnimeKiServerConfig.ABILITIES.beam.knockback.get());
                target.push(push.x, 0.08D, push.z);
                target.hurtMarked = true;
            }
            VfxDispatcher.play(level, VfxEvent.BEAM_IMPACT, target.getBoundingBox().getCenter(), beam.direction(),
                    (float) damageRadius, beam.type().coreColor());
            if (owner instanceof ServerPlayer fighter) {
                VfxDispatcher.camera(fighter, CameraEffectType.SHAKE, 0.2F, 4);
            }
        }

        // 3. Terrain erosion at the impact point.
        if (blocked && beam.type().destructive() && AnimeKiServerConfig.DESTRUCTION.enabled.get()) {
            if (level.getRandom().nextDouble() < AnimeKiServerConfig.ABILITIES.beam.blockBreakChance.get()) {
                DestructionService.apply(level, hit.getLocation(), DestructionProfile.beamErosion(1.4D), owner, null);
            }
            if (gameTime % EFFECT_INTERVAL == 0) {
                VfxDispatcher.play(level, VfxEvent.BEAM_IMPACT, hit.getLocation().add(beam.direction().scale(-0.3D)),
                        beam.direction(), (float) damageRadius * 1.2F, beam.type().coreColor());
            }
        }
    }

    // ------------------------------------------------------------------ clash

    private static void resolveClashes(ServerLevel level, List<BeamInstance> beams, long gameTime) {
        AnimeKiServerConfig.Abilities.Clash config = AnimeKiServerConfig.ABILITIES.clash;
        if (!config.enabled.get() || beams.size() < 2) {
            if (!beams.isEmpty()) {
                for (BeamInstance beam : beams) {
                    if (beam.inClash()) {
                        beam.setClash(null);
                    }
                }
            }
            return;
        }
        for (int i = 0; i < beams.size(); i++) {
            BeamInstance a = beams.get(i);
            for (int j = i + 1; j < beams.size(); j++) {
                BeamInstance b = beams.get(j);
                if (a.ownerId() == b.ownerId()) {
                    continue;
                }
                double[] result = Targeting.segmentSegmentDistance(a.origin(), a.end(), b.origin(), b.end());
                double threshold = (a.type().width() + b.type().width()) * CLASH_WIDTH_FACTOR;
                if (result[0] > threshold) {
                    if (a.clashPartnerId() == b.id()) {
                        a.setClash(null);
                        b.setClash(null);
                    }
                    continue;
                }
                engageClash(level, a, b, result, gameTime, config);
            }
        }
    }

    private static void engageClash(ServerLevel level, BeamInstance a, BeamInstance b, double[] result,
                                    long gameTime, AnimeKiServerConfig.Abilities.Clash config) {
        double lengthA = a.origin().distanceTo(a.end());
        double lengthB = b.origin().distanceTo(b.end());
        Vec3 pointA = a.origin().add(a.direction().scale(lengthA * result[1]));
        Vec3 pointB = b.origin().add(b.direction().scale(lengthB * result[2]));
        Vec3 clashPoint = pointA.add(pointB).scale(0.5D);

        boolean fresh = a.clashPartnerId() != b.id();
        a.setClash(b);
        b.setClash(a);
        a.setClashPoint(clashPoint);
        b.setClashPoint(clashPoint);
        a.tickClash();
        b.tickClash();

        if (fresh) {
            level.playSound(null, clashPoint.x, clashPoint.y, clashPoint.z, ModSounds.CLASH_LOOP.get(),
                    SoundSource.PLAYERS, 1.8F, 1.0F);
            VfxDispatcher.play(level, VfxEvent.CLASH_BURST, clashPoint, a.direction(), 1.2F, 0xFFFFFFFF);
            VfxDispatcher.cameraNear(level, clashPoint, 48.0D, CameraEffectType.SHAKE, 0.8F, 12, a.id());
        }

        // Output comparison decides who is winning.
        double totalPower = Math.max(1.0E-4D, a.power() + b.power());
        double advantage = (b.power() - a.power()) / totalPower;
        double drift = (level.getRandom().nextDouble() - 0.5D) * config.clashPointDrift.get();
        double push = config.pushRate.get() * advantage + drift;
        // Positive progress means "the clash point is travelling towards my emitter", i.e. losing.
        a.setClashProgress(a.clashProgress() + push);
        b.setClashProgress(b.clashProgress() - push);

        drainClashKi(level, a, config);
        drainClashKi(level, b, config);

        if (gameTime % EFFECT_INTERVAL == 0) {
            float intensity = (float) Math.min(2.6D, 0.7D + totalPower * 0.12D);
            double distance = result[0];
            Vec3 spark = clashPoint.add(new Vec3(
                    (level.getRandom().nextDouble() - 0.5D) * distance * 2.0D,
                    (level.getRandom().nextDouble() - 0.5D) * distance * 2.0D,
                    (level.getRandom().nextDouble() - 0.5D) * distance * 2.0D));
            VfxDispatcher.play(level, VfxEvent.CLASH_SPARK, spark, a.direction(), intensity, 0xFFFFFFFF);
            VfxDispatcher.cameraNear(level, clashPoint, 56.0D, CameraEffectType.SHAKE, 0.4F * intensity, 5, (int) gameTime);
        }

        boolean aLoses = a.clashProgress() >= 0.98D;
        boolean bLoses = b.clashProgress() <= -0.98D;
        boolean timedOut = a.clashTicks() > config.maxDurationTicks.get();
        if (aLoses || timedOut) {
            resolveClash(level, b, a, clashPoint, config);
        } else if (bLoses) {
            resolveClash(level, a, b, clashPoint, config);
        }
    }

    private static void drainClashKi(ServerLevel level, BeamInstance beam, AnimeKiServerConfig.Abilities.Clash config) {
        if (beam.owner(level) instanceof ServerPlayer player) {
            double drain = beam.type().kiDrainPerTick() * config.drainMultiplier.get();
            if (!KiService.drain(player, drain)) {
                beam.setClashProgress(1.0D);
            }
        }
    }

    /** One beam wins: the loser bursts, the winner keeps firing. */
    private static void resolveClash(ServerLevel level, @Nullable BeamInstance winner, BeamInstance loser,
                                     Vec3 clashPoint, AnimeKiServerConfig.Abilities.Clash config) {
        level.playSound(null, clashPoint.x, clashPoint.y, clashPoint.z, ModSounds.CLASH_BURST.get(),
                SoundSource.PLAYERS, 2.2F, 1.0F);
        VfxDispatcher.play(level, VfxEvent.CLASH_BURST, clashPoint, null, 2.2F, 0xFFFFFFFF);
        VfxDispatcher.play(level, VfxEvent.DEBRIS, clashPoint, null, 3.0F, 0x8C8C8C);
        VfxDispatcher.cameraNear(level, clashPoint, 64.0D, CameraEffectType.SHAKE, 1.8F, 30, clashPoint.hashCode());
        VfxDispatcher.cameraNear(level, clashPoint, 64.0D, CameraEffectType.FLASH, 0.5F, 8, clashPoint.hashCode());

        double radius = config.burstRadius.get();
        double damage = config.burstDamage.get();
        Entity attacker = winner == null ? null : winner.owner(level);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(clashPoint, clashPoint).inflate(radius))) {
            double distance = entity.position().distanceTo(clashPoint);
            if (distance > radius) {
                continue;
            }
            float scaled = (float) (damage * (1.0D - distance / Math.max(1.0E-4D, radius * 1.15D)));
            if (scaled <= 0.0F) {
                continue;
            }
            entity.invulnerableTime = 0;
            entity.hurt(ModDamageTypes.source(level, ModDamageTypes.CLASH, attacker, attacker), scaled);
            Vec3 push = entity.position().subtract(clashPoint);
            if (push.lengthSqr() < 1.0E-4D) {
                push = new Vec3(0.0D, 1.0D, 0.0D);
            }
            push = push.normalize().scale(1.3D);
            entity.push(push.x, 0.5D, push.z);
            entity.hurtMarked = true;
        }

        stop(level, loser.id(), false);
        if (winner != null) {
            winner.setClash(null);
            winner.setClashProgress(0.0D);
            // The winning beam punches through with a shockwave at its tip.
            Vec3 tip = winner.origin().add(winner.direction().scale(winner.length()));
            VfxDispatcher.impact(level, VfxEvent.BEAM_IMPACT, tip, winner.direction(), 1.8F);
        }
    }

    // ------------------------------------------------------------------ sync

    private static void syncNow(ServerLevel level) {
        List<BeamInstance> active = beams(level);
        if (active.isEmpty()) {
            return;
        }
        List<Payloads.BeamSync.BeamState> states = new ArrayList<>(active.size());
        for (BeamInstance beam : active) {
            Vec3 clash = beam.clashPoint();
            states.add(new Payloads.BeamSync.BeamState(beam.id(), beam.ownerId(), beam.type().id(),
                    beam.origin().x, beam.origin().y, beam.origin().z,
                    (float) beam.direction().x, (float) beam.direction().y, (float) beam.direction().z,
                    (float) beam.length(), (float) beam.type().width(), beam.type().coreColor(),
                    beam.type().glowColor(), (float) beam.power(), (float) beam.clashProgress(),
                    clash.x, clash.y, clash.z, beam.clashPartnerId()));
        }
        Payloads.BeamSync payload = new Payloads.BeamSync(states);
        for (BeamInstance beam : active) {
            double radius = Math.max(64.0D, beam.length() + 32.0D);
            AnimeKiNetwork.sendToNear(level, beam.origin(), radius, payload);
        }
    }

    /** Smoothing helper used by the client renderer (must match the interpolation it performs). */
    public static double interpolate(double previous, double current, float partialTick) {
        return previous + (current - previous) * MathUtil.clamp(partialTick, 0.0F, 1.0F);
    }
}
