package com.megafishing.fishing;

import com.megafishing.fish.FishController;
import com.megafishing.rod.RodDefinition;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;

import java.util.UUID;

public class FishingSession {
    private final UUID playerId;
    private FishingState state;
    private final Location castLocation;
    private final FishingZone zone;
    private final FishingEnvironment environment;
    private final RodDefinition rod;
    private FishController fish;
    private double tension;
    private final long startTick;
    private long biteTick;
    private long biteReactionEndTick;
    private long skillCooldownEndTick;
    private long exhaustedUntilTick;
    private long lastUiTick;
    private long lastRayTraceTick;
    private long lastLineTick;
    private long lastDebugTick;
    private int obstructionTicks;
    private boolean obstructed;
    private boolean tooFar;
    private ItemDisplay bobberDisplay;

    public FishingSession(UUID playerId, Location castLocation, FishingZone zone, FishingEnvironment environment, RodDefinition rod, long startTick) {
        this.playerId = playerId;
        this.castLocation = castLocation.clone();
        this.zone = zone;
        this.environment = environment;
        this.rod = rod;
        this.startTick = startTick;
        this.state = FishingState.CASTING;
    }

    public UUID getPlayerId() { return playerId; }
    public FishingState getState() { return state; }
    public void setState(FishingState state) { this.state = state; }
    public Location getCastLocation() { return castLocation.clone(); }
    public FishingZone getZone() { return zone; }
    public FishingEnvironment getEnvironment() { return environment; }
    public RodDefinition getRod() { return rod; }
    public FishController getFish() { return fish; }
    public void setFish(FishController fish) { this.fish = fish; }
    public double getTension() { return tension; }
    public void setTension(double tension) { this.tension = tension; }
    public long getStartTick() { return startTick; }
    public long getBiteTick() { return biteTick; }
    public void setBiteTick(long biteTick) { this.biteTick = biteTick; }
    public long getBiteReactionEndTick() { return biteReactionEndTick; }
    public void setBiteReactionEndTick(long biteReactionEndTick) { this.biteReactionEndTick = biteReactionEndTick; }
    public long getSkillCooldownEndTick() { return skillCooldownEndTick; }
    public void setSkillCooldownEndTick(long skillCooldownEndTick) { this.skillCooldownEndTick = skillCooldownEndTick; }
    public long getExhaustedUntilTick() { return exhaustedUntilTick; }
    public void setExhaustedUntilTick(long exhaustedUntilTick) { this.exhaustedUntilTick = exhaustedUntilTick; }
    public long getLastUiTick() { return lastUiTick; }
    public void setLastUiTick(long lastUiTick) { this.lastUiTick = lastUiTick; }
    public long getLastRayTraceTick() { return lastRayTraceTick; }
    public void setLastRayTraceTick(long lastRayTraceTick) { this.lastRayTraceTick = lastRayTraceTick; }
    public long getLastLineTick() { return lastLineTick; }
    public void setLastLineTick(long lastLineTick) { this.lastLineTick = lastLineTick; }
    public long getLastDebugTick() { return lastDebugTick; }
    public void setLastDebugTick(long lastDebugTick) { this.lastDebugTick = lastDebugTick; }
    public int getObstructionTicks() { return obstructionTicks; }
    public void setObstructionTicks(int obstructionTicks) { this.obstructionTicks = obstructionTicks; }
    public boolean isObstructed() { return obstructed; }
    public void setObstructed(boolean obstructed) { this.obstructed = obstructed; }
    public boolean isTooFar() { return tooFar; }
    public void setTooFar(boolean tooFar) { this.tooFar = tooFar; }
    public ItemDisplay getBobberDisplay() { return bobberDisplay; }
    public void setBobberDisplay(ItemDisplay bobberDisplay) { this.bobberDisplay = bobberDisplay; }
}
