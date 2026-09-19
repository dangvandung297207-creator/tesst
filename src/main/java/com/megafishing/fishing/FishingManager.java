package com.megafishing.fishing;

import com.megafishing.MegaFishingPlugin;
import com.megafishing.anti.ExploitManager;
import com.megafishing.anti.RayTraceManager;
import com.megafishing.anti.ValidationManager;
import com.megafishing.combat.CombatTick;
import com.megafishing.combat.TugOfWarController;
import com.megafishing.economy.EconomyManager;
import com.megafishing.persistence.CaughtFishRecord;
import com.megafishing.persistence.PlayerData;
import com.megafishing.persistence.PlayerDataManager;
import com.megafishing.rod.RodDefinition;
import com.megafishing.rod.RodManager;
import com.megafishing.skill.SkillController;
import com.megafishing.skill.SkillDefinition;
import com.megafishing.skill.SkillManager;
import com.megafishing.storage.StorageManager;
import com.megafishing.ui.ActionBarManager;
import com.megafishing.ui.BossBarManager;
import com.megafishing.ui.MessageManager;
import com.megafishing.util.Text;
import com.megafishing.visual.LineVisual;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FishingManager implements Listener {
    private final MegaFishingPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final RodManager rodManager;
    private final CastController castController;
    private final BiteController biteController;
    private final RayTraceManager rayTraceManager;
    private final ValidationManager validationManager;
    private final ExploitManager exploitManager;
    private final TugOfWarController tugOfWarController;
    private final SkillManager skillManager;
    private final SkillController skillController;
    private final EconomyManager economyManager;
    private final StorageManager storageManager;
    private final MessageManager messages;
    private final ActionBarManager actionBarManager;
    private final BossBarManager bossBarManager;
    private final LineVisual lineVisual = new LineVisual();
    private final Map<UUID, FishingSession> sessions = new HashMap<>();
    private BukkitTask tickTask;
    private long currentTick;

    public FishingManager(MegaFishingPlugin plugin, PlayerDataManager playerDataManager, RodManager rodManager, CastController castController,
                          BiteController biteController, RayTraceManager rayTraceManager, ValidationManager validationManager,
                          ExploitManager exploitManager, TugOfWarController tugOfWarController, SkillManager skillManager,
                          SkillController skillController, EconomyManager economyManager, StorageManager storageManager,
                          MessageManager messages, ActionBarManager actionBarManager, BossBarManager bossBarManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.rodManager = rodManager;
        this.castController = castController;
        this.biteController = biteController;
        this.rayTraceManager = rayTraceManager;
        this.validationManager = validationManager;
        this.exploitManager = exploitManager;
        this.tugOfWarController = tugOfWarController;
        this.skillManager = skillManager;
        this.skillController = skillController;
        this.economyManager = economyManager;
        this.storageManager = storageManager;
        this.messages = messages;
        this.actionBarManager = actionBarManager;
        this.bossBarManager = bossBarManager;
    }

    public void start() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (UUID uuid : new java.util.ArrayList<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            cancelSession(player, sessions.get(uuid), FishingState.CANCELLED, false, null);
        }
        sessions.clear();
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public int getActiveFishCount() {
        int count = 0;
        for (FishingSession session : sessions.values()) {
            if (session.getFish() != null) {
                count++;
            }
        }
        return count;
    }

    public int getVisualEntityCount() {
        int count = 0;
        for (FishingSession session : sessions.values()) {
            if (session.getBobberDisplay() != null && session.getBobberDisplay().isValid()) {
                count++;
            }
            if (session.getFish() != null && session.getFish().isVisualAlive()) {
                count++;
            }
        }
        return count;
    }

    public FishingSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadAsync(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSession(event.getPlayer(), sessions.remove(event.getPlayer().getUniqueId()), FishingState.CANCELLED, false, null);
        playerDataManager.unload(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        cancelSession(event.getPlayer(), sessions.remove(event.getPlayer().getUniqueId()), FishingState.CANCELLED, false, null);
        playerDataManager.unload(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cancelSession(event.getEntity(), sessions.remove(event.getEntity().getUniqueId()), FishingState.ESCAPED, true, messages.raw("escaped"));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cancelSession(event.getPlayer(), sessions.remove(event.getPlayer().getUniqueId()), FishingState.CANCELLED, true, messages.raw("cast-cancelled"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFish(PlayerFishEvent event) {
        if (!plugin.getConfig().getBoolean("replace-vanilla-fishing", true)) {
            return;
        }
        boolean holding = rodManager.isRod(event.getPlayer().getInventory().getItemInMainHand()) || rodManager.isRod(event.getPlayer().getInventory().getItemInOffHand());
        if (!holding && !sessions.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        if (event.getHook() != null && event.getHook().isValid()) {
            event.getHook().remove();
        }
        if (event.getCaught() instanceof Item item) {
            item.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("replace-vanilla-fishing", true)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!rodManager.isRod(item)) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData data = playerDataManager.get(player.getUniqueId());
        if (data == null) {
            messages.send(player, "profile-loading");
            return;
        }
        FishingSession existing = sessions.get(player.getUniqueId());
        if (existing != null) {
            handleExistingClick(player, data, existing);
            return;
        }
        if (!validationManager.canStoreFish(data)) {
            messages.send(player, "bag-full");
            return;
        }
        RodDefinition rod = rodManager.resolve(item);
        if (rod == null) {
            messages.sendRaw(player, "&cNo valid rod definitions are loaded.");
            return;
        }
        FishingSession session = castController.cast(player, data, rod, currentTick);
        if (session == null) {
            return;
        }
        biteController.schedule(session, currentTick);
        sessions.put(player.getUniqueId(), session);
        messages.send(player, "cast-started");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!"SWAP_HANDS".equalsIgnoreCase(plugin.getConfig().getString("fishing.active-skill.method", "SWAP_HANDS"))) {
            return;
        }
        FishingSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        activateSkill(event.getPlayer(), session);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDropSkill(PlayerDropItemEvent event) {
        if (!"DROP".equalsIgnoreCase(plugin.getConfig().getString("fishing.active-skill.method", "SWAP_HANDS"))) {
            return;
        }
        FishingSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        activateSkill(event.getPlayer(), session);
    }

    private void tick() {
        currentTick++;
        for (Map.Entry<UUID, FishingSession> entry : new HashMap<>(sessions).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            FishingSession session = entry.getValue();
            if (player == null || !player.isOnline()) {
                cancelSession(null, session, FishingState.CANCELLED, false, null);
                sessions.remove(entry.getKey());
                continue;
            }
            if (!rodManager.isRod(player.getInventory().getItemInMainHand())) {
                cancelSession(player, session, FishingState.CANCELLED, true, messages.raw("cast-cancelled"));
                sessions.remove(entry.getKey());
                continue;
            }
            if (exploitManager.tooFar(player, session, plugin.getConfig().getDouble("fishing.safety.max-session-distance", 32.0D))) {
                cancelSession(player, session, FishingState.ESCAPED, true, messages.raw("escaped"));
                sessions.remove(entry.getKey());
                continue;
            }
            switch (session.getState()) {
                case WAITING_BITE -> updateWaiting(player, session);
                case BITE -> updateBite(player, session);
                case FIGHTING -> updateFight(player, session);
                case EXHAUSTED -> updateExhausted(player, session);
                case CAPTURED, CANCELLED, ESCAPED -> {
                    cancelSession(player, session, session.getState(), false, null);
                    sessions.remove(entry.getKey());
                }
                default -> {
                }
            }
        }
    }

    private void updateWaiting(Player player, FishingSession session) {
        bobBobber(session);
        if (biteController.shouldBite(session, currentTick)) {
            biteController.bite(player, session, currentTick);
            playDiscoveryFeedback(player, session);
            actionBarManager.send(player, "&6&l! FISH ON! &eRight click to hook it!");
        }
    }

    private void updateBite(Player player, FishingSession session) {
        bobBobber(session);
        actionBarManager.send(player, "&6&l! FISH ON! &eRight click to hook it!");
        if (currentTick >= session.getBiteReactionEndTick()) {
            cancelSession(player, session, FishingState.ESCAPED, true, messages.raw("missed-bite"));
            sessions.remove(player.getUniqueId());
        }
    }

    private void updateFight(Player player, FishingSession session) {
        if (session.getFish() == null) {
            cancelSession(player, session, FishingState.CANCELLED, true, messages.raw("cast-cancelled"));
            sessions.remove(player.getUniqueId());
            return;
        }
        session.getFish().update(player, rayTraceManager, currentTick);
        handlePhaseTransition(player, session);
        emitFightParticles(player, session);
        if (session.getFish().getStuckTicks() >= Math.round(plugin.getConfig().getDouble("fishing.safety.stuck-seconds", 5.0D) * 20.0D)) {
            session.getFish().chooseNewDirection(player, currentTick, rayTraceManager);
            session.getFish().resetStuckTicks();
        }

        if (currentTick - session.getLastRayTraceTick() >= plugin.getConfig().getLong("fishing.update.raytrace-interval-ticks", 4L)) {
            boolean obstructed = exploitManager.obstructed(player, session.getFish().getLocation(), rayTraceManager);
            boolean tooFar = player.getLocation().distanceSquared(session.getCastLocation()) > Math.pow(plugin.getConfig().getDouble("fishing.safety.max-session-distance", 32.0D), 2);
            session.setObstructed(obstructed);
            session.setTooFar(tooFar);
            session.setLastRayTraceTick(currentTick);
            if (obstructed) {
                session.setObstructionTicks(session.getObstructionTicks() + (int) plugin.getConfig().getLong("fishing.update.raytrace-interval-ticks", 4L));
            } else {
                session.setObstructionTicks(0);
            }
        }

        CombatTick combat = tugOfWarController.tick(player, session, session.getRod(), session.isObstructed(), session.isTooFar());
        if (currentTick % 10L == 0L) {
            session.getFish().hit(player);
        }
        long obstructionLimit = Math.round(plugin.getConfig().getDouble("fishing.safety.obstruction-grace-seconds", 3.0D) * 20.0D);
        if (session.getObstructionTicks() >= obstructionLimit) {
            cancelSession(player, session, FishingState.ESCAPED, true, messages.raw("line-obstructed"));
            sessions.remove(player.getUniqueId());
            return;
        }
        if (session.getTension() >= plugin.getConfig().getDouble("tension.break-point", 100.0D)) {
            performLineSnapEscape(player, session);
            cancelSession(player, session, FishingState.ESCAPED, true, messages.raw("escaped"));
            sessions.remove(player.getUniqueId());
            return;
        }
        if (session.getFish().getHealth() <= 0.0D) {
            startExhausted(player, session);
        }
        updateUi(player, session, combat);
        renderLine(player, session);
        debugSession(player, session, combat);
    }

    private void updateExhausted(Player player, FishingSession session) {
        if (session.getFish() == null) {
            cancelSession(player, session, FishingState.CANCELLED, false, null);
            sessions.remove(player.getUniqueId());
            return;
        }
        session.getFish().setState(com.megafishing.fish.FishState.EXHAUSTED);
        session.getFish().update(player, rayTraceManager, currentTick);
        player.getWorld().spawnParticle(Particle.BUBBLE, session.getFish().getLocation(), 4, 0.25D, 0.2D, 0.25D, 0.02D);
        tugOfWarController.decayExhausted(session);
        updateUi(player, session, null);
        renderLine(player, session);
        debugSession(player, session, null);
        if (currentTick >= session.getExhaustedUntilTick()) {
            cancelSession(player, session, FishingState.ESCAPED, true, messages.raw("escaped"));
            sessions.remove(player.getUniqueId());
        }
    }

    private void updateUi(Player player, FishingSession session, CombatTick combat) {
        if ((currentTick - session.getLastUiTick()) < plugin.getConfig().getLong("fishing.update.ui-interval-ticks", 4L)) {
            return;
        }
        session.setLastUiTick(currentTick);
        double safeMax = plugin.getConfig().getDouble("tension.safe-max", 60.0D);
        double powerMax = plugin.getConfig().getDouble("tension.power-max", 80.0D);
        bossBarManager.update(player, session, safeMax, powerMax);
        String tensionColor = session.getTension() > powerMax ? "&c" : session.getTension() >= safeMax ? "&6" : "&a";
        String skillText = session.getSkillCooldownEndTick() <= currentTick ? "&b[Q] POWER PULL READY" : "&b[Q] POWER PULL " + Text.number((session.getSkillCooldownEndTick() - currentTick) / 20.0D) + "s";
        String phaseText = session.getFish() == null ? "" : phaseColor(session.getFish().getPhase()) + session.getFish().getPhase().name();
        String stateText = session.getState() == FishingState.EXHAUSTED ? "&aEXHAUSTED" : combat == null ? "&bFIGHT" : "&f" + combat.counterResult().arrow();
        String action = "&fTENSION " + tensionColor + Text.bar(session.getTension(), plugin.getConfig().getDouble("tension.break-point", 100.0D), 16) + " &7" + Math.round(session.getTension()) + "% &8| " + stateText + " &8| " + phaseText + " &8| " + skillText;
        if (tugOfWarController.isPowerWindow(session)) {
            action += " &8| &e⚡ POWER WINDOW";
        } else if (tugOfWarController.isDanger(session)) {
            action += " &8| &c⚠ LINE CRITICAL";
            if (currentTick % Math.max(1L, plugin.getConfig().getLong("tension.danger-warning-sound-interval-ticks", 20L)) == 0L) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.7F, 0.6F);
            }
        }
        actionBarManager.send(player, action);
    }

    private void renderLine(Player player, FishingSession session) {
        if (session.getFish() == null) {
            return;
        }
        if ((currentTick - session.getLastLineTick()) < plugin.getConfig().getLong("fishing.update.line-interval-ticks", 5L)) {
            return;
        }
        session.setLastLineTick(currentTick);
        lineVisual.render(player, session.getCastLocation(), session.getFish().getLocation());
    }

    private void handleExistingClick(Player player, PlayerData data, FishingSession session) {
        switch (session.getState()) {
            case WAITING_BITE -> {
                cancelSession(player, session, FishingState.CANCELLED, true, messages.raw("cast-cancelled"));
                sessions.remove(player.getUniqueId());
            }
            case BITE -> startFight(player, session);
            case FIGHTING -> tugOfWarController.reduceTension(session, plugin.getConfig().getDouble("tension.reel-reduction", 3.0D));
            case EXHAUSTED -> capture(player, data, session);
            default -> {
            }
        }
    }

    private void startFight(Player player, FishingSession session) {
        session.setState(FishingState.FIGHTING);
        session.setTension(plugin.getConfig().getDouble("tension.start", 25.0D));
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0F, 1.2F);
    }

    private void activateSkill(Player player, FishingSession session) {
        RodDefinition rod = session.getRod();
        SkillDefinition skill = skillManager.get(rod.getSkillId());
        SkillController.Result result = skillController.activate(session, skill, rod, currentTick, tugOfWarController);
        if (!result.success()) {
            if ("cooldown".equals(result.reasonKey())) {
                messages.sendRaw(player, messages.raw("skill-cooldown").replace("{seconds}", String.valueOf(result.cooldownSeconds())));
            }
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.75F, 1.8F);
        if (session.getFish() != null) {
            session.getFish().boostAwayFrom(player.getEyeLocation(), plugin.getConfig().getDouble("combat.skill-recoil-strength", 0.7D));
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, session.getFish().getLocation(), 10, 0.25D, 0.25D, 0.25D, 0.03D);
        }
        messages.sendRaw(player, messages.raw("skill-used")
                .replace("{damage}", Text.number(result.damage()))
                .replace("{tension}", Text.number(result.tensionReduction())));
        if (session.getFish() != null && session.getFish().getHealth() <= 0.0D && session.getState() == FishingState.FIGHTING) {
            startExhausted(player, session);
        }
    }

    private void startExhausted(Player player, FishingSession session) {
        session.setState(FishingState.EXHAUSTED);
        session.setExhaustedUntilTick(currentTick + Math.round(plugin.getConfig().getDouble("fishing.safety.exhausted-capture-window", 2.0D) * 20.0D));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.25F);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, session.getFish().getLocation(), 6, 0.2D, 0.25D, 0.2D, 0.01D);
        messages.send(player, "exhausted");
    }

    private void capture(Player player, PlayerData data, FishingSession session) {
        if (data.getFishBag().isFull()) {
            messages.send(player, "bag-full");
            return;
        }
        long value = economyManager.calculateValue(data, session.getFish().getDefinition(), session.getFish().getWeight(), session.getFish().getDefinition().getRarity());
        CaughtFishRecord record = new CaughtFishRecord(session.getFish().getDefinition().getId(), session.getFish().getWeight(), session.getFish().getDefinition().getRarity(), value, System.currentTimeMillis());
        if (!storageManager.addFish(data, record)) {
            messages.send(player, "bag-full");
            return;
        }
        data.getStatistics().incrementCatch();
        data.getStatistics().addWeight(record.getWeight());
        data.getStatistics().updateLargest(record.getFishId(), record.getWeight());
        data.getStatistics().updateHighestValue(record.getValue());
        messages.sendRaw(player, messages.raw("captured")
                .replace("{fish}", session.getFish().getDefinition().getDisplayName())
                .replace("{weight}", Text.number(record.getWeight())));
        showCatchReveal(player, session, value);
        if (record.getRarity().ordinal() >= com.megafishing.fish.FishRarity.LEGENDARY.ordinal()) {
            String titleKey = record.getRarity() == com.megafishing.fish.FishRarity.SECRET ? "secret-catch-title" : "legendary-catch-title";
            messages.title(player, messages.raw(titleKey), session.getFish().getDefinition().getDisplayName() + " &7- &6" + Text.number(value));
        } else if (record.getRarity().ordinal() >= com.megafishing.fish.FishRarity.RARE.ordinal()) {
            messages.title(player, messages.raw("rare-catch-title"), session.getFish().getDefinition().getDisplayName() + " &7- &6" + Text.number(value));
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        rodManager.applyCustomDurability(player.getInventory().getItemInMainHand(), false);
        playerDataManager.saveAsync(data);
        cancelSession(player, session, FishingState.CAPTURED, false, null);
        sessions.remove(player.getUniqueId());
    }

    public void cancelAllSessionsForReload() {
        for (UUID uuid : new java.util.ArrayList<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            cancelSession(player, sessions.get(uuid), FishingState.CANCELLED, true, messages.raw("cast-cancelled"));
        }
        sessions.clear();
    }

    private void cancelSession(Player player, FishingSession session, FishingState finalState, boolean message, String line) {
        if (session == null) {
            return;
        }
        session.setState(finalState);
        if (finalState == FishingState.ESCAPED && player != null) {
            PlayerData data = playerDataManager.get(player.getUniqueId());
            if (data != null) {
                data.getStatistics().incrementEscaped();
                playerDataManager.saveAsync(data);
            }
            rodManager.applyCustomDurability(player.getInventory().getItemInMainHand(), true);
        }
        if (session.getBobberDisplay() != null && session.getBobberDisplay().isValid()) {
            session.getBobberDisplay().remove();
        }
        if (session.getFish() != null) {
            session.getFish().removeVisual();
        }
        if (player != null) {
            bossBarManager.clear(player);
            actionBarManager.clear(player);
            if (message && line != null) {
                messages.sendRaw(player, line);
            }
        }
    }

    private void bobBobber(FishingSession session) {
        if (session.getBobberDisplay() == null || !session.getBobberDisplay().isValid()) {
            return;
        }
        Location location = session.getCastLocation().clone().add(0.0D, Math.sin(currentTick / 6.0D) * 0.04D, 0.0D);
        session.getBobberDisplay().teleport(location);
    }

    private void playDiscoveryFeedback(Player player, FishingSession session) {
        if (session.getFish() == null) {
            return;
        }
        switch (session.getFish().getDefinition().getRarity()) {
            case SECRET -> {
                messages.title(player, messages.raw("secret-catch-title"), session.getFish().getDefinition().getDisplayName());
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
            }
            case LEGENDARY, MYTHICAL -> {
                messages.title(player, messages.raw("legendary-catch-title"), session.getFish().getDefinition().getDisplayName());
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.1F);
            }
            case RARE, EPIC -> {
                messages.title(player, messages.raw("rare-catch-title"), session.getFish().getDefinition().getDisplayName());
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9F, 1.3F);
            }
            default -> player.playSound(player.getLocation(), Sound.ENTITY_COD_AMBIENT, 0.7F, 1.3F);
        }
    }

    private void handlePhaseTransition(Player player, FishingSession session) {
        if (session.getFish() == null) {
            return;
        }
        var phase = session.getFish().consumePhaseChange();
        if (phase == null) {
            return;
        }
        switch (phase) {
            case ENRAGED -> {
                messages.title(player, messages.raw("phase-enraged-title"), messages.raw("phase-enraged-subtitle"));
                player.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 0.9F, 0.9F);
                player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, session.getFish().getLocation(), 6, 0.35D, 0.25D, 0.35D, 0.01D);
            }
            case FRENZY -> {
                messages.title(player, messages.raw("phase-frenzy-title"), messages.raw("phase-frenzy-subtitle"));
                player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.9F, 1.15F);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, session.getFish().getLocation(), 14, 0.35D, 0.25D, 0.35D, 0.03D);
            }
            default -> {
            }
        }
    }

    private void emitFightParticles(Player player, FishingSession session) {
        if (session.getFish() == null || currentTick % 8L != 0L) {
            return;
        }
        Particle particle = switch (session.getFish().getState()) {
            case SURGE -> Particle.SPLASH;
            case SHAKE -> Particle.BUBBLE;
            case DIVE -> Particle.DOLPHIN;
            default -> Particle.BUBBLE;
        };
        player.getWorld().spawnParticle(particle, session.getFish().getLocation(), 3, 0.2D, 0.15D, 0.2D, 0.01D);
    }

    private void performLineSnapEscape(Player player, FishingSession session) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.85F);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0F, 0.6F);
        player.getWorld().spawnParticle(Particle.SMOKE, session.getCastLocation(), 8, 0.2D, 0.2D, 0.2D, 0.01D);
        player.getWorld().spawnParticle(Particle.SPLASH, session.getCastLocation(), 12, 0.25D, 0.2D, 0.25D, 0.02D);
        if (session.getFish() != null) {
            session.getFish().boostAwayFrom(player.getEyeLocation(), plugin.getConfig().getDouble("combat.escape-knockback", 0.35D));
        }
        player.setVelocity(player.getVelocity().add(player.getLocation().getDirection().multiply(-plugin.getConfig().getDouble("combat.escape-knockback", 0.35D))));
        messages.title(player, messages.raw("line-snapped-title"), messages.raw("line-snapped-subtitle"));
    }

    private void showCatchReveal(Player player, FishingSession session, long value) {
        if (session.getFish() == null) {
            return;
        }
        var fish = session.getFish();
        messages.title(player, messages.raw("catch-title"), fish.getDefinition().getDisplayName());
        messages.sendRaw(player, "&8&m══════════════════════");
        messages.sendRaw(player, "&6&lCATCH!");
        messages.sendRaw(player, fish.getDefinition().getDisplayName());
        messages.sendRaw(player, "&7Weight: &f" + Text.number(fish.getWeight()) + "kg");
        messages.sendRaw(player, "&7Rarity: &f" + fish.getDefinition().getRarity().name());
        messages.sendRaw(player, "&7Value: &6" + Text.number(value));
        messages.sendRaw(player, "&8&m══════════════════════");
    }

    private String phaseColor(com.megafishing.fish.FishPhase phase) {
        return switch (phase) {
            case ENRAGED -> "&6";
            case FRENZY -> "&c";
            default -> "&a";
        };
    }

    private void debugSession(Player player, FishingSession session, CombatTick combat) {
        if (!plugin.getConfig().getBoolean("debug", false)) {
            return;
        }
        long interval = Math.max(1L, plugin.getConfig().getLong("fishing.update.debug-log-interval-ticks", 40L));
        if ((currentTick - session.getLastDebugTick()) < interval) {
            return;
        }
        session.setLastDebugTick(currentTick);
        String counter = combat == null ? "n/a" : combat.counterResult().state().name();
        plugin.getLogger().info("[MEGA-FISHING][DEBUG] player=" + player.getName()
                + " state=" + session.getState()
                + " fish=" + (session.getFish() == null ? "none" : session.getFish().getDefinition().getId())
                + " hp=" + (session.getFish() == null ? 0.0D : session.getFish().getHealth())
                + "/" + (session.getFish() == null ? 0.0D : session.getFish().getMaxHealth())
                + " tension=" + session.getTension()
                + " counter=" + counter
                + " obstructed=" + session.isObstructed()
                + " tooFar=" + session.isTooFar());
    }
}
