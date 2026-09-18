package com.animeki.registry;

import com.animeki.AnimeKi;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Datapack tag keys used by the destruction system so packs can extend the rules. */
public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        private Blocks() {
        }

        /** Blocks that can never be destroyed by Ki attacks (bedrock and friends live here). */
        public static final TagKey<Block> INDESTRUCTIBLE =
                TagKey.create(Registries.BLOCK, AnimeKi.id("indestructible"));

        /** Extra blocks that are allowed to be destroyed even if a pack would normally protect them. */
        public static final TagKey<Block> DESTRUCTIBLE_EXTRA =
                TagKey.create(Registries.BLOCK, AnimeKi.id("destructible_extra"));
    }
}
