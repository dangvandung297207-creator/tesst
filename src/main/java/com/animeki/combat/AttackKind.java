package com.animeki.combat;

import com.animeki.config.AnimeKiServerConfig;
import com.animeki.vfx.VfxEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The moves of the martial arts combo.
 *
 * <p>The light chain is <b>punch &rarr; punch &rarr; kick &rarr; heavy finisher</b>. Each move carries
 * its own timing, damage, knockback behaviour and impact effect, but every number comes from the
 * configuration so a server owner can re-balance the whole chain.</p>
 */
public enum AttackKind {
    /** First light punch, step 0 of the grounded chain. */
    PUNCH_LEFT(0, false, false, false),
    /** Second light punch, step 1 of the grounded chain. */
    PUNCH_RIGHT(1, false, false, false),
    /** Rising kick, step 2. */
    KICK(2, false, true, false),
    /** Heavy finisher, step 3. Launches and splashes. */
    HEAVY(3, true, false, false),
    /** Air punch, step 0 of the air chain. */
    AIR_PUNCH(0, false, false, true),
    /** Air kick, step 1 of the air chain. */
    AIR_KICK(1, false, false, true),
    /** Air slam, step 2 of the air chain: knocks the target downwards. */
    AIR_SLAM(2, true, false, true),
    /** Chase follow up after a launch. */
    CHASE_STRIKE(0, false, true, true);

    private final int comboStep;
    private final boolean heavy;
    private final boolean launch;
    private final boolean aerial;

    AttackKind(int comboStep, boolean heavy, boolean launch, boolean aerial) {
        this.comboStep = comboStep;
        this.heavy = heavy;
        this.launch = launch;
        this.aerial = aerial;
    }

    public int comboStep() {
        return comboStep;
    }

    public boolean heavy() {
        return heavy;
    }

    /** Whether the move pushes the target upwards (knock up). */
    public boolean launch() {
        return launch;
    }

    public boolean aerial() {
        return aerial;
    }

    public double damage() {
        AnimeKiServerConfig.Combat config = AnimeKiServerConfig.COMBAT;
        return switch (this) {
            case PUNCH_LEFT, PUNCH_RIGHT -> config.lightDamage.get();
            case KICK -> config.kickDamage.get();
            case HEAVY, AIR_SLAM -> config.heavyDamage.get();
            case AIR_PUNCH -> config.lightDamage.get() * 0.9D;
            case AIR_KICK -> config.kickDamage.get() * 1.1D;
            case CHASE_STRIKE -> config.kickDamage.get();
        };
    }

    public double knockback() {
        AnimeKiServerConfig.Combat config = AnimeKiServerConfig.COMBAT;
        return heavy() ? config.knockbackHeavy.get() : config.knockbackLight.get();
    }

    public double kiCost() {
        return heavy() ? AnimeKiServerConfig.COMBAT.heavyKiCost.get() : 0.0D;
    }

    public int cooldownTicks() {
        return heavy() ? AnimeKiServerConfig.COMBAT.heavyCooldownTicks.get() : AnimeKiServerConfig.COMBAT.lightCooldownTicks.get();
    }

    public VfxEvent impactEvent() {
        return heavy() ? VfxEvent.HEAVY_HIT : VfxEvent.LIGHT_HIT;
    }

    public VfxEvent swingEvent() {
        return heavy() ? VfxEvent.COMBO_HIT : VfxEvent.SPARK;
    }

    public SoundEvent swingSound() {
        return heavy() ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_WEAK;
    }

    public SoundEvent hitSound() {
        return heavy() ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_NODAMAGE;
    }

    /** Resolves the next move of the chain for the given step, based on the current movement state. */
    public static AttackKind forStep(int step, boolean airborne) {
        if (airborne) {
            return switch (step) {
                case 0 -> AIR_PUNCH;
                case 1 -> AIR_KICK;
                default -> AIR_SLAM;
            };
        }
        return switch (step) {
            case 0 -> PUNCH_LEFT;
            case 1 -> PUNCH_RIGHT;
            case 2 -> KICK;
            default -> HEAVY;
        };
    }

    public static AttackKind byOrdinal(int ordinal) {
        AttackKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return PUNCH_LEFT;
        }
        return values[ordinal];
    }
}
