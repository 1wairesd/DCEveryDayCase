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

        if (!event.definition().settings().type().equalsIgnoreCase(targetCaseName)) return;

        addon.getDCAPI().getPlatform().getScheduler().run(addon, () -> {
            if (service.getDCAPI().getCaseKeyManager().getCache(targetCaseName, player.getName()) == 0)
                service.resetTimer(player.getName());
        }, 2L);
    }

    @Subscribe
    public void onPlayerJoin(JoinEvent event) {
        DCPlayer dcPlayer = event.player();
        Player player = BukkitUtils.toBukkit(dcPlayer);
        boolean offLogic = addon.getConfig().isTurnOffDailyCaseLogic();
        String newPlayerChoice = addon.getConfig().getNewPlayerChoice();

        if (offLogic && "timer".equalsIgnoreCase(newPlayerChoice)) {
            return;
        }

        if (offLogic && "case".equalsIgnoreCase(newPlayerChoice)) {
            if (!service.getNextClaimTimes().containsKey(player.getName())) {
                service.giveGift(player.getName());
                service.getNextClaimTimes().put(player.getName(), -1L);
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfig().isDebug())
                        addon.getLogger().info("Next claim times updated in database.");
                });
            }
            return;
        }

        if (!offLogic && "case".equalsIgnoreCase(newPlayerChoice)) {
            if (!service.getNextClaimTimes().containsKey(player.getName())) {
                service.giveGift(player.getName()); // We give out the key
                long nextTime = System.currentTimeMillis() + service.getClaimCooldown(); // Timer
                service.getNextClaimTimes().put(player.getName(), nextTime);
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfig().isDebug())
                        addon.getLogger().info("Next claim times updated in database.");
                });
            }
            return;
        }

        if (!offLogic && "timer".equalsIgnoreCase(newPlayerChoice)) {
            if (!service.getNextClaimTimes().containsKey(player.getName())) {
                long nextTime = System.currentTimeMillis() + service.getClaimCooldown(); // Timer
                service.getNextClaimTimes().put(player.getName(), nextTime);
                addon.getDatabaseManager().asyncSaveNextClaimTimes(service.getNextClaimTimes(), () -> {
                    if (addon.getConfig().isDebug())
                        addon.getLogger().info("Next claim times updated in database.");
                });
            }
        }
    }
}