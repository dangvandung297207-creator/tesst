package com.megafishing.fishing;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.anti.RayTraceManager;
import com.megafishing.anti.ValidationManager;
import com.megafishing.progression.IslandManager;
import com.megafishing.progression.ProgressionManager;
import com.megafishing.persistence.PlayerData;
import com.megafishing.rod.RodDefinition;
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
    private final IslandManager islandManager;
    private final ProgressionManager progressionManager;

    public CastController(MegaFishingPlugin plugin, RayTraceManager rayTraceManager, ValidationManager validationManager,
                          IslandManager islandManager, ProgressionManager progressionManager) {
        this.plugin = plugin;
        this.rayTraceManager = rayTraceManager;
        this.validationManager = validationManager;
        this.islandManager = islandManager;
        this.progressionManager = progressionManager;
    }

    public FishingSession cast(Player player, PlayerData data, RodDefinition rod, long currentTick) {
        Optional<Location> castLocation = rayTraceManager.findCastLocation(player, rod.getCastRange());
        if (castLocation.isEmpty()) {
            plugin.messages().send(player, "not-water");
            return null;
        }
        FishingZone zone = islandManager.findZone(castLocation.get());
        if (!validationManager.isValidCastLocation(castLocation.get(), zone)) {
            plugin.messages().send(player, zone == null ? "not-zone" : "not-water");
            return null;
        }
        if (!progressionManager.canAccessIsland(data, zone.getIslandId())) {
            plugin.messages().send(player, "not-zone");
            return null;
        }
        FishingSession session = new FishingSession(player.getUniqueId(), castLocation.get(), zone, rod, currentTick);
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
