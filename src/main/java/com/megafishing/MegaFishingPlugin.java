package com.megafishing;

import com.megafishing.anti.ExploitManager;
import com.megafishing.anti.RayTraceManager;
import com.megafishing.anti.ValidationManager;
import com.megafishing.command.MegaFishingCommand;
import com.megafishing.combat.DamageController;
import com.megafishing.combat.DirectionController;
import com.megafishing.combat.TensionManager;
import com.megafishing.combat.TugOfWarController;
import com.megafishing.economy.EconomyManager;
import com.megafishing.economy.RodShopManager;
import com.megafishing.economy.SellManager;
import com.megafishing.fish.FishManager;
import com.megafishing.fishing.BiteController;
import com.megafishing.fishing.CastController;
import com.megafishing.fishing.FishingManager;
import com.megafishing.persistence.DatabaseManager;
import com.megafishing.persistence.PlayerDataManager;
import com.megafishing.pet.PetManager;
import com.megafishing.pet.PetMenuManager;
import com.megafishing.progression.BagUpgradeManager;
import com.megafishing.progression.IslandManager;
import com.megafishing.progression.IslandMenuManager;
import com.megafishing.progression.PetSlotUpgradeManager;
import com.megafishing.progression.ProgressionManager;
import com.megafishing.progression.QuestManager;
import com.megafishing.rod.RodManager;
import com.megafishing.skill.SkillController;
import com.megafishing.skill.SkillManager;
import com.megafishing.storage.StorageManager;
import com.megafishing.ui.ActionBarManager;
import com.megafishing.ui.BossBarManager;
import com.megafishing.ui.MessageManager;
import com.megafishing.ui.OnboardingManager;
import com.megafishing.validation.ConfigValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;

public class MegaFishingPlugin extends JavaPlugin {
    private MessageManager messages;
    private RodManager rodManager;
    private SkillManager skillManager;
    private PetManager petManager;
    private FishManager fishManager;
    private IslandManager islandManager;
    private QuestManager questManager;
    private ProgressionManager progressionManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;
    private SellManager sellManager;
    private RodShopManager rodShopManager;
    private BagUpgradeManager bagUpgradeManager;
    private PetSlotUpgradeManager petSlotUpgradeManager;
    private IslandMenuManager islandMenuManager;
    private PetMenuManager petMenuManager;
    private OnboardingManager onboardingManager;
    private StorageManager storageManager;
    private FishingManager fishingManager;
    private ConfigValidator configValidator;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureResources(List.of("messages.yml", "skills.yml", "rods.yml", "fish.yml", "pets.yml", "islands.yml"));

        this.messages = new MessageManager(this);
        this.rodManager = new RodManager(this);
        this.skillManager = new SkillManager();
        this.petManager = new PetManager(this);
        this.onboardingManager = new OnboardingManager(this);
        this.fishManager = new FishManager(this);
        this.islandManager = new IslandManager(this);
        this.questManager = new QuestManager();
        this.progressionManager = new ProgressionManager(islandManager);
        this.storageManager = new StorageManager();
        this.configValidator = new ConfigValidator(this);
        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.init();
            getLogger().info("[MEGA-FISHING] SQLite initialized.");
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize MegaFishing database", exception);
        }
        this.playerDataManager = new PlayerDataManager(this, databaseManager, progressionManager);
        this.economyManager = new EconomyManager(this, petManager);
        this.sellManager = new SellManager(this, economyManager, playerDataManager, messages);
        this.rodShopManager = new RodShopManager(this, rodManager, playerDataManager, messages);
        this.bagUpgradeManager = new BagUpgradeManager(this, playerDataManager, messages);
        this.petSlotUpgradeManager = new PetSlotUpgradeManager(this, playerDataManager, messages);
        this.islandMenuManager = new IslandMenuManager(this, islandManager, progressionManager, playerDataManager, messages);
        this.petMenuManager = new PetMenuManager(this, petManager, playerDataManager, messages);

        reloadPlugin();

        RayTraceManager rayTraceManager = new RayTraceManager();
        ValidationManager validationManager = new ValidationManager();
        ExploitManager exploitManager = new ExploitManager();
        CastController castController = new CastController(this, rayTraceManager, validationManager, islandManager, progressionManager);
        BiteController biteController = new BiteController(this, fishManager);
        DirectionController directionController = new DirectionController(this);
        TensionManager tensionManager = new TensionManager(this);
        DamageController damageController = new DamageController(this);
        TugOfWarController tugOfWarController = new TugOfWarController(this, directionController, tensionManager, damageController);
        SkillController skillController = new SkillController();
        ActionBarManager actionBarManager = new ActionBarManager();
        BossBarManager bossBarManager = new BossBarManager();

        this.fishingManager = new FishingManager(this, playerDataManager, rodManager, castController, biteController, rayTraceManager, validationManager,
                exploitManager, tugOfWarController, skillManager, skillController, economyManager, storageManager, messages, actionBarManager, bossBarManager);

        Bukkit.getPluginManager().registerEvents(fishingManager, this);
        Bukkit.getPluginManager().registerEvents(storageManager, this);
        Bukkit.getPluginManager().registerEvents(sellManager, this);
        Bukkit.getPluginManager().registerEvents(rodShopManager, this);
        Bukkit.getPluginManager().registerEvents(bagUpgradeManager, this);
        Bukkit.getPluginManager().registerEvents(petSlotUpgradeManager, this);
        Bukkit.getPluginManager().registerEvents(islandMenuManager, this);
        Bukkit.getPluginManager().registerEvents(petMenuManager, this);

        PluginCommand command = getCommand("megafishing");
        if (command != null) {
            MegaFishingCommand executor = new MegaFishingCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.loadAsync(player);
        }
        fishingManager.start();
        startAutosave();
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        if (fishingManager != null) {
            fishingManager.shutdown();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (databaseManager != null) {
            try {
                databaseManager.close();
            } catch (Exception exception) {
                getLogger().warning("[MEGA-FISHING] Failed to close database cleanly: " + exception.getMessage());
            }
        }
    }

    public void reloadPlugin() {
        if (fishingManager != null) {
            fishingManager.cancelAllSessionsForReload();
        }
        reloadConfig();
        messages.reload();
        rodManager.reload();
        skillManager.load(new File(getDataFolder(), "skills.yml"), getLogger());
        petManager.reload();
        fishManager.reload();
        islandManager.reload();
        questManager.reload();
        getLogger().info("[MEGA-FISHING] Loaded " + fishManager.getRegistry().values().size() + " fish.");
        getLogger().info("[MEGA-FISHING] Loaded " + rodManager.getRegistry().values().size() + " rods.");
        validateDefinitions();
        configValidator.validate();
        getLogger().info("[MEGA-FISHING] Loaded " + petManager.getRegistry().values().size() + " pets.");
        getLogger().info("[MEGA-FISHING] Loaded " + islandManager.islands().size() + " islands.");
        if (autosaveTask != null) {
            startAutosave();
        }
    }

    private void validateDefinitions() {
        if (fishManager.getRegistry().values().isEmpty()) {
            getLogger().warning("[MEGA-FISHING] No fish loaded. Fishing will not work.");
        }
        if (rodManager.getRegistry().values().isEmpty()) {
            getLogger().warning("[MEGA-FISHING] No rods loaded. Default rod fallback will fail.");
        }
        rodManager.getRegistry().values().forEach(rod -> {
            if (skillManager.get(rod.getSkillId()) == null) {
                getLogger().warning("[MEGA-FISHING] Rod '" + rod.getId() + "' references missing skill '" + rod.getSkillId() + "'.");
            }
        });
        islandManager.islands().forEach(island -> island.getZones().forEach(zone -> zone.getEligibleFish().keySet().forEach(fishId -> {
            if (fishManager.getRegistry().get(fishId) == null) {
                getLogger().warning("[MEGA-FISHING] Zone '" + zone.getId() + "' references missing fish '" + fishId + "'.");
            }
        })));
    }

    private void ensureResources(List<String> names) {
        for (String name : names) {
            File file = new File(getDataFolder(), name);
            if (!file.exists()) {
                saveResource(name, false);
            }
        }
    }

    private void startAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
        long interval = Math.max(20L, getConfig().getLong("save.autosave-seconds", 120L) * 20L);
        autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> playerDataManager.saveAll(), interval, interval);
    }

    public MessageManager messages() { return messages; }
    public RodManager rodManager() { return rodManager; }
    public SkillManager skillManager() { return skillManager; }
    public PetManager petManager() { return petManager; }
    public FishManager fishManager() { return fishManager; }
    public IslandManager islandManager() { return islandManager; }
    public PlayerDataManager playerDataManager() { return playerDataManager; }
    public EconomyManager economyManager() { return economyManager; }
    public SellManager sellManager() { return sellManager; }
    public RodShopManager rodShopManager() { return rodShopManager; }
    public BagUpgradeManager bagUpgradeManager() { return bagUpgradeManager; }
    public PetSlotUpgradeManager petSlotUpgradeManager() { return petSlotUpgradeManager; }
    public IslandMenuManager islandMenuManager() { return islandMenuManager; }
    public PetMenuManager petMenuManager() { return petMenuManager; }
    public OnboardingManager onboardingManager() { return onboardingManager; }
    public ProgressionManager progressionManager() { return progressionManager; }
    public StorageManager storageManager() { return storageManager; }
    public FishingManager fishingManager() { return fishingManager; }
    public DatabaseManager databaseManager() { return databaseManager; }
}
