package com.wairesd.dceverydaycase.service;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.database.DatabaseStatus;
import com.jodexindustries.donatecase.api.platform.DCPlayer;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.tools.Color;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Service that manages daily case issuance and timer control. */
public class DailyCaseService {
    private final DCEveryDayCaseAddon addon;
    private final DCAPI dcapi;
    private final Map<String, Long> nextClaimTimes;
    private final long claimCooldown;
    private final String caseName;
    private final int keysAmount;
    private final boolean debug;
    private SchedulerTask schedulerTask;
    private final Logger logger;
    private final Set<String> pendingKeys = new HashSet<>();

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

    /** Starts a scheduler to check every online player for gift eligibility. */
    public void startScheduler() {
        schedulerTask = dcapi.getPlatform().getScheduler().run(addon,
                () -> Bukkit.getOnlinePlayers().forEach(this::checkPlayer), 0, 20);
    }

    /** Cancels the scheduler task if running. */
    public void cancelScheduler() { if (schedulerTask != null) schedulerTask.cancel(); }

    /** Checks player eligibility and issues gift if criteria are met. */
    private void checkPlayer(Player player) {
        String name = player.getName();
        if (pendingKeys.contains(name)) {
            if (debug) logger.info("Player " + name + " is pending. Skipping check.");
            return;
        }
        if (dcapi.getCaseKeyManager().getCache(caseName, name) > 0) return;
        long now = System.currentTimeMillis();
        if (!nextClaimTimes.containsKey(name)) {
            if ("case".equalsIgnoreCase(addon.getConfig().node().newPlayerChoice)) {
                giveGift((DCPlayer) player);
                pendingKeys.add(name);
                if (addon.getDatabaseManager().getNotificationStatus(name))
                    player.sendMessage(Color.translate(addon.getConfig().node().messages.caseReadyMessage));
            }
            nextClaimTimes.put(name, now + claimCooldown);
            return;
        }
        if (now >= nextClaimTimes.get(name)) {
            giveGift((DCPlayer) player);
            pendingKeys.add(name);
            if (addon.getDatabaseManager().getNotificationStatus(name))
                player.sendMessage(Color.translate(addon.getConfig().node().messages.caseReadyMessage));
            nextClaimTimes.put(name, now + claimCooldown);
        }
    }

    public void giveGift(DCPlayer player) {
        dcapi.getCaseKeyManager().add(caseName, player.getName(), keysAmount).thenAccept(status -> {
            if (status == DatabaseStatus.COMPLETE && debug) {
                logger.info(addon.getConfig().node().messages.logConsoleGiveKey
                        .replace("{key}", String.valueOf(keysAmount))
                        .replace("{player}", player.getName())
                        .replace("{case}", caseName));
            }
            long nextTime = System.currentTimeMillis() + claimCooldown;
            nextClaimTimes.put(player.getName(), nextTime);
            addon.getDatabaseManager().asyncSaveNextClaimTimes(nextClaimTimes, () -> {
                if (addon.getConfig().node().debug)
                    logger.info("Player " + player.getName() + "'s next claim time saved.");
            });
        });
    }


    /** Resets the timer for a player and clears pending status. */
    public void resetTimer(String playerName) {
        nextClaimTimes.put(playerName, System.currentTimeMillis() + claimCooldown);
        pendingKeys.remove(playerName);
        addon.getDatabaseManager().asyncSaveNextClaimTimes(nextClaimTimes, () -> {
            if (addon.getConfig().node().debug)
                logger.info("Player " + playerName + "'s timer reset.");
        });
    }

    // Getters
    public Map<String, Long> getNextClaimTimes() { return nextClaimTimes; }
    public long getClaimCooldown() { return claimCooldown; }
    public Plugin getPlugin() { return addon.getPluginInstance(); }
    public DCAPI getDCAPI() { return dcapi; }
    public String getCaseName() { return caseName; }
}
