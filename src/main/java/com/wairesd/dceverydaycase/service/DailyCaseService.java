package com.wairesd.dceverydaycase.service;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.database.DatabaseStatus;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.jodexindustries.donatecase.api.tools.DCTools;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DailyCaseService {
    private final DCEveryDayCaseAddon addon;
    private final DCAPI dcapi;
    private final Map<String, Long> nextClaimTimes;
    private long claimCooldown;
    private String caseName;
    private int keysAmount;
    private boolean debug;
    private SchedulerTask schedulerTask;
    public final Set<String> pendingKeys = new HashSet<>();
    private final Logger logger;

    public DailyCaseService(DCEveryDayCaseAddon addon, DCAPI dcapi, Map<String, Long> nextClaimTimes,
                            long claimCooldown, String caseName, int keysAmount, boolean debug) {
        this.addon = addon;
        this.dcapi = dcapi;
        this.nextClaimTimes = nextClaimTimes;
        this.claimCooldown = claimCooldown;
        this.caseName = caseName;
        this.keysAmount = keysAmount;
        this.debug = debug;
        this.logger = addon.getLogger();
    }

    public void startScheduler() {
        // Schedule a task to check all online players every 20 ticks
        schedulerTask = dcapi.getPlatform().getScheduler().run(addon,
                () -> Bukkit.getOnlinePlayers().forEach(this::checkPlayer), 0, 20);
    }

    public void cancelScheduler() { if (schedulerTask != null) schedulerTask.cancel(); }

    public void checkPlayer(Player player) {
        // Skip if daily case logic is off
        if (addon.getConfig().node().OffLogicDailyCase) return;

        String name = player.getName();

        // Skip if the player already has pending keys or available keys
        if (pendingKeys.contains(name)) return;
        if (dcapi.getCaseKeyManager().getCache(caseName, name) > 0) return;

        long now = System.currentTimeMillis();

        // Handle new player (if no next claim time is set)
        if (!nextClaimTimes.containsKey(name)) {
            handleNewPlayerCase(player, now);
            return;
        }

        // If the cooldown is over, give the gift and update claim time
        if (now >= nextClaimTimes.get(name)) {
            giveGift(name);
            pendingKeys.add(name);
            // Send notification to player if enabled
            if (addon.getDatabaseManager().getNotificationStatus(name))
                player.sendMessage(DCTools.rc(addon.getConfig().node().messages.caseReadyMessage));
            nextClaimTimes.put(name, now + claimCooldown);
        }
    }


    public void handleNewPlayerCase(Player player, long now) {
        // If the choice for new players is "case", give the gift and send notification
        if ("case".equalsIgnoreCase(addon.getConfig().node().newPlayerChoice)) {
            giveGift(player.getName());
            pendingKeys.add(player.getName());
            // Send case ready message if notifications are enabled
            if (addon.getDatabaseManager().getNotificationStatus(player.getName()))
                player.sendMessage(DCTools.rc(addon.getConfig().node().messages.caseReadyMessage));
        }

        // Set the next claim time for the player
        nextClaimTimes.put(player.getName(), now + claimCooldown);
    }


    public void giveGift(String player) {
        // Add case keys for the player asynchronously
        dcapi.getCaseKeyManager().add(caseName, player, keysAmount).thenAccept(status -> {
            // Log gift action if successful and debugging is enabled
            if (status == DatabaseStatus.COMPLETE && debug) {
                logger.info(addon.getConfig().node().messages.logConsoleGiveKey
                        .replace("{key}", String.valueOf(keysAmount))
                        .replace("{player}", player)
                        .replace("{case}", caseName));
            }

            // Set next claim time and save it asynchronously
            long nextTime = System.currentTimeMillis() + claimCooldown;
            nextClaimTimes.put(player, nextTime);
            addon.getDatabaseManager().asyncSaveNextClaimTimes(nextClaimTimes, () -> {
                // Log confirmation if debugging is enabled
                if (addon.getConfig().node().debug)
                    logger.info("Player " + player + "'s next claim time saved.");
            });
        });
    }


    public void resetTimer(String playerName) {
        // Reset the player's next claim time and remove from pending keys
        nextClaimTimes.put(playerName, System.currentTimeMillis() + claimCooldown);
        pendingKeys.remove(playerName);

        // Save the updated next claim times asynchronously
        addon.getDatabaseManager().asyncSaveNextClaimTimes(nextClaimTimes, () -> {
            // Log the reset action if debugging is enabled
            if (addon.getConfig().node().debug)
                logger.info("Player " + playerName + "'s timer reset.");
        });
    }


    public void reload() {
        // Reload configuration values
        this.claimCooldown = addon.getConfig().node().claimCooldown * 1000;
        this.caseName = addon.getConfig().node().caseName;
        this.keysAmount = addon.getConfig().node().keysAmount;
        this.debug = addon.getConfig().node().debug;

        // Load and update the player's last claim times
        Map<String, Long> lastClaimTimes = addon.getDatabaseManager().loadNextClaimTimes();
        this.nextClaimTimes.clear();
        this.nextClaimTimes.putAll(lastClaimTimes);

        // Restart the scheduler
        cancelScheduler();
        startScheduler();

        // Log the successful reload
        addon.getLogger().info("DailyCaseService reloaded successfully.");
    }

    public Map<String, Long> getNextClaimTimes() { return nextClaimTimes; }
    public long getClaimCooldown() { return claimCooldown; }
    public Plugin getPlugin() { return addon.getPluginInstance(); }
    public DCAPI getDCAPI() { return dcapi; }
    public String getCaseName() { return caseName; }
}
