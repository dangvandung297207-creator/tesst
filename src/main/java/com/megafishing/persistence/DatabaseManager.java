package com.megafishing.persistence;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.fish.FishRarity;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private final MegaFishingPlugin plugin;
    private final File databaseFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "MegaFishing-SQLite"));
    private Connection connection;

    public DatabaseManager(MegaFishingPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "megafishing.db");
    }

    public void init() throws Exception {
        if (databaseFile.getParentFile() != null && !databaseFile.getParentFile().exists()) {
            databaseFile.getParentFile().mkdirs();
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, coins INTEGER NOT NULL, rod_id TEXT NOT NULL, bag_capacity INTEGER NOT NULL, max_pet_slots INTEGER NOT NULL, tutorial_completed INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pets (player_uuid TEXT NOT NULL, pet_id TEXT NOT NULL, equipped INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS statistics (player_uuid TEXT PRIMARY KEY, total_catches INTEGER NOT NULL, total_weight REAL NOT NULL, largest_fish REAL NOT NULL, largest_fish_id TEXT NOT NULL, highest_value INTEGER NOT NULL, total_coins_earned INTEGER NOT NULL, fish_escaped INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS fish_bag (player_uuid TEXT NOT NULL, fish_id TEXT NOT NULL, weight REAL NOT NULL, rarity TEXT NOT NULL, value INTEGER NOT NULL, timestamp INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS islands (player_uuid TEXT NOT NULL, island_id TEXT NOT NULL)");
            try {
                statement.executeUpdate("ALTER TABLE players ADD COLUMN tutorial_completed INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {
            }
        }
    }

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid, java.util.function.Supplier<PlayerData> fallbackSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerData fallback = fallbackSupplier.get();
                long coins = fallback.getCoins();
                String rodId = fallback.getRodId();
                int bagCapacity = fallback.getBagCapacity();
                int maxPetSlots = fallback.getMaxPetSlots();
                boolean tutorialCompleted = fallback.isTutorialCompleted();
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            coins = result.getLong("coins");
                            rodId = result.getString("rod_id");
                            bagCapacity = result.getInt("bag_capacity");
                            maxPetSlots = result.getInt("max_pet_slots");
                            tutorialCompleted = result.getInt("tutorial_completed") == 1;
                        }
                    }
                }
                Set<String> ownedPets = new LinkedHashSet<>();
                List<String> equippedPets = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement("SELECT pet_id, equipped FROM pets WHERE player_uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            String petId = result.getString("pet_id");
                            ownedPets.add(petId);
                            if (result.getInt("equipped") == 1) {
                                equippedPets.add(petId);
                            }
                        }
                    }
                }
                Set<String> islands = new LinkedHashSet<>();
                try (PreparedStatement statement = connection.prepareStatement("SELECT island_id FROM islands WHERE player_uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            islands.add(result.getString("island_id"));
                        }
                    }
                }
                List<CaughtFishRecord> fishRecords = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM fish_bag WHERE player_uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            fishRecords.add(new CaughtFishRecord(
                                    result.getString("fish_id"),
                                    result.getDouble("weight"),
                                    FishRarity.valueOf(result.getString("rarity")),
                                    result.getLong("value"),
                                    result.getLong("timestamp")
                            ));
                        }
                    }
                }
                PlayerStatistics statistics = new PlayerStatistics();
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM statistics WHERE player_uuid = ?")) {
                    statement.setString(1, uuid.toString());
                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            statistics = PlayerStatistics.of(
                                    result.getLong("total_catches"),
                                    result.getDouble("total_weight"),
                                    result.getDouble("largest_fish"),
                                    result.getString("largest_fish_id"),
                                    result.getLong("highest_value"),
                                    result.getLong("total_coins_earned"),
                                    result.getLong("fish_escaped")
                            );
                        }
                    }
                }
                return new PlayerData(uuid, coins, rodId, bagCapacity, maxPetSlots, ownedPets, equippedPets, islands, fishRecords, statistics, tutorialCompleted);
            } catch (Exception exception) {
                plugin.getLogger().warning("[MEGA-FISHING] Failed to load player data for " + uuid + ": " + exception.getMessage());
                return fallbackSupplier.get();
            }
        }, executor);
    }

    public CompletableFuture<Void> savePlayer(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement upsertPlayer = connection.prepareStatement("INSERT INTO players(uuid, coins, rod_id, bag_capacity, max_pet_slots, tutorial_completed) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET coins = excluded.coins, rod_id = excluded.rod_id, bag_capacity = excluded.bag_capacity, max_pet_slots = excluded.max_pet_slots, tutorial_completed = excluded.tutorial_completed")) {
                    upsertPlayer.setString(1, data.getUuid().toString());
                    upsertPlayer.setLong(2, data.getCoins());
                    upsertPlayer.setString(3, data.getRodId());
                    upsertPlayer.setInt(4, data.getBagCapacity());
                    upsertPlayer.setInt(5, data.getMaxPetSlots());
                    upsertPlayer.setInt(6, data.isTutorialCompleted() ? 1 : 0);
                    upsertPlayer.executeUpdate();
                }
                try (PreparedStatement deletePets = connection.prepareStatement("DELETE FROM pets WHERE player_uuid = ?")) {
                    deletePets.setString(1, data.getUuid().toString());
                    deletePets.executeUpdate();
                }
                try (PreparedStatement insertPet = connection.prepareStatement("INSERT INTO pets(player_uuid, pet_id, equipped) VALUES (?, ?, ?)")) {
                    for (String petId : data.getOwnedPets()) {
                        insertPet.setString(1, data.getUuid().toString());
                        insertPet.setString(2, petId);
                        insertPet.setInt(3, data.getEquippedPets().contains(petId) ? 1 : 0);
                        insertPet.addBatch();
                    }
                    insertPet.executeBatch();
                }
                try (PreparedStatement deleteFish = connection.prepareStatement("DELETE FROM fish_bag WHERE player_uuid = ?")) {
                    deleteFish.setString(1, data.getUuid().toString());
                    deleteFish.executeUpdate();
                }
                try (PreparedStatement insertFish = connection.prepareStatement("INSERT INTO fish_bag(player_uuid, fish_id, weight, rarity, value, timestamp) VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (CaughtFishRecord record : data.getFishBag().contents()) {
                        insertFish.setString(1, data.getUuid().toString());
                        insertFish.setString(2, record.getFishId());
                        insertFish.setDouble(3, record.getWeight());
                        insertFish.setString(4, record.getRarity().name());
                        insertFish.setLong(5, record.getValue());
                        insertFish.setLong(6, record.getTimestamp());
                        insertFish.addBatch();
                    }
                    insertFish.executeBatch();
                }
                try (PreparedStatement deleteIslands = connection.prepareStatement("DELETE FROM islands WHERE player_uuid = ?")) {
                    deleteIslands.setString(1, data.getUuid().toString());
                    deleteIslands.executeUpdate();
                }
                try (PreparedStatement insertIsland = connection.prepareStatement("INSERT INTO islands(player_uuid, island_id) VALUES (?, ?)")) {
                    for (String islandId : data.getUnlockedIslands()) {
                        insertIsland.setString(1, data.getUuid().toString());
                        insertIsland.setString(2, islandId);
                        insertIsland.addBatch();
                    }
                    insertIsland.executeBatch();
                }
                try (PreparedStatement upsertStats = connection.prepareStatement("INSERT INTO statistics(player_uuid, total_catches, total_weight, largest_fish, largest_fish_id, highest_value, total_coins_earned, fish_escaped) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(player_uuid) DO UPDATE SET total_catches = excluded.total_catches, total_weight = excluded.total_weight, largest_fish = excluded.largest_fish, largest_fish_id = excluded.largest_fish_id, highest_value = excluded.highest_value, total_coins_earned = excluded.total_coins_earned, fish_escaped = excluded.fish_escaped")) {
                    upsertStats.setString(1, data.getUuid().toString());
                    upsertStats.setLong(2, data.getStatistics().getTotalCatches());
                    upsertStats.setDouble(3, data.getStatistics().getTotalWeight());
                    upsertStats.setDouble(4, data.getStatistics().getLargestFishWeight());
                    upsertStats.setString(5, data.getStatistics().getLargestFishId());
                    upsertStats.setLong(6, data.getStatistics().getHighestValue());
                    upsertStats.setLong(7, data.getStatistics().getTotalCoinsEarned());
                    upsertStats.setLong(8, data.getStatistics().getFishEscaped());
                    upsertStats.executeUpdate();
                }
                connection.commit();
                connection.setAutoCommit(true);
            } catch (Exception exception) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (Exception ignored) {
                }
                plugin.getLogger().warning("[MEGA-FISHING] Failed to save player data for " + data.getUuid() + ": " + exception.getMessage());
            }
        }, executor);
    }

    public void close() throws Exception {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public String status() {
        try {
            return connection != null && !connection.isClosed() ? "connected" : "closed";
        } catch (Exception exception) {
            return "error";
        }
    }
}
