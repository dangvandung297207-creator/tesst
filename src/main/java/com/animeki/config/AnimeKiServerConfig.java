package com.animeki.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server side balance configuration.
 *
 * <p>Every combat number used by the mod is read from here. Abilities never hardcode balance
 * values, they ask the configuration (through small helper accessors) so server owners can
 * re-balance the whole mod without touching code.</p>
 */
public final class AnimeKiServerConfig {
    public static final ModConfigSpec SPEC;
    public static final Ki KI;
    public static final Combat COMBAT;
    public static final Flight FLIGHT;
    public static final Dash DASH;
    public static final Vanish VANISH;
    public static final Abilities ABILITIES;
    public static final Destruction DESTRUCTION;
    public static final Boss BOSS;
    public static final Transformations TRANSFORMATIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        AnimeKiServerConfig instance = new AnimeKiServerConfig(builder);
        INSTANCE = instance;
        SPEC = builder.build();
        KI = instance.ki;
        COMBAT = instance.combat;
        FLIGHT = instance.flight;
        DASH = instance.dash;
        VANISH = instance.vanish;
        ABILITIES = instance.abilities;
        DESTRUCTION = instance.destruction;
        BOSS = instance.boss;
        TRANSFORMATIONS = instance.transformations;
    }

    private static AnimeKiServerConfig INSTANCE;

    public final Ki ki;
    public final Combat combat;
    public final Flight flight;
    public final Dash dash;
    public final Vanish vanish;
    public final Abilities abilities;
    public final Destruction destruction;
    public final Boss boss;
    public final Transformations transformations;

    private AnimeKiServerConfig(ModConfigSpec.Builder builder) {
        this.ki = new Ki(builder);
        this.combat = new Combat(builder);
        this.flight = new Flight(builder);
        this.dash = new Dash(builder);
        this.vanish = new Vanish(builder);
        this.abilities = new Abilities(builder);
        this.destruction = new Destruction(builder);
        this.boss = new Boss(builder);
        this.transformations = new Transformations(builder);
    }

    /** Core Ki pool behaviour. */
    public static final class Ki {
        public final ModConfigSpec.DoubleValue maxKi;
        public final ModConfigSpec.DoubleValue regenerationPerTick;
        public final ModConfigSpec.IntValue regenerationDelayTicks;
        public final ModConfigSpec.DoubleValue chargePerTick;
        public final ModConfigSpec.DoubleValue chargeOutputBonus;
        public final ModConfigSpec.DoubleValue globalOutputMultiplier;
        public final ModConfigSpec.IntValue exhaustionDurationTicks;
        public final ModConfigSpec.DoubleValue exhaustionRegenerationMultiplier;
        public final ModConfigSpec.BooleanValue chargingRequiresStandingStill;
        public final ModConfigSpec.DoubleValue movementInterruptTolerance;
        public final ModConfigSpec.BooleanValue interruptChargeOnDamage;
        public final ModConfigSpec.BooleanValue enableForAllPlayers;

        Ki(ModConfigSpec.Builder builder) {
            builder.comment("Core Ki energy pool. Baseline pool size is 1000.").push("ki");
            maxKi = builder.comment("Maximum Ki a player starts with.")
                    .defineInRange("maxKi", 1000.0D, 10.0D, 1_000_000.0D);
            regenerationPerTick = builder.comment("Passive Ki regeneration per server tick while not charging.")
                    .defineInRange("regenerationPerTick", 0.35D, 0.0D, 1000.0D);
            regenerationDelayTicks = builder.comment("Ticks after spending Ki before passive regeneration resumes.")
                    .defineInRange("regenerationDelayTicks", 30, 0, 1200);
            chargePerTick = builder.comment("Ki gained per tick while charging.")
                    .defineInRange("chargePerTick", 12.0D, 0.0D, 10000.0D);
            chargeOutputBonus = builder.comment("Extra Ki output multiplier granted while charging (0.35 = +35%).")
                    .defineInRange("chargeOutputBonus", 0.35D, 0.0D, 10.0D);
            globalOutputMultiplier = builder.comment("Global multiplier applied to every Ki output calculation.")
                    .defineInRange("globalOutputMultiplier", 1.0D, 0.0D, 100.0D);
            exhaustionDurationTicks = builder.comment("How long a player stays exhausted after burning out all Ki.")
                    .defineInRange("exhaustionDurationTicks", 80, 0, 12000);
            exhaustionRegenerationMultiplier = builder.comment("Regeneration multiplier while exhausted.")
                    .defineInRange("exhaustionRegenerationMultiplier", 0.3D, 0.0D, 10.0D);
            chargingRequiresStandingStill = builder.comment("Charging is interrupted when the player moves.")
                    .define("chargingRequiresStandingStill", true);
            movementInterruptTolerance = builder.comment("Horizontal speed that still counts as standing still while charging.")
                    .defineInRange("movementInterruptTolerance", 0.06D, 0.0D, 1.0D);
            interruptChargeOnDamage = builder.comment("Taking damage interrupts a charge and briefly blocks regeneration.")
                    .define("interruptChargeOnDamage", true);
            enableForAllPlayers = builder.comment("Give every player the Ki system (disable to gate it behind a command).")
                    .define("enableForAllPlayers", true);
            builder.pop();
        }
    }

    /** Martial arts combo combat. */
    public static final class Combat {
        public final ModConfigSpec.BooleanValue replaceVanillaAttack;
        public final ModConfigSpec.DoubleValue lightDamage;
        public final ModConfigSpec.DoubleValue kickDamage;
        public final ModConfigSpec.DoubleValue heavyDamage;
        public final ModConfigSpec.DoubleValue heavyFinisherMultiplier;
        public final ModConfigSpec.IntValue lightCooldownTicks;
        public final ModConfigSpec.IntValue heavyCooldownTicks;
        public final ModConfigSpec.IntValue comboWindowTicks;
        public final ModConfigSpec.IntValue airComboWindowTicks;
        public final ModConfigSpec.DoubleValue comboDamageBonusPerHit;
        public final ModConfigSpec.DoubleValue knockbackLight;
        public final ModConfigSpec.DoubleValue knockbackHeavy;
        public final ModConfigSpec.DoubleValue knockUpPower;
        public final ModConfigSpec.DoubleValue knockDownPower;
        public final ModConfigSpec.DoubleValue meleeRange;
        public final ModConfigSpec.DoubleValue meleeArcDegrees;
        public final ModConfigSpec.DoubleValue heavyKiCost;
        public final ModConfigSpec.BooleanValue nearbyHeavyDamage;
        public final ModConfigSpec.DoubleValue nearbyHeavyRadius;
        public final ModConfigSpec.BooleanValue airCombatEnabled;
        public final ModConfigSpec.BooleanValue chaseAttackEnabled;
        public final ModConfigSpec.DoubleValue chaseAttackRange;
        public final ModConfigSpec.DoubleValue chaseAttackKiCost;
        public final ModConfigSpec.BooleanValue attackPlayers;

        Combat(ModConfigSpec.Builder builder) {
            builder.comment("Original martial arts combo system: light, light, kick, heavy finisher.").push("combat");
            replaceVanillaAttack = builder.comment("Replace vanilla melee swings with the martial arts combo.")
                    .define("replaceVanillaAttack", true);
            attackPlayers = builder.comment("Allow martial arts attacks to hit other players.")
                    .define("attackPlayers", true);
            lightDamage = builder.comment("Damage of the first two light punches.")
                    .defineInRange("lightDamage", 3.0D, 0.0D, 1000.0D);
            kickDamage = builder.comment("Damage of the third combo step (kick).")
                    .defineInRange("kickDamage", 5.5D, 0.0D, 1000.0D);
            heavyDamage = builder.comment("Damage of the heavy finisher.")
                    .defineInRange("heavyDamage", 9.0D, 0.0D, 1000.0D);
            heavyFinisherMultiplier = builder.comment("Extra damage multiplier applied to a heavy finisher that ends a full combo.")
                    .defineInRange("heavyFinisherMultiplier", 1.45D, 0.0D, 20.0D);
            lightCooldownTicks = builder.comment("Server side cooldown between light attacks.")
                    .defineInRange("lightCooldownTicks", 5, 0, 200);
            heavyCooldownTicks = builder.comment("Server side cooldown between heavy attacks.")
                    .defineInRange("heavyCooldownTicks", 14, 0, 200);
            comboWindowTicks = builder.comment("Time window to continue a grounded combo.")
                    .defineInRange("comboWindowTicks", 26, 1, 400);
            airComboWindowTicks = builder.comment("Time window to continue an air combo.")
                    .defineInRange("airComboWindowTicks", 34, 1, 400);
            comboDamageBonusPerHit = builder.comment("Damage bonus per combo step already landed.")
                    .defineInRange("comboDamageBonusPerHit", 0.1D, 0.0D, 10.0D);
            knockbackLight = builder.comment("Horizontal knockback of light hits.")
                    .defineInRange("knockbackLight", 0.28D, 0.0D, 20.0D);
            knockbackHeavy = builder.comment("Horizontal knockback of heavy hits.")
                    .defineInRange("knockbackHeavy", 0.85D, 0.0D, 20.0D);
            knockUpPower = builder.comment("Upward launch power of knock-up attacks.")
                    .defineInRange("knockUpPower", 0.62D, 0.0D, 20.0D);
            knockDownPower = builder.comment("Downward slam power of knock-down attacks.")
                    .defineInRange("knockDownPower", 0.9D, 0.0D, 20.0D);
            meleeRange = builder.comment("Reach of martial arts strikes in blocks.")
                    .defineInRange("meleeRange", 3.4D, 0.5D, 32.0D);
            meleeArcDegrees = builder.comment("Half angle of the strike cone in degrees.")
                    .defineInRange("meleeArcDegrees", 55.0D, 5.0D, 180.0D);
            heavyKiCost = builder.comment("Ki spent on a heavy finisher.")
                    .defineInRange("heavyKiCost", 25.0D, 0.0D, 100000.0D);
            nearbyHeavyDamage = builder.comment("Heavy hits also damage entities close to the main target.")
                    .define("nearbyHeavyDamage", true);
            nearbyHeavyRadius = builder.comment("Radius of the heavy hit splash damage.")
                    .defineInRange("nearbyHeavyRadius", 2.5D, 0.0D, 16.0D);
            airCombatEnabled = builder.comment("Enable air punches, air kicks, knock-up and knock-down.")
                    .define("airCombatEnabled", true);
            chaseAttackEnabled = builder.comment("Allow a chase attack after a knock-up by pressing dash toward the target.")
                    .define("chaseAttackEnabled", true);
            chaseAttackRange = builder.comment("Maximum chase distance.")
                    .defineInRange("chaseAttackRange", 18.0D, 1.0D, 128.0D);
            chaseAttackKiCost = builder.comment("Ki cost of a chase attack.")
                    .defineInRange("chaseAttackKiCost", 20.0D, 0.0D, 100000.0D);
            builder.pop();
        }
    }

    /** Ki powered flight. */
    public static final class Flight {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue speed;
        public final ModConfigSpec.DoubleValue boostMultiplier;
        public final ModConfigSpec.DoubleValue verticalSpeed;
        public final ModConfigSpec.DoubleValue acceleration;
        public final ModConfigSpec.DoubleValue drag;
        public final ModConfigSpec.DoubleValue maxSpeed;
        public final ModConfigSpec.DoubleValue kiDrainPerTick;
        public final ModConfigSpec.DoubleValue boostKiDrainPerTick;
        public final ModConfigSpec.IntValue disableOnDamageTicks;
        public final ModConfigSpec.BooleanValue requireTransformation;

        Flight(ModConfigSpec.Builder builder) {
            builder.comment("Ki powered flight (survival compatible, does not touch creative flight).").push("flight");
            enabled = builder.define("enabled", true);
            speed = builder.comment("Base horizontal flight speed in blocks/tick.")
                    .defineInRange("speed", 0.72D, 0.05D, 20.0D);
            boostMultiplier = builder.comment("Sprint boost multiplier.")
                    .defineInRange("boostMultiplier", 1.85D, 1.0D, 20.0D);
            verticalSpeed = builder.comment("Vertical ascend/descend speed.")
                    .defineInRange("verticalSpeed", 0.5D, 0.05D, 20.0D);
            acceleration = builder.comment("Acceleration towards the wanted velocity.")
                    .defineInRange("acceleration", 0.1D, 0.01D, 1.0D);
            drag = builder.comment("Velocity retention per tick when no input is given (air braking = lower value).")
                    .defineInRange("drag", 0.86D, 0.0D, 1.0D);
            maxSpeed = builder.comment("Hard cap for flight velocity.")
                    .defineInRange("maxSpeed", 3.2D, 0.1D, 50.0D);
            kiDrainPerTick = builder.comment("Ki drained per tick while flying.")
                    .defineInRange("kiDrainPerTick", 0.5D, 0.0D, 10000.0D);
            boostKiDrainPerTick = builder.comment("Additional Ki drained per tick while boosting.")
                    .defineInRange("boostKiDrainPerTick", 0.9D, 0.0D, 10000.0D);
            disableOnDamageTicks = builder.comment("Ticks of flight lockout after taking damage.")
                    .defineInRange("disableOnDamageTicks", 20, 0, 1200);
            requireTransformation = builder.comment("Only allow flight while transformed.")
                    .define("requireTransformation", false);
            builder.pop();
        }
    }

    /** High speed dash. */
    public static final class Dash {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue speed;
        public final ModConfigSpec.DoubleValue airSpeed;
        public final ModConfigSpec.IntValue cooldownTicks;
        public final ModConfigSpec.DoubleValue kiCost;
        public final ModConfigSpec.IntValue invulnerabilityTicks;
        public final ModConfigSpec.IntValue chargeTicks;
        public final ModConfigSpec.BooleanValue airDashEnabled;
        public final ModConfigSpec.IntValue airDashCharges;
        public final ModConfigSpec.DoubleValue impactDamage;
        public final ModConfigSpec.DoubleValue sonicBoomSpeed;

        Dash(ModConfigSpec.Builder builder) {
            builder.comment("Directional high speed dash.").push("dash");
            enabled = builder.define("enabled", true);
            speed = builder.comment("Dash velocity on the ground.")
                    .defineInRange("speed", 2.3D, 0.1D, 50.0D);
            airSpeed = builder.comment("Dash velocity in the air.")
                    .defineInRange("airSpeed", 2.1D, 0.1D, 50.0D);
            cooldownTicks = builder.defineInRange("cooldownTicks", 26, 0, 2400);
            kiCost = builder.defineInRange("kiCost", 35.0D, 0.0D, 100000.0D);
            invulnerabilityTicks = builder.defineInRange("invulnerabilityTicks", 6, 0, 100);
            chargeTicks = builder.comment("Short preparation window before the dash launches.")
                    .defineInRange("chargeTicks", 4, 0, 100);
            airDashEnabled = builder.define("airDashEnabled", true);
            airDashCharges = builder.comment("Air dashes available before touching the ground.")
                    .defineInRange("airDashCharges", 2, 0, 20);
            impactDamage = builder.comment("Damage dealt to entities hit while dashing.")
                    .defineInRange("impactDamage", 6.0D, 0.0D, 1000.0D);
            sonicBoomSpeed = builder.comment("Speed above which a sonic boom effect is created.")
                    .defineInRange("sonicBoomSpeed", 0.85D, 0.1D, 50.0D);
            builder.pop();
        }
    }

    /** Short range combat teleport. */
    public static final class Vanish {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue range;
        public final ModConfigSpec.IntValue cooldownTicks;
        public final ModConfigSpec.DoubleValue kiCost;
        public final ModConfigSpec.BooleanValue targetDetection;
        public final ModConfigSpec.DoubleValue targetRange;
        public final ModConfigSpec.IntValue postVanishAttackWindow;
        public final ModConfigSpec.DoubleValue postVanishDamageBonus;

        Vanish(ModConfigSpec.Builder builder) {
            builder.comment("Vanish / short range teleport used for repositioning in melee.").push("vanish");
            enabled = builder.define("enabled", true);
            range = builder.defineInRange("range", 16.0D, 1.0D, 128.0D);
            cooldownTicks = builder.defineInRange("cooldownTicks", 55, 0, 2400);
            kiCost = builder.defineInRange("kiCost", 55.0D, 0.0D, 100000.0D);
            targetDetection = builder.comment("Prefer teleporting behind the entity the player is aiming at.")
                    .define("targetDetection", true);
            targetRange = builder.defineInRange("targetRange", 22.0D, 1.0D, 256.0D);
            postVanishAttackWindow = builder.comment("Ticks after a vanish in which attacks gain a bonus.")
                    .defineInRange("postVanishAttackWindow", 24, 0, 400);
            postVanishDamageBonus = builder.defineInRange("postVanishDamageBonus", 0.3D, 0.0D, 10.0D);
            builder.pop();
        }
    }

    /** Ability specific tuning. Abilities read their numbers from here. */
    public static final class Abilities {
        public final ModConfigSpec.ConfigValue<String> primarySkill;
        public final ModConfigSpec.ConfigValue<String> secondarySkill;
        public final ModConfigSpec.ConfigValue<String> ultimateSkill;

        public final KiBlast kiBlast;
        public final ChargedKiBlast chargedKiBlast;
        public final Beam beam;
        public final Clash clash;
        public final GroundSlam groundSlam;
        public final DashStrike dashStrike;
        public final Meteor meteor;
        public final Sphere sphere;

        Abilities(ModConfigSpec.Builder builder) {
            builder.comment("Ability framework tuning.").push("abilities");
            primarySkill = builder.comment("Ability bound to the Primary Skill key.")
                    .define("primarySkill", "animeki:energy_beam");
            secondarySkill = builder.comment("Ability bound to the Secondary Skill key.")
                    .define("secondarySkill", "animeki:ground_slam");
            ultimateSkill = builder.comment("Ability bound to the Ultimate key.")
                    .define("ultimateSkill", "animeki:meteor_strike");
            this.kiBlast = new KiBlast(builder);
            this.chargedKiBlast = new ChargedKiBlast(builder);
            this.beam = new Beam(builder);
            this.clash = new Clash(builder);
            this.groundSlam = new GroundSlam(builder);
            this.dashStrike = new DashStrike(builder);
            this.meteor = new Meteor(builder);
            this.sphere = new Sphere(builder);
            builder.pop();
        }

        public static final class KiBlast {
            public final ModConfigSpec.DoubleValue kiCost;
            public final ModConfigSpec.DoubleValue damage;
            public final ModConfigSpec.DoubleValue speed;
            public final ModConfigSpec.DoubleValue range;
            public final ModConfigSpec.DoubleValue explosionRadius;
            public final ModConfigSpec.DoubleValue knockback;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.DoubleValue chargedDamageMultiplier;
            public final ModConfigSpec.IntValue chargedChargeTicks;
            public final ModConfigSpec.DoubleValue chargedKiCost;

            KiBlast(ModConfigSpec.Builder builder) {
                builder.push("kiBlast");
                kiCost = builder.defineInRange("kiCost", 40.0D, 0.0D, 100000.0D);
                damage = builder.defineInRange("damage", 7.5D, 0.0D, 10000.0D);
                speed = builder.defineInRange("speed", 0.95D, 0.05D, 20.0D);
                range = builder.defineInRange("range", 48.0D, 1.0D, 512.0D);
                explosionRadius = builder.defineInRange("explosionRadius", 2.2D, 0.0D, 32.0D);
                knockback = builder.defineInRange("knockback", 0.55D, 0.0D, 20.0D);
                cooldownTicks = builder.defineInRange("cooldownTicks", 8, 0, 1200);
                chargedChargeTicks = builder.defineInRange("chargedChargeTicks", 30, 0, 400);
                chargedKiCost = builder.defineInRange("chargedKiCost", 110.0D, 0.0D, 100000.0D);
                chargedDamageMultiplier = builder.defineInRange("chargedDamageMultiplier", 2.6D, 0.1D, 50.0D);
                builder.pop();
            }
        }

        public static final class ChargedKiBlast {
            public final ModConfigSpec.DoubleValue kiCost;
            public final ModConfigSpec.DoubleValue damage;
            public final ModConfigSpec.IntValue chargeTicks;
            public final ModConfigSpec.IntValue projectiles;
            public final ModConfigSpec.DoubleValue spreadDegrees;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.DoubleValue speed;

            ChargedKiBlast(ModConfigSpec.Builder builder) {
                builder.push("chargedKiBlast");
                kiCost = builder.defineInRange("kiCost", 140.0D, 0.0D, 100000.0D);
                damage = builder.defineInRange("damage", 6.0D, 0.0D, 10000.0D);
                chargeTicks = builder.defineInRange("chargeTicks", 35, 0, 400);
                projectiles = builder.defineInRange("projectiles", 5, 1, 40);
                spreadDegrees = builder.defineInRange("spreadDegrees", 9.0D, 0.0D, 90.0D);
                cooldownTicks = builder.defineInRange("cooldownTicks", 40, 0, 2400);
                speed = builder.defineInRange("speed", 1.1D, 0.05D, 20.0D);
                builder.pop();
            }
        }

        public static final class Beam {
            public final ModConfigSpec.DoubleValue kiCostPerTick;
            public final ModConfigSpec.DoubleValue damagePerTick;
            public final ModConfigSpec.IntValue chargeTicks;
            public final ModConfigSpec.DoubleValue range;
            public final ModConfigSpec.DoubleValue width;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.IntValue maxDurationTicks;
            public final ModConfigSpec.DoubleValue blockBreakChance;
            public final ModConfigSpec.IntValue entityDamageInterval;
            public final ModConfigSpec.DoubleValue knockback;

            Beam(ModConfigSpec.Builder builder) {
                builder.push("energyBeam");
                kiCostPerTick = builder.defineInRange("kiCostPerTick", 7.0D, 0.0D, 100000.0D);
                damagePerTick = builder.comment("Damage applied every entityDamageInterval ticks of contact.")
                        .defineInRange("damagePerTick", 2.4D, 0.0D, 10000.0D);
                chargeTicks = builder.defineInRange("chargeTicks", 40, 0, 1000);
                range = builder.defineInRange("range", 48.0D, 4.0D, 512.0D);
                width = builder.defineInRange("width", 1.15D, 0.05D, 32.0D);
                cooldownTicks = builder.defineInRange("cooldownTicks", 50, 0, 2400);
                maxDurationTicks = builder.defineInRange("maxDurationTicks", 120, 1, 12000);
                blockBreakChance = builder.comment("Chance per tick to erode a block at the beam impact point.")
                        .defineInRange("blockBreakChance", 0.35D, 0.0D, 1.0D);
                entityDamageInterval = builder.defineInRange("entityDamageInterval", 4, 1, 200);
                knockback = builder.defineInRange("knockback", 0.25D, 0.0D, 20.0D);
                builder.pop();
            }
        }

        public static final class Clash {
            public final ModConfigSpec.BooleanValue enabled;
            public final ModConfigSpec.DoubleValue pushRate;
            public final ModConfigSpec.IntValue maxDurationTicks;
            public final ModConfigSpec.DoubleValue clashPointDrift;
            public final ModConfigSpec.DoubleValue burstDamage;
            public final ModConfigSpec.DoubleValue burstRadius;
            public final ModConfigSpec.DoubleValue drainMultiplier;

            Clash(ModConfigSpec.Builder builder) {
                builder.comment("Beam versus beam power clash.").push("powerClash");
                enabled = builder.define("enabled", true);
                pushRate = builder.comment("How fast the clash point travels towards the weaker beam.")
                        .defineInRange("pushRate", 0.05D, 0.001D, 1.0D);
                maxDurationTicks = builder.defineInRange("maxDurationTicks", 200, 20, 12000);
                clashPointDrift = builder.comment("Random drift of the clash point for a lively look.")
                        .defineInRange("clashPointDrift", 0.02D, 0.0D, 1.0D);
                burstDamage = builder.comment("Damage of the explosion when a clash resolves.")
                        .defineInRange("burstDamage", 22.0D, 0.0D, 10000.0D);
                burstRadius = builder.defineInRange("burstRadius", 5.0D, 0.0D, 64.0D);
                drainMultiplier = builder.comment("Extra Ki drain multiplier for both fighters during a clash.")
                        .defineInRange("drainMultiplier", 1.6D, 0.1D, 20.0D);
                builder.pop();
            }
        }

        public static final class GroundSlam {
            public final ModConfigSpec.DoubleValue kiCost;
            public final ModConfigSpec.DoubleValue damage;
            public final ModConfigSpec.DoubleValue radius;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.DoubleValue knockUp;
            public final ModConfigSpec.DoubleValue craterRadius;
            
            GroundSlam(ModConfigSpec.Builder builder) {
                builder.push("groundSlam");
                kiCost = builder.defineInRange("kiCost", 80.0D, 0.0D, 100000.0D);
                damage = builder.defineInRange("damage", 15.0D, 0.0D, 10000.0D);
                radius = builder.defineInRange("radius", 6.0D, 0.5D, 64.0D);
                cooldownTicks = builder.defineInRange("cooldownTicks", 110, 0, 2400);
                knockUp = builder.defineInRange("knockUp", 0.9D, 0.0D, 20.0D);
                craterRadius = builder.defineInRange("craterRadius", 3.2D, 0.0D, 32.0D);
                builder.pop();
            }
        }

        public static final class DashStrike {
            public final ModConfigSpec.DoubleValue kiCost;
            public final ModConfigSpec.DoubleValue damage;
            public final ModConfigSpec.DoubleValue knockback;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.DoubleValue speed;

            DashStrike(ModConfigSpec.Builder builder) {
                builder.push("dashStrike");
                kiCost = builder.defineInRange("kiCost", 55.0D, 0.0D, 100000.0D);
                damage = builder.defineInRange("damage", 13.0D, 0.0D, 10000.0D);
                knockback = builder.defineInRange("knockback", 1.0D, 0.0D, 20.0D);
                cooldownTicks = builder.defineInRange("cooldownTicks", 45, 0, 2400);
                speed = builder.defineInRange("speed", 2.6D, 0.1D, 50.0D);
                builder.pop();
            }
        }

        public static final class Meteor {
            public final ModConfigSpec.DoubleValue kiCost;
            public final ModConfigSpec.DoubleValue damage;
            public final ModConfigSpec.IntValue chargeTicks;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.DoubleValue radius;
            public final ModConfigSpec.DoubleValue craterRadius;
            public final ModConfigSpec.DoubleValue riseHeight;
            public final ModConfigSpec.IntValue riseTicks;
            public final ModConfigSpec.IntValue fallTicks;

            Meteor(ModConfigSpec.Builder builder) {
                builder.push("meteorStrike");
                kiCost = builder.defineInRange("kiCost", 450.0D, 0.0D, 1000000.0D);
                damage = builder.defineInRange("damage", 55.0D, 0.0D, 100000.0D);
                chargeTicks = builder.defineInRange("chargeTicks", 60, 0, 2000);
                cooldownTicks = builder.defineInRange("cooldownTicks", 600, 0, 24000);
                radius = builder.defineInRange("radius", 11.0D, 1.0D, 128.0D);
                craterRadius = builder.defineInRange("craterRadius", 8.0D, 0.0D, 64.0D);
                riseHeight = builder.defineInRange("riseHeight", 26.0D, 4.0D, 200.0D);
                riseTicks = builder.defineInRange("riseTicks", 30, 1, 600);
                fallTicks = builder.defineInRange("fallTicks", 26, 1, 600);
                builder.pop();
            }
        }

        public static final class Sphere {
            public final ModConfigSpec.DoubleValue kiCost;
            public final ModConfigSpec.DoubleValue damage;
            public final ModConfigSpec.IntValue chargeTicks;
            public final ModConfigSpec.IntValue cooldownTicks;
            public final ModConfigSpec.DoubleValue speed;
            public final ModConfigSpec.DoubleValue explosionRadius;
            public final ModConfigSpec.DoubleValue size;

            Sphere(ModConfigSpec.Builder builder) {
                builder.push("ultimateSphere");
                kiCost = builder.defineInRange("kiCost", 350.0D, 0.0D, 1000000.0D);
                damage = builder.defineInRange("damage", 42.0D, 0.0D, 100000.0D);
                chargeTicks = builder.defineInRange("chargeTicks", 50, 0, 2000);
                cooldownTicks = builder.defineInRange("cooldownTicks", 500, 0, 24000);
                speed = builder.defineInRange("speed", 0.55D, 0.05D, 20.0D);
                explosionRadius = builder.defineInRange("explosionRadius", 8.0D, 0.0D, 64.0D);
                size = builder.defineInRange("size", 2.4D, 0.2D, 16.0D);
                builder.pop();
            }
        }
    }

    /** Controlled terrain destruction. */
    public static final class Destruction {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue maxRadius;
        public final ModConfigSpec.IntValue maxBlocksPerImpact;
        public final ModConfigSpec.IntValue maxBlocksPerTick;
        public final ModConfigSpec.BooleanValue dropBlocks;
        public final ModConfigSpec.BooleanValue protectBlockEntities;
        public final ModConfigSpec.IntValue cooldownTicks;
        public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> protectedBlocks;
        public final ModConfigSpec.BooleanValue debrisVfx;

        Destruction(ModConfigSpec.Builder builder) {
            builder.comment("Controlled arena destruction. Bedrock and protected blocks are never removed.").push("destruction");
            enabled = builder.define("enabled", true);
            maxRadius = builder.comment("Hard cap for any destruction radius.")
                    .defineInRange("maxRadius", 8.0D, 0.0D, 64.0D);
            maxBlocksPerImpact = builder.comment("Maximum blocks a single impact may remove.")
                    .defineInRange("maxBlocksPerImpact", 320, 0, 100000);
            maxBlocksPerTick = builder.comment("Server wide budget of block changes per tick from this mod.")
                    .defineInRange("maxBlocksPerTick", 64, 1, 100000);
            dropBlocks = builder.comment("Drop items for destroyed blocks.")
                    .define("dropBlocks", false);
            protectBlockEntities = builder.comment("Never destroy blocks that own a block entity (chests, machines, ...).")
                    .define("protectBlockEntities", true);
            cooldownTicks = builder.defineInRange("cooldownTicks", 8, 0, 2400);
            protectedBlocks = builder.comment("Extra protected block ids (in addition to the animeki:indestructible tag).")
                    .defineListAllowEmpty("protectedBlocks", java.util.List.of("minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:ancient_debris", "minecraft:respawn_anchor"), () -> "minecraft:stone", o -> o instanceof String);
            debrisVfx = builder.define("debrisVfx", true);
            builder.pop();
        }
    }

    /** Boss difficulty for the Void Titan (and future bosses). */
    public static final class Boss {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue healthMultiplier;
        public final ModConfigSpec.DoubleValue damageMultiplier;
        public final ModConfigSpec.DoubleValue abilityCooldownMultiplier;
        public final ModConfigSpec.DoubleValue phase2Threshold;
        public final ModConfigSpec.DoubleValue phase3Threshold;
        public final ModConfigSpec.DoubleValue rageDamageBonus;
        public final ModConfigSpec.BooleanValue terrainDestruction;
        public final ModConfigSpec.BooleanValue respectMobGriefing;

        Boss(ModConfigSpec.Builder builder) {
            builder.comment("Boss framework difficulty knobs.").push("boss");
            enabled = builder.define("enabled", true);
            healthMultiplier = builder.defineInRange("healthMultiplier", 1.0D, 0.1D, 100.0D);
            damageMultiplier = builder.defineInRange("damageMultiplier", 1.0D, 0.0D, 100.0D);
            abilityCooldownMultiplier = builder.comment("Higher values make bosses wait longer between abilities.")
                    .defineInRange("abilityCooldownMultiplier", 1.0D, 0.1D, 20.0D);
            phase2Threshold = builder.comment("Health fraction at which phase 2 begins.")
                    .defineInRange("phase2Threshold", 0.66D, 0.01D, 0.99D);
            phase3Threshold = builder.comment("Health fraction at which phase 3 begins.")
                    .defineInRange("phase3Threshold", 0.33D, 0.01D, 0.98D);
            rageDamageBonus = builder.defineInRange("rageDamageBonus", 0.3D, 0.0D, 10.0D);
            terrainDestruction = builder.define("terrainDestruction", true);
            respectMobGriefing = builder.comment("Respect mobGriefing gamerule for boss terrain damage.")
                    .define("respectMobGriefing", true);
            builder.pop();
        }
    }

    /** Per transformation balance. */
    public static final class Transformations {
        public final Ascended ascended;
        public final Divine divine;
        public final Celestial celestial;
        public final Transcendent transcendent;
        public final ModConfigSpec.BooleanValue allowStackingTransformations;
        public final ModConfigSpec.IntValue globalDrainInterval;

        Transformations(ModConfigSpec.Builder builder) {
            builder.comment("Transformation balance. Each form has its own multiplier block.").push("transformations");
            allowStackingTransformations = builder.define("allowStackingTransformations", false);
            globalDrainInterval = builder.comment("Ticks between transformation drain applications.")
                    .defineInRange("drainInterval", 20, 1, 400);
            this.ascended = new Ascended(builder);
            this.divine = new Divine(builder);
            this.celestial = new Celestial(builder);
            this.transcendent = new Transcendent(builder);
            builder.pop();
        }

        /** Shared layout of a transformation block. */
        public abstract static class Form {
            public final ModConfigSpec.BooleanValue enabled;
            public final ModConfigSpec.DoubleValue kiMultiplier;
            public final ModConfigSpec.DoubleValue damageMultiplier;
            public final ModConfigSpec.DoubleValue defenseMultiplier;
            public final ModConfigSpec.DoubleValue speedMultiplier;
            public final ModConfigSpec.DoubleValue flightMultiplier;
            public final ModConfigSpec.DoubleValue regenerationMultiplier;
            public final ModConfigSpec.DoubleValue drainPerSecond;
            public final ModConfigSpec.DoubleValue activationKiCost;
            public final ModConfigSpec.IntValue durationTicks;
            public final ModConfigSpec.IntValue sequenceSpeed;

            Form(ModConfigSpec.Builder builder, String name) {
                builder.push(name);
                enabled = builder.define("enabled", true);
                kiMultiplier = builder.defineInRange("kiMultiplier", 1.35D, 0.1D, 100.0D);
                damageMultiplier = builder.defineInRange("damageMultiplier", 1.3D, 0.1D, 100.0D);
                defenseMultiplier = builder.defineInRange("defenseMultiplier", 1.2D, 0.1D, 100.0D);
                speedMultiplier = builder.defineInRange("speedMultiplier", 1.15D, 0.1D, 20.0D);
                flightMultiplier = builder.defineInRange("flightMultiplier", 1.2D, 0.1D, 20.0D);
                regenerationMultiplier = builder.defineInRange("regenerationMultiplier", 1.2D, 0.0D, 100.0D);
                drainPerSecond = builder.defineInRange("drainPerSecond", 6.0D, 0.0D, 100000.0D);
                activationKiCost = builder.defineInRange("activationKiCost", 120.0D, 0.0D, 1000000.0D);
                durationTicks = builder.comment("0 means the form lasts until Ki runs out or the player cancels.")
                        .defineInRange("durationTicks", 0, 0, 1000000);
                sequenceSpeed = builder.comment("Ticks per transformation sequence step.")
                        .defineInRange("sequenceSpeed", 20, 4, 200);
                builder.pop();
            }
        }

        public static final class Ascended extends Form {
            Ascended(ModConfigSpec.Builder builder) {
                super(builder, "ascended");
            }
        }

        public static final class Divine extends Form {
            Divine(ModConfigSpec.Builder builder) {
                super(builder, "divine");
            }
        }

        public static final class Celestial extends Form {
            Celestial(ModConfigSpec.Builder builder) {
                super(builder, "celestial");
            }
        }

        public static final class Transcendent extends Form {
            Transcendent(ModConfigSpec.Builder builder) {
                super(builder, "transcendent");
            }
        }
    }
}
