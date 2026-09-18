package com.animeki.client.vfx;

import com.animeki.client.ClientState;
import com.animeki.config.AnimeKiClientConfig;
import com.animeki.network.Payloads;
import com.animeki.registry.ModSounds;
import com.animeki.vfx.VfxEvent;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Client side effect spawning.
 *
 * <p>Two layers cooperate so the visuals stay cheap:</p>
 * <ol>
 *     <li>a handful of vanilla particles for texture and motion, and</li>
 *     <li>custom geometry queued in {@link ClientState} and drawn by the world renderer
 *         (rings, shockwaves, spheres, beams).</li>
 * </ol>
 *
 * <p>Every count is capped and every particle is only spawned if it is close enough to matter, so
 * large fights do not turn into a particle storm.</p>
 */
public final class ClientVfx {
    /** Effects further away than this are dropped instead of being drawn. */
    private static final double MAX_DISTANCE_SQUARED = 144.0D * 144.0D;
    private static final int MAX_ACTIVE = 96;

    private static final RandomSource RANDOM = RandomSource.create();

    private ClientVfx() {
    }

    public static void spawn(Payloads.Vfx payload) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        double distanceSquared = 0.0D;
        if (minecraft.player != null) {
            distanceSquared = minecraft.player.distanceToSqr(payload.x(), payload.y(), payload.z());
        }
        if (distanceSquared > MAX_DISTANCE_SQUARED) {
            return;
        }
        VfxEvent event = VfxEvent.byOrdinal(payload.event());
        if (ClientState.ACTIVE_VFX.size() < MAX_ACTIVE && payload.duration() > 0) {
            ClientState.ACTIVE_VFX.add(new ClientState.ActiveVfx(event, payload.x(), payload.y(), payload.z(),
                    payload.dx(), payload.dy(), payload.dz(), payload.scale(), payload.color(), payload.duration(),
                    payload.entityId()));
        }
        if (AnimeKiClientConfig.VFX.vanillaParticles.get()) {
            spawnParticles(level, event, payload);
        }
    }

    private static void spawnParticles(Level level, VfxEvent event, Payloads.Vfx payload) {
        float intensity = AnimeKiClientConfig.VFX.intensity.get().floatValue();
        if (intensity <= 0.0F) {
            return;
        }
        double x = payload.x();
        double y = payload.y();
        double z = payload.z();
        double vx = payload.dx() * 0.15D;
        double vy = payload.dy() * 0.15D;
        double vz = payload.dz() * 0.15D;
        int count = Math.max(1, (int) Math.min(14.0F, 2.0F + payload.scale() * 3.0F * intensity));
        switch (event) {
            case LIGHT_HIT, COMBO_HIT -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.CRIT, x, y, z, spread(), spread(), spread());
                }
            }
            case HEAVY_HIT, KI_BLAST_IMPACT -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.CRIT, x, y, z, spread(), spread() + 0.1D, spread());
                    level.addParticle(ParticleTypes.FIREWORK, x, y, z, spread(), spread(), spread());
                }
            }
            case GROUND_IMPACT, TRANSFORMATION_SHOCKWAVE, ULTIMATE_IMPACT, CLASH_BURST -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.EXPLOSION, x + spread() * 2.0D, y, z + spread() * 2.0D,
                            0.0D, 0.05D, 0.0D);
                }
                // Vanilla's flash particle stands in for a real light source: cinematic events light the
                // area up without a single block update or dynamic light engine hook.
                level.addParticle(ParticleTypes.FLASH, x, y + 1.0D, z, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < count * 2; i++) {
                    level.addParticle(ParticleTypes.CLOUD, x + spread() * 2.5D, y, z + spread() * 2.5D,
                            spread() * 0.4D, 0.08D, spread() * 0.4D);
                }
            }
            case DEBRIS -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, x + spread() * 2.0D, y + 0.2D,
                            z + spread() * 2.0D, spread() * 0.2D, 0.12D, spread() * 0.2D);
                }
            }
            case DASH_IMPACT, SONIC_BOOM -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.CLOUD, x, y, z, spread() * 0.3D, spread() * 0.3D, spread() * 0.3D);
                }
                if (event == VfxEvent.SONIC_BOOM) {
                    level.addParticle(ParticleTypes.SONIC_BOOM, x, y, z, 0.0D, 0.0D, 0.0D);
                }
            }
            case KI_CHARGE_TICK, CHARGE_READY, BEAM_CHARGE, ULTIMATE_CHARGE, AURA_BURST -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.END_ROD, x + spread() * 0.8D, y + RANDOM.nextDouble() * 2.0D,
                            z + spread() * 0.8D, 0.0D, 0.02D + RANDOM.nextDouble() * 0.03D, 0.0D);
                }
            }
            case KI_BLAST_FIRE, BEAM_FIRE -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, spread(), spread(), spread());
                }
            }
            case BEAM_IMPACT -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.FLAME, x, y, z, spread() * 0.5D, spread() * 0.5D, spread() * 0.5D);
                }
            }
            case CLASH_SPARK -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + spread(), y + spread(), z + spread(),
                            vx, vy, vz);
                }
            }
            case VANISH, APPEAR -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.POOF, x + spread() * 0.6D, y + RANDOM.nextDouble() * 1.6D,
                            z + spread() * 0.6D, 0.0D, 0.02D, 0.0D);
                }
            }
            case KI_BURNOUT -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.SMOKE, x + spread(), y + spread(), z + spread(),
                            0.0D, 0.05D, 0.0D);
                }
            }
            case SPARK, FLIGHT_BOOST -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.END_ROD, x + spread() * 0.4D, y + spread() * 0.4D,
                            z + spread() * 0.4D, -vx, -vy, -vz);
                }
            }
            case TARGET_MARKER -> {
                for (int i = 0; i < count; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
                    level.addParticle(ParticleTypes.END_ROD, x + Math.cos(angle) * payload.scale(), y + 0.2D,
                            z + Math.sin(angle) * payload.scale(), 0.0D, 0.02D, 0.0D);
                }
            }
            case BOSS_ROAR, BOSS_PHASE -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + spread() * 1.5D, y + spread(),
                            z + spread() * 1.5D, 0.0D, 0.05D, 0.0D);
                }
                level.addParticle(ParticleTypes.FLASH, x, y + 2.0D, z, 0.0D, 0.0D, 0.0D);
            }
            default -> {
                for (int i = 0; i < count; i++) {
                    level.addParticle(ParticleTypes.END_ROD, x, y, z, spread(), spread(), spread());
                }
            }
        }
    }

    private static double spread() {
        return (RANDOM.nextDouble() - 0.5D) * 0.6D;
    }

    /** Ages the queued geometry and drops anything that finished or moved out of range. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            ClientState.ACTIVE_VFX.clear();
            return;
        }
        double maxDistance = MAX_DISTANCE_SQUARED;
        Iterator<ClientState.ActiveVfx> iterator = ClientState.ACTIVE_VFX.iterator();
        while (iterator.hasNext()) {
            ClientState.ActiveVfx vfx = iterator.next();
            vfx.age++;
            if (vfx.finished()
                    || minecraft.player.distanceToSqr(vfx.x, vfx.y, vfx.z) > maxDistance) {
                iterator.remove();
            }
        }
    }

    /** Quiet "that did not work" ping played when the server refuses an action. */
    public static void feedbackPing() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSoundManager() == null) {
            return;
        }
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.KI_BLAST_IMPACT.get(), 1.6F));
    }

    /** Convenience for client code that wants a particle with no packet involved. */
    public static void local(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            level.addParticle(options, x, y, z, vx, vy, vz);
        }
    }
}
