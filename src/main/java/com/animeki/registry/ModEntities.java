package com.animeki.registry;

import com.animeki.AnimeKi;
import com.animeki.boss.VoidTitanEntity;
import com.animeki.entity.KiBlastEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity types.
 *
 * <p>Only two exist by design: the Ki Blast projectile and the Void Titan boss. Beams are not
 * entities at all (see {@code com.animeki.beam.BeamService}) so a duel between two fighters never
 * costs thousands of entity ticks.</p>
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AnimeKi.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<KiBlastEntity>> KI_BLAST =
            ENTITY_TYPES.register("ki_blast", () -> EntityType.Builder.<KiBlastEntity>of(KiBlastEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build(AnimeKi.id("ki_blast").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidTitanEntity>> VOID_TITAN =
            ENTITY_TYPES.register("void_titan", () -> EntityType.Builder.of(VoidTitanEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .fireImmune()
                    .build(AnimeKi.id("void_titan").toString()));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(VOID_TITAN.get(), VoidTitanEntity.createTitanAttributes().build());
    }
}
