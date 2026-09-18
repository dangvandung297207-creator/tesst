package com.animeki.ability.impl;

import com.animeki.ability.Ability;
import com.animeki.ability.AbilityContext;
import com.animeki.ability.AbilityStats;
import com.animeki.ability.AbilityStopReason;
import com.animeki.ability.ActiveAbility;
import com.animeki.beam.BeamInstance;
import com.animeki.beam.BeamService;
import com.animeki.beam.BeamType;
import com.animeki.beam.BeamTypes;
import com.animeki.config.AnimeKiServerConfig;
import com.animeki.ki.KiService;
import com.animeki.registry.ModAttachments;
import com.animeki.registry.ModSounds;
import com.animeki.util.MathUtil;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Energy Beam.
 *
 * <p>Charge with the hands together, then hold to keep firing. The beam itself is a single
 * segment/ray object (see {@link BeamService}) - never a chain of entities - so the cost stays
 * constant no matter how long the beam is. The player can steer it while channeling and the beam
 * stops when the Ki runs out, when the ability is cancelled or when the duration cap is reached.</p>
 */
public final class EnergyBeamAbility extends Ability {
    public static final String ID = "animeki:energy_beam";

    private int beamId = -1;

    public EnergyBeamAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(0.0D)
                .cooldown(AnimeKiServerConfig.ABILITIES.beam.cooldownTicks.get())
                .chargeTicks(AnimeKiServerConfig.ABILITIES.beam.chargeTicks.get())
                .range(AnimeKiServerConfig.ABILITIES.beam.range.get())
                .damage(AnimeKiServerConfig.ABILITIES.beam.damagePerTick.get())
                .castType(AbilityStats.CastType.CHANNEL)
                .build());
    }

    @Override
    public String animationId() {
        return "energy_beam";
    }

    @Override
    public boolean cancelWhenReleasedEarly() {
        return true;
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BEAM_CHARGE.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void onChargeTick(ServerPlayer player, AbilityContext context, int chargeTicks) {
        int required = Math.max(1, stats().chargeTicks());
        float progress = Math.min(1.0F, chargeTicks / (float) required);
        if (chargeTicks % 3 == 0) {
            // Sphere between the hands.
            VfxDispatcher.playOnPlayer(player, VfxEvent.BEAM_CHARGE, new Vec3(0.0D, -0.15D, 0.0D), 0.5F + progress);
            // Rising aura tells everyone in the area that a beam is coming.
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_CHARGE_TICK, Vec3.ZERO, 0.6F + progress * 1.2F);
        }
        VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.06F + progress * 0.12F, 3);
    }

    @Override
    public void onRelease(ServerPlayer player, AbilityContext context, int chargeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.Beam config = AnimeKiServerConfig.ABILITIES.beam;
        BeamType base = BeamTypes.forTransformation(ModAttachments.transformation(player).activeId());
        // Numbers come from the config, the visual identity (colours, clash weight) from the beam type.
        BeamType effective = new BeamType(base.id(), base.coreColor(), base.glowColor(), config.width.get(),
                config.damagePerTick.get(), config.kiCostPerTick.get(), config.range.get(), base.clashWeight(),
                true);
        Vec3 origin = player.getEyePosition().add(context.direction().scale(0.8D));
        BeamInstance beam = BeamService.create(level, player, effective, origin, context.direction(),
                context.power(), config.maxDurationTicks.get());
        beamId = beam.id();
        VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.8F, 12);
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.22F, 10);
    }

    @Override
    public void onTick(ServerPlayer player, AbilityContext context, ActiveAbility active) {
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.Beam config = AnimeKiServerConfig.ABILITIES.beam;
        if (beamId < 0) {
            active.markFinished();
            return;
        }
        BeamInstance beam = BeamService.byId(level, beamId);
        if (beam == null) {
            beamId = -1;
            active.markFinished();
            return;
        }

        // Continuous Ki drain: the beam dies the moment the pool is empty.
        if (!KiService.drain(player, config.kiCostPerTick.get())) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 1.2F);
            active.markFinished();
            return;
        }

        // Steering: the aim follows the player's view, smoothed so it still feels like a beam.
        Vec3 wanted = player.getViewVector(1.0F).normalize();
        Vec3 current = beam.direction();
        Vec3 steered = MathUtil.lerp(current, wanted, 0.22D);
        if (steered.lengthSqr() > 1.0E-6D) {
            beam.setDirection(steered);
        }
        beam.setOrigin(player.getEyePosition().add(beam.direction().scale(0.8D)));

        if (active.ticks() % 8 == 0) {
            VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.25F, 4);
        }
        if (active.ticks() >= config.maxDurationTicks.get()) {
            active.markFinished();
        }
    }

    @Override
    public void onStop(ServerPlayer player, AbilityContext context, AbilityStopReason reason) {
        if (beamId >= 0 && player.level() instanceof ServerLevel level) {
            BeamService.stop(level, beamId, reason != AbilityStopReason.KI_DEPLETED);
            beamId = -1;
        }
    }
}
