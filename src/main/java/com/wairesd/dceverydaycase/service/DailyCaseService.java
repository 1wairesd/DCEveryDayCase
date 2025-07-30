package com.wairesd.dceverydaycase.service;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.database.DatabaseStatus;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.jodexindustries.donatecase.api.tools.DCTools;
import com.wairesd.dceverydaycase.bootstrap.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DailyCaseService {
    private final Map<String, Boolean> giftInProgress = new ConcurrentHashMap<>();
    private final Main addon;
    private final DCAPI dcapi;
    private final Map<String, Long> nextClaimTimes;
    private long claimCooldown;
    private String caseName;
    private int keysAmount;
    private boolean debug;
    private SchedulerTask schedulerTask;
    public final Set<String> pendingKeys = new HashSet<>();
    private final Logger logger;

    public DailyCaseService(Main addon, DCAPI dcapi, Map<String, Long> nextClaimTimes,
                            long claimCooldown, String caseName, int keysAmount, boolean debug) {
        this.addon = addon;
        this.dcapi = dcapi;
        this.nextClaimTimes = nextClaimTimes;
        this.claimCooldown = claimCooldown;
        this.caseName = caseName;
        this.keysAmount = keysAmount;
        this.debug = debug;
        this.logger = dcapi.getPlatform().getLogger();
    }

    public void startScheduler() {
        schedulerTask = dcapi.getPlatform().getScheduler().run(addon.getAddon(),
                () -> Bukkit.getOnlinePlayers().forEach(this::checkPlayer), 0, 20);
    }

    public void cancelScheduler() {
        if (schedulerTask != null) schedulerTask.cancel();
    }

    public void checkPlayer(Player player) {
        if (addon.getConfigManager().isTurnOffDailyCaseLogic()) return;

        String name = player.getName();

        if (giftInProgress.getOrDefault(name, false)) return;
        if (dcapi.getCaseKeyManager().getCache(caseName, name) > 0) return;

        long now = System.currentTimeMillis();

        if (!nextClaimTimes.containsKey(name)) {
            handleNewPlayerCase(player, now);
            return;
        }

        if (now >= nextClaimTimes.get(name)) {
            giftInProgress.put(name, true);
            giveGift(name);
            pendingKeys.add(name);
            if (addon.getDatabaseManager().getNotificationStatus(name))
                player.sendMessage(DCTools.rc(addon.getConfigManager().getCaseReadyMessage()));
        }
    }

    public void handleNewPlayerCase(Player player, long now) {
        if ("case".equalsIgnoreCase(addon.getConfigManager().getNewPlayerChoice())) {
            giveGift(player.getName());
            pendingKeys.add(player.getName());
            if (addon.getDatabaseManager().getNotificationStatus(player.getName()))
                player.sendMessage(DCTools.rc(addon.getConfigManager().getCaseReadyMessage()));
        }

        nextClaimTimes.put(player.getName(), now + claimCooldown);
    }

    public void giveGift(String player) {
        dcapi.getCaseKeyManager().add(caseName, player, keysAmount).thenAccept(status -> {
            if (status == DatabaseStatus.COMPLETE && debug) {
                logger.info(addon.getConfigManager().getLogConsoleGiveKey()
                        .replace("{key}", String.valueOf(keysAmount))
                        .replace("{player}", player)
                        .replace("{case}", caseName));
            }

            long nextTime = System.currentTimeMillis() + claimCooldown;
            nextClaimTimes.put(player, nextTime);
            addon.getDatabaseManager().asyncSaveNextClaimTimes(nextClaimTimes, () -> {
                if (addon.getConfigManager().isDebug())
                    logger.info("Player " + player + "'s next claim time saved.");
            });
            giftInProgress.remove(player);
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Error giving gift to " + player, ex);
            giftInProgress.remove(player);
            return null;
        });
    }

    public void resetTimer(String playerName) {
        nextClaimTimes.put(playerName, System.currentTimeMillis() + claimCooldown);
        pendingKeys.remove(playerName);

        addon.getDatabaseManager().asyncSaveNextClaimTimes(nextClaimTimes, () -> {
            if (addon.getConfigManager().isDebug())
                logger.info("Player " + playerName + "'s timer reset.");
        });
    }

    public void reload() {
        this.claimCooldown = addon.getConfigManager().getClaimCooldown() * 1000;
        this.caseName = addon.getConfigManager().getCaseName();
        this.keysAmount = addon.getConfigManager().getKeysAmount();
        this.debug = addon.getConfigManager().isDebug();

        Map<String, Long> lastClaimTimes = addon.getDatabaseManager().loadNextClaimTimes();
        this.nextClaimTimes.clear();
        this.nextClaimTimes.putAll(lastClaimTimes);

        cancelScheduler();
        startScheduler();

        dcapi.getPlatform().getLogger().info("DailyCaseService reloaded successfully.");
    }

    public Map<String, Long> getNextClaimTimes() {
        return nextClaimTimes;
    }

    public long getClaimCooldown() {
        return claimCooldown;
    }

    public Plugin getPlugin() {
        return addon.getPluginInstance();
    }

    public DCAPI getDCAPI() {
        return dcapi;
    }

    public String getCaseName() {
        return caseName;
    }

    public int getKeysAmount() {
        return keysAmount;
    }
}