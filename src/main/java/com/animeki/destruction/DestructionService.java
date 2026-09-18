package com.animeki.destruction;

import com.animeki.config.AnimeKiServerConfig;
import com.animeki.registry.ModTags;
import com.animeki.util.MathUtil;
import com.animeki.vfx.VfxDispatcher;
import com.animeki.vfx.VfxEvent;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Controlled, server friendly terrain destruction.
 *
 * <ul>
 *     <li>Impacts are queued and applied over several ticks with a hard budget, so a big ultimate
 *     cannot freeze a server with thousands of block updates in a single tick.</li>
 *     <li>Every limit is configurable: radius, blocks per impact, blocks per tick, drops,
 *     protected blocks (config list plus the {@code animeki:indestructible} tag) and block
 *     entities.</li>
 *     <li>Bedrock, barriers, command blocks and anything protected by a pack are never removed.</li>
 * </ul>
 */
public final class DestructionService {
    private static final int MAX_QUEUE_PER_LEVEL = 8192;
    private static final int MAX_CANDIDATES = 16_000;

    private static final Map<ResourceKey<Level>, ArrayDeque<PendingBlock>> QUEUE = new HashMap<>();

    private DestructionService() {
    }

    /** A queued block removal plus the data needed to keep applying it later. */
    private record PendingBlock(BlockPos pos, boolean dropBlocks) {
    }

    /**
     * Queues a terrain impact.
     *
     * @param source the responsible entity (may be {@code null})
     * @param focus  main target, used only as a hint for future facing based effects
     */
    public static void apply(ServerLevel level, Vec3 center, DestructionProfile profile,
                             @Nullable Entity source, @Nullable Entity focus) {
        AnimeKiServerConfig.Destruction config = AnimeKiServerConfig.DESTRUCTION;
        if (!config.enabled.get()) {
            return;
        }
        double radius = Math.min(profile.radius(), config.maxRadius.get());
        int maxBlocks = Math.min(profile.maxBlocks(), config.maxBlocksPerImpact.get());
        if (radius <= 0.0D || maxBlocks <= 0) {
            return;
        }

        ArrayDeque<PendingBlock> queue = QUEUE.computeIfAbsent(level.dimension(), key -> new ArrayDeque<>());
        if (queue.size() > MAX_QUEUE_PER_LEVEL) {
            return;
        }

        boolean drops = profile.dropBlocks() && config.dropBlocks.get();
        for (BlockPos pos : collectBlocks(level, center, profile, radius, maxBlocks)) {
            queue.add(new PendingBlock(pos, drops));
        }

        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                1.6F, 0.7F);
        VfxDispatcher.play(level, VfxEvent.DEBRIS, center, null, (float) radius, VfxEvent.DEBRIS.defaultColor());
    }

    /** Collects the block positions of the requested shape, honouring every protection rule. */
    private static Set<BlockPos> collectBlocks(ServerLevel level, Vec3 center, DestructionProfile profile,
                                               double radius, int maxBlocks) {
        Set<BlockPos> result = new HashSet<>();
        int intRadius = (int) Math.ceil(radius);
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        double density = MathUtil.clamp(profile.density(), 0.0D, 1.0D);
        int candidateLimit = Math.min(maxBlocks * 4, MAX_CANDIDATES);

        for (int dx = -intRadius; dx <= intRadius; dx++) {
            for (int dz = -intRadius; dz <= intRadius; dz++) {
                int columnY = profile.shape() == DestructionProfile.Shape.CRATER
                        ? findSurface(level, cx + dx, cy, cz + dz, intRadius)
                        : cy;
                for (int dy = -intRadius; dy <= intRadius; dy++) {
                    if (result.size() >= maxBlocks || result.size() >= candidateLimit) {
                        return result;
                    }
                    int y = columnY + dy;
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    double distance = Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz);
                    if (distance > radius) {
                        continue;
                    }
                    if (profile.shape() == DestructionProfile.Shape.SHELL && distance < radius * 0.6D) {
                        continue;
                    }
                    if (level.getRandom().nextDouble() > density) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(cx + dx, y, cz + dz);
                    if (isProtected(level, pos)) {
                        continue;
                    }
                    result.add(pos);
                }
            }
        }
        return result;
    }

    /** Rough surface search used by crater shaped impacts. */
    private static int findSurface(ServerLevel level, int x, int centerY, int z, int radius) {
        int top = Math.min(level.getMaxBuildHeight() - 1, centerY + radius);
        int bottom = Math.max(level.getMinBuildHeight(), centerY - radius);
        for (int y = top; y >= bottom; y--) {
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return centerY;
    }

    /** Protection rules: tags, config list, hardness and block entities. */
    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(ModTags.Blocks.INDESTRUCTIBLE)) {
            return true;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return true;
        }
        AnimeKiServerConfig.Destruction config = AnimeKiServerConfig.DESTRUCTION;
        if (config.protectBlockEntities.get() && state.hasBlockEntity()) {
            return true;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (String protectedEntry : config.protectedBlocks.get()) {
            if (blockId.equals(protectedEntry)) {
                return true;
            }
        }
        return false;
    }

    /** Drains the queue within the configured per tick budget. */
    public static void tick(ServerLevel level) {
        AnimeKiServerConfig.Destruction config = AnimeKiServerConfig.DESTRUCTION;
        if (!config.enabled.get()) {
            QUEUE.remove(level.dimension());
            return;
        }
        ArrayDeque<PendingBlock> queue = QUEUE.get(level.dimension());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int budget = config.maxBlocksPerTick.get();
        int processed = 0;
        Iterator<PendingBlock> iterator = queue.iterator();
        while (iterator.hasNext() && processed < budget) {
            PendingBlock pending = iterator.next();
            iterator.remove();
            processed++;
            breakBlock(level, pending);
        }
    }

    private static void breakBlock(ServerLevel level, PendingBlock pending) {
        BlockPos pos = pending.pos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || isProtected(level, pos)) {
            return;
        }
        level.levelEvent(2001, pos, Block.getId(state));
        level.destroyBlock(pos, pending.dropBlocks());
    }

    public static int queued(ServerLevel level) {
        ArrayDeque<PendingBlock> queue = QUEUE.get(level.dimension());
        return queue == null ? 0 : queue.size();
    }

    public static void clear(ServerLevel level) {
        QUEUE.remove(level.dimension());
    }
}
