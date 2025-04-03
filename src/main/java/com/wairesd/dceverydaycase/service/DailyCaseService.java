package com.wairesd.dceverydaycase.service;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.database.DatabaseStatus;
import com.jodexindustries.donatecase.api.scheduler.SchedulerTask;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for the management of the issuance of cases and control of the time.
 */
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

    /** launches a planner who checks the status of players every second*/
    public void startScheduler() {
        schedulerTask = dcapi.getPlatform().getScheduler().run(addon, () -> Bukkit.getOnlinePlayers().forEach(DailyCaseService.this::checkPlayer), 0, 20);
    }

    /** stops the planner*/
    public void cancelScheduler() {
        if (schedulerTask != null) schedulerTask.cancel();
    }

    /** checks whether it is possible to issue the key to the player, and gives him out if the timut is expired*/
    private void checkPlayer(Player player) {
        int keys = dcapi.getCaseKeyManager().getCache(caseName, player.getName());
        if (keys > 0) return;
        long currentTime = System.currentTimeMillis();
        long nextClaim = nextClaimTimes.getOrDefault(player.getName(), currentTime + claimCooldown);
        if (currentTime >= nextClaim) {
            giveGift(player);
            resetTimer(player.getName());
        }
    }

    /** issues the key to the player using the console command Donatecase*/
    public void giveGift(Player player) {

        dcapi.getCaseKeyManager().add(caseName, player.getName(), keysAmount).thenAccept(status -> {
            if (status == DatabaseStatus.COMPLETE && debug) {
                String messageTemplate = addon.getConfig().getLogConsoleGiveKeyMessage();

                String message = messageTemplate
                        .replace("{key}", String.valueOf(keysAmount))
                        .replace("{player}", player.getName())
                        .replace("{case}", caseName);

                logger.info(message);
            }
        });
    }

    /** Updates the next receipt of the key for the player*/
    public void resetTimer(String playerName) {
        nextClaimTimes.put(playerName, System.currentTimeMillis() + claimCooldown);
    }

    public Map<String, Long> getNextClaimTimes() {
        return nextClaimTimes;
    }

    public long getClaimCooldown() {
        return claimCooldown;
    }

    public DCAPI getDCAPI() {
        return dcapi;
    }

    public String getCaseName() {
        return caseName;
    }
}
