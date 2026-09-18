package com.animeki.ability;

import com.animeki.util.MathUtil;
import net.minecraft.server.level.ServerPlayer;

/** Runtime instance of a running ability, owned by exactly one player. */
public final class ActiveAbility {
    private final Ability ability;
    private final AbilityContext context;
    private final long startTick;
    private int ticks;
    private int chargeTicks;
    private boolean charged;
    private boolean released;
    private boolean finished;

    public ActiveAbility(Ability ability, AbilityContext context, long startTick) {
        this.ability = ability;
        this.context = context;
        this.startTick = startTick;
    }

    public Ability ability() {
        return ability;
    }

    public AbilityContext context() {
        return context;
    }

    public ServerPlayer player() {
        return context.player();
    }

    public long startTick() {
        return startTick;
    }

    public int ticks() {
        return ticks;
    }

    public void tick() {
        this.ticks++;
    }

    public int chargeTicks() {
        return chargeTicks;
    }

    public void addChargeTick() {
        this.chargeTicks++;
    }

    /** True once the charge phase finished (the ability has fired at least once). */
    public boolean charged() {
        return charged;
    }

    public void markCharged() {
        this.charged = true;
    }

    public boolean released() {
        return released;
    }

    public void markReleased() {
        this.released = true;
    }

    public boolean finished() {
        return finished;
    }

    public void markFinished() {
        this.finished = true;
    }

    /** Charge progress in {@code [0,1]} for HUD display. */
    public float progress() {
        int required = ability.stats().chargeTicks();
        if (required <= 0) {
            return 1.0F;
        }
        return MathUtil.clamp(chargeTicks / (float) required, 0.0F, 1.0F);
    }
}
