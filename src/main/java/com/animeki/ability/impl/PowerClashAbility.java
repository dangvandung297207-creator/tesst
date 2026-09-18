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
import com.animeki.util.MathUtil;
import com.animeki.vfx.CameraEffectType;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Power Clash.
 *
 * <p>Fires a wide, stable Ki field. When two compatible beams intersect, {@link BeamService} stops
 * them at the collision point, compares their output and pushes the clash point towards the weaker
 * one. This ability adds the physical feedback: the player is braced, movement is restricted and the
 * camera leans into the pressure.</p>
 */
public final class PowerClashAbility extends Ability {
    public static final String ID = "animeki:power_clash";

    private int beamId = -1;

    public PowerClashAbility() {
        super(ID, AbilityStats.builder()
                .kiCost(0.0D)
                .cooldownTicks(AnimeKiServerConfig.ABILITIES.beam.cooldownTicks.get())
                .chargeTicks(0)
                .range(AnimeKiServerConfig.ABILITIES.clash.burstRadius.get() * 6.0D)
                .damage(AnimeKiServerConfig.ABILITIES.clash.burstDamage.get())
                .castType(AbilityStats.CastType.CHANNEL)
                .build());
    }

    @Override
    public String animationId() {
        return "power_clash";
    }

    @Override
    public void onStart(ServerPlayer player, AbilityContext context) {
        if (!AnimeKiServerConfig.ABILITIES.clash.enabled.get()) {
            context.stop(AbilityStopReason.CANCELLED);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        AnimeKiServerConfig.Abilities.Beam beamConfig = AnimeKiServerConfig.ABILITIES.beam;
        BeamType base = BeamTypes.CLASH_FIELD;
        BeamType effective = new BeamType(base.id(), base.coreColor(), base.glowColor(), beamConfig.width.get() * 1.6D,
                beamConfig.damagePerTick.get(), beamConfig.kiCostPerTick.get() * AnimeKiServerConfig.ABILITIES.clash.drainMultiplier.get(),
                beamConfig.range.get() * 0.8D, base.clashWeight(), false);
        Vec3 origin = player.getEyePosition().add(context.direction().scale(0.7D));
        BeamInstance beam = BeamService.create(level, player, effective, origin, context.direction(), context.power(),
                AnimeKiServerConfig.ABILITIES.clash.maxDurationTicks.get());
        beamId = beam.id();
        VfxDispatcher.camera(player, CameraEffectType.FOV_PUNCH, 0.12F, 8);
    }

    @Override
    public void onTick(ServerPlayer player, AbilityContext context, ActiveAbility active) {
        ServerLevel level = (ServerLevel) player.level();
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

        if (!KiService.drain(player, beam.type().kiDrainPerTick())) {
            VfxDispatcher.playOnPlayer(player, VfxEvent.KI_BURNOUT, Vec3.ZERO, 1.2F);
            active.markFinished();
            return;
        }

        beam.setOrigin(player.getEyePosition().add(beam.direction().scale(0.7D)));
        Vec3 wanted = player.getViewVector(1.0F).normalize();
        if (beam.inClash()) {
            // Braced: no free steering while the beams wrestle, the clash itself pushes back.
            double progress = beam.clashProgress();
            Vec3 push = beam.direction().scale(-0.06D - Math.max(0.0D, progress) * 0.12D);
            player.setDeltaMovement(push.x, Math.max(-0.05D, player.getDeltaMovement().y * 0.4D), push.z);
            player.hurtMarked = true;
            if (active.ticks() % 5 == 0) {
                VfxDispatcher.camera(player, CameraEffectType.SHAKE, 0.6F + (float) Math.abs(progress) * 1.4F, 6);
                VfxDispatcher.camera(player, CameraEffectType.IMPULSE, (float) -progress * 0.4F, 6);
            }
        } else {
            beam.setDirection(MathUtil.lerp(beam.direction(), wanted, 0.15D));
        }
        if (active.ticks() >= AnimeKiServerConfig.ABILITIES.clash.maxDurationTicks.get()) {
            active.markFinished();
        }
    }

    @Override
    public void onStop(ServerPlayer player, AbilityContext context, AbilityStopReason reason) {
        if (beamId >= 0 && player.level() instanceof ServerLevel level) {
            BeamService.stop(level, beamId, true);
            beamId = -1;
        }
    }
}
