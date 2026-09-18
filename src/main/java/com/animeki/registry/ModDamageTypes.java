package com.animeki.registry;

import com.animeki.AnimeKi;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Damage types owned by the mod.
 *
 * <p>The types themselves are datapack entries ({@code data/animeki/damage_type/*.json}) so servers
 * can rebalance death messages and scaling, while the code only ever references these keys.</p>
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> KI_BLAST = key("ki_blast");
    public static final ResourceKey<DamageType> BEAM = key("ki_beam");
    public static final ResourceKey<DamageType> MARTIAL = key("martial_strike");
    public static final ResourceKey<DamageType> ULTIMATE = key("ultimate");
    public static final ResourceKey<DamageType> CLASH = key("power_clash");
    public static final ResourceKey<DamageType> BOSS = key("boss_attack");

    private ModDamageTypes() {
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, AnimeKi.id(name));
    }

    public static void bootstrap() {
        AnimeKi.LOGGER.debug("animeki damage types are provided by datapack entries");
    }

    /** Builds a damage source, attributing kills to the causing entity (usually the caster). */
    public static DamageSource source(Level level, ResourceKey<DamageType> type,
                                      @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),
                directEntity, causingEntity);
    }

    /** Caster attributed source used by most Ki attacks. */
    public static DamageSource of(Level level, ResourceKey<DamageType> type, Entity caster) {
        return source(level, type, caster, caster);
    }
}
