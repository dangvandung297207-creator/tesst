package com.megafishing.fishing;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.anti.RayTraceManager;
import com.megafishing.anti.ValidationManager;
import com.megafishing.persistence.PlayerData;
import com.megafishing.progression.ProgressionManager;
import com.megafishing.rod.RodDefinition;
import com.megafishing.rod.RodManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class CastController {
    private final MegaFishingPlugin plugin;
    private final RayTraceManager rayTraceManager;
    private final ValidationManager validationManager;
    private final FishingEnvironmentManager environmentManager;
    private final ProgressionManager progressionManager;
    private final RodManager rodManager;

    public CastController(MegaFishingPlugin plugin, RayTraceManager rayTraceManager, ValidationManager validationManager,
                          FishingEnvironmentManager environmentManager, ProgressionManager progressionManager,
                          RodManager rodManager) {
        this.plugin = plugin;
        this.rayTraceManager = rayTraceManager;
        this.validationManager = validationManager;
        this.environmentManager = environmentManager;
        this.progressionManager = progressionManager;
        this.rodManager = rodManager;
    }

    public FishingSession cast(Player player, PlayerData data, RodDefinition rod, long currentTick) {
        Optional<Location> castLocation = rayTraceManager.findCastLocation(player, rod.getCastRange());
        if (castLocation.isEmpty()) {
            plugin.messages().send(player, "not-water");
            return null;
        }
        FishingEnvironment environment = environmentManager.resolve(castLocation.get(), rod, data);
        FishingZone zone = environment == null ? null : environment.getZone();
        if (!validationManager.isValidCastLocation(castLocation.get(), zone)) {
            plugin.messages().send(player, zone == null ? "not-zone" : "not-water");
            return null;
        }
        if (zone == null || zone.getEligibleFish().isEmpty()) {
            plugin.messages().send(player, "not-zone");
            return null;
        }
        if (!progressionManager.canAccessIsland(data, zone.getIslandId())) {
            plugin.messages().sendRaw(player, plugin.messages().raw("zone-locked").replace("{zone}", zone.getDisplayName()));
            return null;
        }
        if (!zone.getMinimumRodId().isBlank() && !rodManager.meetsRequirement(rod, zone.getMinimumRodId())) {
            var requiredRod = rodManager.getRegistry().getExact(zone.getMinimumRodId());
            plugin.messages().sendRaw(player, plugin.messages().raw("zone-rod-required")
                    .replace("{zone}", zone.getDisplayName())
                    .replace("{rod}", requiredRod == null ? zone.getMinimumRodId() : requiredRod.getDisplayName()));
            return null;
        }
        FishingSession session = new FishingSession(player.getUniqueId(), castLocation.get(), zone, environment, rod, currentTick);
        session.setBobberDisplay(spawnBobber(castLocation.get()));
        session.setState(FishingState.WAITING_BITE);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0F, 1.0F);
        return session;
    }

    private ItemDisplay spawnBobber(Location location) {
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.RED_DYE));
            display.setPersistent(false);
        });
    }
}
