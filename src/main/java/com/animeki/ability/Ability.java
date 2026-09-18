package com.animeki.ability;

import net.minecraft.server.level.ServerPlayer;

/**
 * Base class of every skill.
 *
 * <p>Adding a skill means: extend this class, register it in {@link AbilityRegistry}, point a
 * keybind or a boss controller at it. Nothing in the core combat loop needs to change.</p>
 */
public abstract class Ability {
    private final String id;
    private final AbilityStats stats;

    protected Ability(String id, AbilityStats stats) {
        this.id = id;
        this.stats = stats;
    }

    /** Namespaced id, for example {@code animeki:ki_blast}. */
    public String id() {
        return id;
    }

    public AbilityStats stats() {
        return stats;
    }

    /** Icon used by the HUD and by resource packs that restyle the ability wheel. */
    public net.minecraft.resources.ResourceLocation icon() {
        String path = id().contains(":") ? id().substring(id().indexOf(':') + 1) : id();
        return com.animeki.AnimeKi.id("textures/gui/ability/" + path + ".png");
    }

    /** Optional animation id resolved by the client (extensible to Blender exported clips). */
    public String animationId() {
        return "";
    }

    /** Called before any Ki is spent; return {@code false} to refuse the cast. */
    public boolean canStart(ServerPlayer player, AbilityContext context) {
        return true;
    }

    /** Ki has been taken and the ability is now running. */
    public void onStart(ServerPlayer player, AbilityContext context) {
    }

    /** Called every tick while the ability is still charging. */
    public void onChargeTick(ServerPlayer player, AbilityContext context, int chargeTicks) {
    }

    /** Called when the player releases the input of a charge or channel ability. */
    public void onRelease(ServerPlayer player, AbilityContext context, int chargeTicks) {
    }

    /** Called every tick while the ability is active. */
    public void onTick(ServerPlayer player, AbilityContext context, ActiveAbility active) {
    }

    /** Called exactly once when the ability stops for any reason. */
    public void onStop(ServerPlayer player, AbilityContext context, AbilityStopReason reason) {
    }

    /** Whether taking damage should interrupt the ability. */
    public boolean interruptibleByDamage() {
        return true;
    }

    /** Whether movement input should cancel the ability (used by cinematic ultimates). */
    public boolean cancelOnMovement() {
        return false;
    }

    /** Channel style abilities usually cancel instead of firing weakly when released early. */
    public boolean cancelWhenReleasedEarly() {
        return false;
    }

    /**
     * Scripted sequences (the ultimates) keep playing after the input is released; the ability
     * itself decides when the sequence is over by finishing its {@link ActiveAbility}.
     */
    public boolean continuesAfterRelease() {
        return false;
    }
}
