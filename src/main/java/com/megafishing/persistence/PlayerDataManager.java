package com.megafishing.persistence;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.progression.ProgressionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final MegaFishingPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ProgressionManager progressionManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();

    public PlayerDataManager(MegaFishingPlugin plugin, DatabaseManager databaseManager, ProgressionManager progressionManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.progressionManager = progressionManager;
    }

    public void loadAsync(Player player) {
        UUID uuid = player.getUniqueId();
        if (cache.containsKey(uuid) || loading.contains(uuid)) {
            return;
        }
        loading.add(uuid);
        databaseManager.loadPlayer(uuid, () -> createDefault(uuid)).thenAccept(data -> Bukkit.getScheduler().runTask(plugin, () -> {
            loading.remove(uuid);
            if (!player.isOnline()) {
                return;
            }
            progressionManager.applyDefaultUnlocks(data);
            if (plugin.petManager().normalizeEquippedPets(data)) {
                saveAsync(data);
            }
            cache.put(uuid, data);
            if (plugin.onboardingManager() != null) {
                plugin.onboardingManager().handleProfileLoaded(player, data);
            }
        })).exceptionally(throwable -> {
            loading.remove(uuid);
            plugin.getLogger().warning("[MEGA-FISHING] Unexpected async load failure for " + uuid + ": " + throwable.getMessage());
            return null;
        });
    }

    public PlayerData createDefault(UUID uuid) {
        PlayerData data = new PlayerData(uuid, 0L, "wooden", plugin.getConfig().getInt("bag.default-capacity", 10),
                Math.max(1, plugin.getConfig().getInt("pets.default-slots", 2)),
                new java.util.LinkedHashSet<>(), new ArrayList<>(), new java.util.LinkedHashSet<>(), new ArrayList<>(), new PlayerStatistics(), false);
        progressionManager.applyDefaultUnlocks(data);
        return data;
    }

    public PlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public boolean isLoading(UUID uuid) {
        return loading.contains(uuid);
    }

    public void saveAsync(PlayerData data) {
        if (data != null) {
            databaseManager.savePlayer(data);
        }
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            saveAsync(data);
        }
        loading.remove(uuid);
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            saveAsync(data);
        }
    }

    public int loadedCount() {
        return cache.size();
    }

    public List<PlayerData> allLoaded() {
        return Collections.unmodifiableList(new ArrayList<>(cache.values()));
    }
}
