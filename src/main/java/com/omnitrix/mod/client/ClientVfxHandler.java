package com.omnitrix.mod.client;

import com.omnitrix.mod.OmnitrixMod;
import com.omnitrix.mod.network.payload.OmnitrixVfxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = OmnitrixMod.MODID, value = Dist.CLIENT)
public class ClientVfxHandler {

    public static class DynamicVfx {
        public OmnitrixVfxPayload.VfxType type;
        public Vec3 pos;
        public int color;
        public int duration;
        public int maxDuration;

        public DynamicVfx(OmnitrixVfxPayload.VfxType type, Vec3 pos, int color, int duration) {
            this.type = type;
            this.pos = pos;
            this.color = color;
            this.duration = duration;
            this.maxDuration = duration;
        }
    }

    private static final List<DynamicVfx> activeEffects = new ArrayList<>();

    public static void handleVfx(OmnitrixVfxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Entity entity = level.getEntity(payload.entityId());
        Vec3 pos = (entity != null) ? entity.position() : Vec3.ZERO;

        switch (payload.vfxType()) {
            case FLASH_SHOCKWAVE -> spawnShockwave(level, pos, payload.color(), payload.extraData());
            case TRANSFORM_SPIRAL -> spawnTransformSpiral(level, pos, payload.color());
            case REVERSION_RED -> spawnReversionDust(level, pos);
            case SPEED_AFTERIMAGE -> spawnSpeedAfterimages(level, pos, payload.color());
            case GROUND_SLAM_DUST -> spawnGroundSlamDust(level, pos, payload.extraData());
            case FIRE_SLAM_BURST -> spawnFireSlamBurst(level, pos);
            case CRYSTAL_WALL_SUMMON -> spawnCrystalSparks(level, pos);
            case SONAR_RADAR_PULSE -> spawnSonarWave(level, pos);
            case TECH_GLOW_SURGE -> spawnTechGlow(level, pos);
            case SLIME_SPLATTER -> spawnSlimeSplatter(level, pos);
        }

        activeEffects.add(new DynamicVfx(payload.vfxType(), pos, payload.color(), 20));
    }

    private static void spawnShockwave(ClientLevel level, Vec3 pos, int color, int radius) {
        int r = (radius > 0) ? radius : 4;
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double dx = Math.cos(angle) * r * 0.4;
            double dz = Math.sin(angle) * r * 0.4;
            level.addParticle(ParticleTypes.EXPLOSION, pos.x + dx, pos.y + 0.2, pos.z + dz, dx * 0.2, 0.05, dz * 0.2);
            level.addParticle(ParticleTypes.FLASH, pos.x, pos.y + 1.0, pos.z, 0, 0, 0);
        }
    }

    private static void spawnTransformSpiral(ClientLevel level, Vec3 pos, int color) {
        for (int i = 0; i < 60; i++) {
            double progress = i / 60.0;
            double angle = progress * Math.PI * 6.0;
            double radius = 1.2 * (1.0 - progress * 0.4);
            double y = progress * 2.2;
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x + dx, pos.y + y, pos.z + dz, 0, 0.05, 0);
            level.addParticle(ParticleTypes.INSTANT_EFFECT, pos.x + dx, pos.y + y, pos.z + dz, 0, 0.02, 0);
        }
    }

    private static void spawnReversionDust(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 40; i++) {
            double dx = (Math.random() - 0.5) * 1.5;
            double dy = Math.random() * 2.0;
            double dz = (Math.random() - 0.5) * 1.5;
            level.addParticle(ParticleTypes.SMOKE, pos.x + dx, pos.y + dy, pos.z + dz, dx * 0.1, 0.1, dz * 0.1);
            level.addParticle(ParticleTypes.ANGRY_VILLAGER, pos.x + dx, pos.y + dy, pos.z + dz, 0, 0, 0);
        }
    }

    private static void spawnSpeedAfterimages(ClientLevel level, Vec3 pos, int color) {
        for (int i = 0; i < 15; i++) {
            double dx = (Math.random() - 0.5) * 0.8;
            double dy = Math.random() * 1.8;
            double dz = (Math.random() - 0.5) * 0.8;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x + dx, pos.y + dy, pos.z + dz, 0, 0.02, 0);
        }
    }

    private static void spawnGroundSlamDust(ClientLevel level, Vec3 pos, int radius) {
        int r = (radius > 0) ? radius : 5;
        for (int i = 0; i < 45; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = Math.random() * r;
            double dx = Math.cos(angle) * dist;
            double dz = Math.sin(angle) * dist;
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x + dx, pos.y + 0.1, pos.z + dz, 0, 0.15, 0);
            level.addParticle(ParticleTypes.POOF, pos.x + dx, pos.y + 0.1, pos.z + dz, dx * 0.05, 0.1, dz * 0.05);
        }
    }

    private static void spawnFireSlamBurst(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 30; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = Math.random() * 3.5;
            double dx = Math.cos(angle) * dist;
            double dz = Math.sin(angle) * dist;
            level.addParticle(ParticleTypes.FLAME, pos.x + dx, pos.y + 0.1, pos.z + dz, dx * 0.08, 0.12, dz * 0.08);
            level.addParticle(ParticleTypes.LAVA, pos.x + dx, pos.y + 0.1, pos.z + dz, 0, 0, 0);
        }
    }

    private static void spawnCrystalSparks(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 25; i++) {
            double dx = (Math.random() - 0.5) * 2.5;
            double dy = Math.random() * 2.5;
            double dz = (Math.random() - 0.5) * 2.5;
            level.addParticle(ParticleTypes.ENCHANTED_HIT, pos.x + dx, pos.y + dy, pos.z + dz, 0, 0.05, 0);
        }
    }

    private static void spawnSonarWave(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double dx = Math.cos(angle) * 3.0;
            double dz = Math.sin(angle) * 3.0;
            level.addParticle(ParticleTypes.SONIC_BOOM, pos.x + dx, pos.y + 1.0, pos.z + dz, dx * 0.5, 0, dz * 0.5);
        }
    }

    private static void spawnTechGlow(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 20; i++) {
            double dx = (Math.random() - 0.5) * 1.5;
            double dy = Math.random() * 2.0;
            double dz = (Math.random() - 0.5) * 1.5;
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.x + dx, pos.y + dy, pos.z + dz, 0, 0.05, 0);
        }
    }

    private static void spawnSlimeSplatter(ClientLevel level, Vec3 pos) {
        for (int i = 0; i < 20; i++) {
            double dx = (Math.random() - 0.5) * 2.0;
            double dy = Math.random() * 1.0;
            double dz = (Math.random() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.ITEM_SLIME, pos.x + dx, pos.y + dy, pos.z + dz, 0, -0.1, 0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Iterator<DynamicVfx> it = activeEffects.iterator();
        while (it.hasNext()) {
            DynamicVfx vfx = it.next();
            vfx.duration--;
            if (vfx.duration <= 0) {
                it.remove();
            }
        }
    }
}
