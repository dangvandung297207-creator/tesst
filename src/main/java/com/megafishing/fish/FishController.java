package com.megafishing.fish;

import com.megafishing.anti.RayTraceManager;
import com.megafishing.fishing.FishingEnvironment;
import com.megafishing.fishing.FishingZone;
import com.megafishing.visual.DisplayFishVisual;
import com.megafishing.visual.FishVisual;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FishController {
    private final UUID id = UUID.randomUUID();
    private final FishDefinition definition;
    private final FishingZone zone;
    private final FishingEnvironment environment;
    private final Location anchor;
    private final FishVisual visual;
    private final Map<FishPhase, FishPhaseModifiers> phaseModifiers;
    private final Random random = new Random();
    private final double maxHealth;
    private final double weight;
    private final FishSizeTier sizeTier;
    private final double visualScale;
    private Vector position;
    private Vector velocity = new Vector();
    private Vector direction = new Vector(1, 0, 0);
    private Vector target;
    private double health;
    private FishState state = FishState.SWIM;
    private FishPhase phase = FishPhase.NORMAL;
    private FishPhase pendingPhaseChange;
    private long nextDirectionChangeTick;
    private long stuckTicks;
    private Vector lastStablePosition;

    public FishController(FishDefinition definition, FishingZone zone, FishingEnvironment environment, Location anchor,
                          double weight, double maxHealth, Map<FishPhase, FishPhaseModifiers> phaseModifiers,
                          FishSizeTier sizeTier, double visualScale) {
        this.definition = definition;
        this.zone = zone;
        this.environment = environment;
        this.anchor = anchor.clone();
        this.weight = weight;
        this.maxHealth = maxHealth;
        this.phaseModifiers = phaseModifiers;
        this.sizeTier = sizeTier;
        this.visualScale = visualScale;
        this.health = maxHealth;
        this.position = anchor.toVector().add(new Vector(0.0D, -0.8D, 0.0D));
        this.target = this.position.clone();
        this.lastStablePosition = this.position.clone();
        this.visual = new DisplayFishVisual(definition);
    }

    public void spawnVisual() {
        visual.spawn(getLocation(), direction, visualScale(), animationKey());
    }

    public void update(Player player, RayTraceManager rayTraceManager, long tick) {
        World world = player.getWorld();
        if (tick >= nextDirectionChangeTick || target.distanceSquared(position) < 0.08D) {
            chooseNewDirection(player, tick, rayTraceManager);
        }
        double speed = effectiveSpeed();
        Vector desired = target.clone().subtract(position);
        if (desired.lengthSquared() > 0.0001D) {
            desired.normalize().multiply(speed);
        }
        velocity.multiply(0.82D).add(desired.multiply(0.18D));
        if (velocity.lengthSquared() > speed * speed) {
            velocity.normalize().multiply(speed);
        }
        Vector next = position.clone().add(velocity);
        if (!rayTraceManager.isWater(world, next)) {
            chooseNewDirection(player, tick, rayTraceManager);
            next = position.clone();
            velocity.multiply(0.2D);
        }
        position = next;
        if (velocity.lengthSquared() > 0.0001D) {
            direction = velocity.clone().normalize();
        }
        if (position.distanceSquared(lastStablePosition) < 0.01D && state != FishState.EXHAUSTED) {
            stuckTicks++;
        } else {
            stuckTicks = 0L;
            lastStablePosition = position.clone();
        }
        visual.update(getLocation(), direction, visualScale(), animationKey());
        updatePhase();
    }

    public void chooseNewDirection(Player player, long tick, RayTraceManager rayTraceManager) {
        FishState[] states = state == FishState.EXHAUSTED
                ? new FishState[]{FishState.EXHAUSTED}
                : chooseWeightedStatePool();
        this.state = states[random.nextInt(states.length)];
        double min = definition.getDirectionChangeMin();
        double max = definition.getDirectionChangeMax();
        double turnRateMultiplier = getPhaseModifiers().turnRateMultiplier();
        double delaySeconds = (min + (random.nextDouble() * (max - min))) / Math.max(0.25D, turnRateMultiplier);
        this.nextDirectionChangeTick = tick + Math.round(delaySeconds * 20.0D);

        Vector away = position.clone().subtract(player.getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 0.01D) {
            away = player.getLocation().getDirection().clone().setY(0).multiply(-1);
        }
        if (away.lengthSquared() < 0.01D) {
            away = new Vector(1, 0, 0);
        }
        away.normalize();
        Vector right = new Vector(-away.getZ(), 0, away.getX()).normalize();
        Vector offset = switch (state) {
            case TURN_LEFT -> right.clone().multiply(-1.5D - random.nextDouble() * 2.2D);
            case TURN_RIGHT -> right.clone().multiply(1.5D + random.nextDouble() * 2.2D);
            case DIVE -> away.clone().multiply(1.5D + random.nextDouble() * 1.5D).setY(-0.8D - random.nextDouble());
            case SURGE -> away.clone().multiply(3.0D + random.nextDouble() * 2.5D);
            case SHAKE -> new Vector((random.nextDouble() - 0.5D) * 2.4D, -0.25D + random.nextDouble() * 0.5D, (random.nextDouble() - 0.5D) * 2.4D);
            case EXHAUSTED -> away.clone().multiply(0.6D).setY(-0.2D);
            default -> away.clone().multiply(1.4D + random.nextDouble() * 2.0D);
        };
        this.target = rayTraceManager.findNearestWater(anchor.getWorld(), position.clone().add(offset), anchor.toVector(), Math.max(2.8D, zone.getRadius() > 0 ? Math.min(5.5D, zone.getRadius()) : 5.5D));
    }

    private double effectiveSpeed() {
        double multiplier = getPhaseModifiers().speedMultiplier();
        double stateMultiplier = switch (state) {
            case SURGE -> 1.55D;
            case SHAKE -> 1.20D;
            case EXHAUSTED -> 0.35D;
            default -> 1.0D;
        };
        return definition.getSwimSpeed() * multiplier * stateMultiplier;
    }

    private void updatePhase() {
        double percent = health / maxHealth;
        FishPhase newPhase;
        if (percent <= 0.30D) {
            newPhase = FishPhase.FRENZY;
        } else if (percent <= 0.70D) {
            newPhase = FishPhase.ENRAGED;
        } else {
            newPhase = FishPhase.NORMAL;
        }
        if (newPhase != phase) {
            phase = newPhase;
            pendingPhaseChange = newPhase;
        }
    }

    public void hit(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.45F, 1.5F);
        player.getWorld().spawnParticle(Particle.CRIT, getLocation(), 2, 0.15D, 0.15D, 0.15D, 0.01D);
    }

    public void boostAwayFrom(Location source, double strength) {
        if (source == null) {
            return;
        }
        Vector away = position.clone().subtract(source.toVector());
        if (away.lengthSquared() < 0.0001D) {
            away = direction.clone();
        }
        if (away.lengthSquared() < 0.0001D) {
            away = new Vector(1, 0, 0);
        }
        this.velocity.add(away.normalize().multiply(strength));
    }

    public FishPhase consumePhaseChange() {
        FishPhase value = pendingPhaseChange;
        pendingPhaseChange = null;
        return value;
    }

    public UUID getId() { return id; }
    public FishDefinition getDefinition() { return definition; }
    public FishingZone getZone() { return zone; }
    public FishingEnvironment getEnvironment() { return environment; }
    public Location getAnchor() { return anchor.clone(); }
    public Location getLocation() { return position.toLocation(anchor.getWorld()); }
    public Vector getDirection() { return direction.clone(); }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public double getWeight() { return weight; }
    public FishState getState() { return state; }
    public FishPhase getPhase() { return phase; }
    public FishSizeTier getSizeTier() { return sizeTier; }
    public long getStuckTicks() { return stuckTicks; }
    public boolean isVisualAlive() { return visual.isValid(); }
    public double getCurrentPullMultiplier() { return getPhaseModifiers().pullMultiplier(); }
    public double getVisualScale() { return visualScale(); }

    public void setState(FishState state) { this.state = state; }
    public void setHealth(double health) { this.health = Math.max(0.0D, Math.min(maxHealth, health)); }
    public void setPosition(Vector position) { this.position = position.clone(); }
    public void resetStuckTicks() { this.stuckTicks = 0L; this.lastStablePosition = position.clone(); }
    public void removeVisual() { visual.remove(); }

    private FishPhaseModifiers getPhaseModifiers() {
        FishPhaseModifiers modifiers = phaseModifiers.get(phase);
        return modifiers == null ? new FishPhaseModifiers(1.0D, 1.0D, 1.0D) : modifiers;
    }

    private FishState[] chooseWeightedStatePool() {
        return switch (phase) {
            case ENRAGED -> new FishState[]{FishState.TURN_LEFT, FishState.TURN_LEFT, FishState.TURN_RIGHT, FishState.TURN_RIGHT, FishState.SWIM, FishState.SWIM, FishState.DIVE, FishState.SURGE, FishState.SHAKE};
            case FRENZY -> new FishState[]{FishState.TURN_LEFT, FishState.TURN_RIGHT, FishState.SWIM, FishState.DIVE, FishState.DIVE, FishState.SURGE, FishState.SURGE, FishState.SHAKE, FishState.SHAKE};
            default -> new FishState[]{FishState.TURN_LEFT, FishState.TURN_LEFT, FishState.TURN_RIGHT, FishState.TURN_RIGHT, FishState.TURN_RIGHT, FishState.SWIM, FishState.SWIM, FishState.SWIM, FishState.DIVE, FishState.DIVE, FishState.SURGE, FishState.SHAKE};
        };
    }

    private double visualScale() {
        return visualScale;
    }

    private String animationKey() {
        return switch (state) {
            case SURGE -> definition.getModel().animation("surge", "surge");
            case TURN_LEFT, TURN_RIGHT -> definition.getModel().animation("turn", "turn");
            case EXHAUSTED -> definition.getModel().animation("exhausted", "exhausted");
            default -> definition.getModel().animation("idle", "swim");
        };
    }
}
