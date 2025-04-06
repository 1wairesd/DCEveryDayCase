package com.wairesd.dceverydaycase.events;

import com.jodexindustries.donatecase.api.event.Subscriber;
import com.jodexindustries.donatecase.api.event.player.JoinEvent;
import com.jodexindustries.donatecase.api.event.player.OpenCaseEvent;
import com.jodexindustries.donatecase.api.platform.DCPlayer;
import com.jodexindustries.donatecase.spigot.tools.BukkitUtils;
import com.wairesd.dceverydaycase.DCEveryDayCaseAddon;
import com.wairesd.dceverydaycase.service.DailyCaseService;
import net.kyori.event.method.annotation.Subscribe;
import org.bukkit.entity.Player;

public class OpenCaseListener implements Subscriber {
    private final DailyCaseService service;
    private final String targetCaseName;
    private final DCEveryDayCaseAddon addon;

    public OpenCaseListener(DailyCaseService service, String targetCaseName, DCEveryDayCaseAddon addon) {
        this.service = service;
        this.targetCaseName = targetCaseName;
        this.addon = addon;
    }

    @Subscribe
    public void onCaseOpen(OpenCaseEvent event) {
        DCPlayer dcPlayer = event.player();
        Player player = BukkitUtils.toBukkit(dcPlayer);

        // Skip if the case is not the target case
        if (!event.caseData().caseType().equalsIgnoreCase(targetCaseName)) return;

        // Schedule a task to reset the timer if the player has no case keys
        addon.getDCAPI().getPlatform().getScheduler().run(addon, () -> {
            if (service.getDCAPI().getCaseKeyManager().getCache(targetCaseName, player.getName()) == 0)
                service.resetTimer(player.getName());
        }, 2L);
    }


    @Subscribe
    public void onPlayerJoin(JoinEvent event) {
        DCPlayer dcPlayer = event.player();
        Player player = BukkitUtils.toBukkit(dcPlayer);
        boolean offLogic = addon.getConfig().node().OffLogicDailyCase;
        String newPlayerChoice = addon.getConfig().node().newPlayerChoice;

        // Logic, if the case is turned off the new selection "timer"
        if (offLogic && "timer".equalsIgnoreCase(newPlayerChoice)) {
            // Logic is turned off and selected "timer" - we do nothing
            return;
        }

        // Logic, if the case is turned off and selected "case"
        if (offLogic && "case".equalsIgnoreCase(newPlayerChoice)) {
            // We give out the key, but do not start the timer
            if (!service.getNextClaimTimes().containsKey(player.getName())) {
                service.giveGift(player.getName()); // We give out the key
                service.getNextClaimTimes().put(player.getName(), -1L); // set a temporary mark, which means that the key has already been issued
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfig().node().debug)
                        addon.getLogger().info("Next claim times updated in database.");
                });
            }
            return;
        }

        // Logic, if the case is turned on (turn_off_daily_case_logic = false) and selected "case"
        if (!offLogic && "case".equalsIgnoreCase(newPlayerChoice)) {
            // We give out the key at the first entry and begin the timer after the opening of the case
            if (!service.getNextClaimTimes().containsKey(player.getName())) {
                service.giveGift(player.getName()); // We give out the key
                long nextTime = System.currentTimeMillis() + service.getClaimCooldown(); // Timer
                service.getNextClaimTimes().put(player.getName(), nextTime);
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfig().node().debug)
                        addon.getLogger().info("Next claim times updated in database.");
                });
            }
            return;
        }

        // Logic, if the case is turned on (turn_off_daily_case_logic = false) and selected "timer"
        if (!offLogic && "timer".equalsIgnoreCase(newPlayerChoice)) {
            // We start the timer, but do not give out the key
            if (!service.getNextClaimTimes().containsKey(player.getName())) {
                long nextTime = System.currentTimeMillis() + service.getClaimCooldown(); // Timer
                service.getNextClaimTimes().put(player.getName(), nextTime);
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfig().node().debug)
                        addon.getLogger().info("Next claim times updated in database.");
                });
            }
        }
    }
}
